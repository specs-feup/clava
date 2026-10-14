#!/usr/bin/env python3
"""Resolve and verify the Protobuf artifacts selected by clang-dumper-release.tag."""

from __future__ import annotations

import argparse
import hashlib
import json
import platform
import re
import urllib.error
import urllib.request
from pathlib import Path, PurePosixPath

from google.protobuf import descriptor_pb2


RELEASE_ROOT = "https://github.com/specs-feup/clang-dumper/releases/download"
MANIFEST_NAME = "clang-dumper-release-manifest.json"
SCHEMA_NAME = "clang-dumper-ast-wire.proto"
DESCRIPTOR_NAME = "clang-dumper-ast-wire.pb"
DESCRIPTOR_SCHEMA_NAME = "clava_ast_wire.proto"
PROTOCOL_ID = "clava-ast-wire"
SEMANTIC_CONTRACT = "clava-ast-wire-v1"
PROTOCOL_MAJOR = 1
PROTOCOL_MINOR = 1
FRAMING = "CLAVAPB1 plus protobuf varint-delimited Envelope(Chunk)"
MAX_RECORD_BYTES = 64 * 1024 * 1024
SUPPORTED_LLVM_MAJOR = 18
JAVA_PROTOC_VERSION = "4.28.3"
JAVA_RUNTIME_VERSION = "4.28.3"
JAVA_PACKAGE = "pt.up.fe.specs.clang.wire"
ATTRIBUTE_CLASS_PATTERN = "<closed AttributeKind enum value>Attr"


class ReleaseError(ValueError):
    """A selected release cannot be safely consumed by this Clava checkout."""


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def valid_sha(value: object) -> bool:
    return (isinstance(value, str) and len(value) == 64
            and all(character in "0123456789abcdef" for character in value))


def safe_basename(value: object, label: str) -> str:
    if not isinstance(value, str):
        raise ReleaseError(f"{label} filename must be a string")
    path = PurePosixPath(value)
    if (path.is_absolute() or path.name != value or value in ("", ".", "..")
            or "\\" in value):
        raise ReleaseError(f"Invalid {label} filename: {value!r}")
    return value


def version_tuple(value: object, label: str) -> tuple[int, ...]:
    if not isinstance(value, str) or not re.fullmatch(r"[0-9]+(?:\.[0-9]+){1,3}", value):
        raise ReleaseError(f"Invalid {label} version: {value!r}")
    return tuple(int(part) for part in value.split("."))


def compare_versions(left: tuple[int, ...], right: tuple[int, ...]) -> int:
    width = max(len(left), len(right))
    padded_left = left + (0,) * (width - len(left))
    padded_right = right + (0,) * (width - len(right))
    return (padded_left > padded_right) - (padded_left < padded_right)


def parse_selection(tag_file: Path) -> tuple[str, Path | None]:
    selected = tag_file.read_text(encoding="utf-8").strip()
    path = Path(selected)
    if path.is_absolute():
        resolved = path.resolve()
        if not resolved.is_dir():
            raise ReleaseError(f"Selected local clang-dumper build does not exist: {resolved}")
        return selected, resolved
    if not selected or selected in (".", "..") or "/" in selected or "\\" in selected:
        raise ReleaseError(f"Expected a release tag or an absolute local build path, got {selected!r}")
    return selected, None


def fetch(url: str) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": "Clava-Protobuf-wire-build"})
    with urllib.request.urlopen(request, timeout=90) as response:
        return response.read()


def read_manifest(selected: str, local_folder: Path | None) -> dict:
    if local_folder is not None:
        path = local_folder / MANIFEST_NAME
        if not path.is_file():
            raise ReleaseError(f"Local clang-dumper build is missing {MANIFEST_NAME}: {path}")
        data = path.read_bytes()
    else:
        data = fetch(f"{RELEASE_ROOT}/{selected}/{MANIFEST_NAME}")
    try:
        manifest = json.loads(data)
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        raise ReleaseError(f"Could not parse {MANIFEST_NAME}: {error}") from error
    if not isinstance(manifest, dict):
        raise ReleaseError(f"{MANIFEST_NAME} must contain a JSON object")
    return manifest


def validate_manifest(manifest: dict, selected: str) -> dict:
    if manifest.get("schema_version") != 1:
        raise ReleaseError(f"Unsupported clang-dumper manifest schema version: {manifest.get('schema_version')!r}")

    protocol = manifest.get("protocol")
    if not isinstance(protocol, dict):
        raise ReleaseError("clang-dumper manifest has no protocol metadata")
    required_protocol = {
        "id": PROTOCOL_ID,
        "major": PROTOCOL_MAJOR,
        "minor": PROTOCOL_MINOR,
        "semantic_contract": SEMANTIC_CONTRACT,
        "framing": FRAMING,
        "max_record_bytes": MAX_RECORD_BYTES,
        "llvm_major": SUPPORTED_LLVM_MAJOR,
        "producer_version": f"clang-dumper-{SUPPORTED_LLVM_MAJOR}",
    }
    for key, expected in required_protocol.items():
        actual = protocol.get(key)
        if actual != expected:
            raise ReleaseError(
                f"Selected release {selected!r} has incompatible protocol.{key}: "
                f"expected {expected!r}, got {actual!r}")
    for key in ("schema_sha256", "descriptor_sha256"):
        if not valid_sha(protocol.get(key)):
            raise ReleaseError(f"Invalid protocol.{key} in clang-dumper manifest")

    toolchain = manifest.get("toolchain")
    if not isinstance(toolchain, dict):
        raise ReleaseError("clang-dumper manifest is missing native Protobuf toolchain metadata")
    protobuf_version = version_tuple(toolchain.get("protobuf_version"), "native Protobuf")
    protoc_version = version_tuple(toolchain.get("protoc_version"), "native protoc")
    if protobuf_version != protoc_version:
        raise ReleaseError(
            "Selected release uses mismatched native Protobuf runtime/protoc versions "
            f"({toolchain.get('protobuf_version')} vs {toolchain.get('protoc_version')}); "
            "publish a release with a matched native toolchain")

    compatibility = manifest.get("compatibility")
    if not isinstance(compatibility, dict):
        raise ReleaseError("clang-dumper manifest is missing consumer compatibility metadata")
    minimum_protoc = version_tuple(compatibility.get("minimum_java_protoc_version"),
                                   "minimum Java protoc")
    minimum_runtime = version_tuple(compatibility.get("minimum_java_runtime_version"),
                                    "minimum Java runtime")
    java_protoc = version_tuple(JAVA_PROTOC_VERSION, "pinned Java protoc")
    java_runtime = version_tuple(JAVA_RUNTIME_VERSION, "pinned Java runtime")
    if compare_versions(java_runtime, java_protoc) < 0:
        raise ReleaseError(
            f"Clava pins protobuf-java {JAVA_RUNTIME_VERSION} below its Java generator {JAVA_PROTOC_VERSION}; "
            "explicitly bump the runtime to a supported Protobuf version")
    if java_runtime[0] not in (java_protoc[0], java_protoc[0] + 1):
        raise ReleaseError(
            f"Clava's Java generator/runtime majors {JAVA_PROTOC_VERSION}/{JAVA_RUNTIME_VERSION} are outside "
            "Protobuf's supported V/V+1 compatibility range; explicitly align the Java toolchain")
    if compare_versions(minimum_protoc, java_protoc) > 0:
        raise ReleaseError(
            f"Selected release requires Java protoc {compatibility['minimum_java_protoc_version']} or newer, "
            f"but Clava pins {JAVA_PROTOC_VERSION}; explicitly bump the Clava Protobuf toolchain")
    if compare_versions(minimum_runtime, java_runtime) > 0:
        raise ReleaseError(
            f"Selected release requires protobuf-java {compatibility['minimum_java_runtime_version']} or newer, "
            f"but Clava pins {JAVA_RUNTIME_VERSION}; explicitly bump the Clava Protobuf toolchain")

    assets = manifest.get("assets")
    if not isinstance(assets, list) or not assets:
        raise ReleaseError("clang-dumper manifest has no assets")
    filenames = set()
    for asset in assets:
        if not isinstance(asset, dict):
            raise ReleaseError("clang-dumper manifest contains a malformed asset")
        filename = safe_basename(asset.get("filename"), "asset")
        if filename in filenames:
            raise ReleaseError(f"clang-dumper manifest repeats asset filename {filename!r}")
        filenames.add(filename)
        if not valid_sha(asset.get("sha256")):
            raise ReleaseError(f"Invalid SHA-256 for clang-dumper asset {filename!r}")
        if asset.get("llvm_major") != SUPPORTED_LLVM_MAJOR:
            raise ReleaseError(f"Clang-dumper asset {filename!r} has unsupported LLVM major")

    for filename, expected_hash in ((SCHEMA_NAME, protocol["schema_sha256"]),
                                    (DESCRIPTOR_NAME, protocol["descriptor_sha256"])):
        matching = [asset for asset in assets
                    if asset.get("filename") == filename and asset.get("kind") == "protocol"]
        if len(matching) != 1:
            raise ReleaseError(f"Manifest must contain exactly one protocol asset named {filename!r}")
        if matching[0]["sha256"] != expected_hash:
            raise ReleaseError(f"protocol.{filename} hash disagrees with its protocol asset entry")

    generic_contracts = manifest.get("generic_payload_contracts")
    if not isinstance(generic_contracts, dict):
        raise ReleaseError("clang-dumper manifest is missing checked generic payload contracts")
    attributes = generic_contracts.get("attributes")
    openmp = generic_contracts.get("openmp")
    if not isinstance(attributes, dict) or attributes.get("class_name_pattern") != ATTRIBUTE_CLASS_PATTERN \
            or attributes.get("payload") != "AttributeData":
        raise ReleaseError("clang-dumper manifest has an incompatible generic attribute payload contract")
    if not isinstance(openmp, dict) or openmp.get("payload") != "StmtData" \
            or not isinstance(openmp.get("class_names"), list) or not openmp["class_names"]:
        raise ReleaseError("clang-dumper manifest has an incompatible generic OpenMP payload contract")
    if any(not isinstance(name, str) or not name.startswith("OMP") or not name.isidentifier()
           for name in openmp["class_names"]) or len(set(openmp["class_names"])) != len(openmp["class_names"]):
        raise ReleaseError("clang-dumper manifest contains invalid or repeated generic OpenMP class names")

    return {
        "protocol": protocol,
        "toolchain": toolchain,
        "compatibility": compatibility,
        "generic_payload_contracts": generic_contracts,
        "assets": assets,
    }


def verified_artifact(filename: str, expected_hash: str, selected: str, local_folder: Path | None,
                      cache_root: Path) -> bytes:
    cache_path = cache_root / expected_hash / filename

    local_data = None
    if local_folder is not None:
        source = local_folder / filename
        if not source.is_file():
            raise ReleaseError(f"Local clang-dumper build is missing protocol artifact {source}")
        local_data = source.read_bytes()
        if sha256(local_data) != expected_hash:
            raise ReleaseError(
                f"SHA-256 mismatch for local {filename}: expected {expected_hash}, got {sha256(local_data)}")

    if cache_path.is_file():
        cached = cache_path.read_bytes()
        if sha256(cached) == expected_hash:
            return cached
        cache_path.unlink()

    if local_folder is not None:
        data = local_data
    else:
        data = fetch(f"{RELEASE_ROOT}/{selected}/{filename}")
    actual_hash = sha256(data)
    if actual_hash != expected_hash:
        raise ReleaseError(f"SHA-256 mismatch for {filename}: expected {expected_hash}, got {actual_hash}")

    cache_path.parent.mkdir(parents=True, exist_ok=True)
    temporary = cache_path.with_name(cache_path.name + ".tmp")
    temporary.write_bytes(data)
    temporary.replace(cache_path)
    return data


def parse_descriptor(data: bytes) -> descriptor_pb2.FileDescriptorProto:
    descriptor_set = descriptor_pb2.FileDescriptorSet()
    try:
        descriptor_set.ParseFromString(data)
    except Exception as error:  # protobuf raises DecodeError across runtime versions
        raise ReleaseError(f"Malformed canonical protobuf descriptor set: {error}") from error
    if len(descriptor_set.file) != 1:
        raise ReleaseError(f"Expected one canonical schema descriptor, got {len(descriptor_set.file)}")
    file_descriptor = descriptor_set.file[0]
    if file_descriptor.name != DESCRIPTOR_SCHEMA_NAME or file_descriptor.package != "astwire.v1":
        raise ReleaseError(
            f"Unexpected canonical descriptor identity {file_descriptor.name!r}/{file_descriptor.package!r}")
    if file_descriptor.options.HasField("java_package") or file_descriptor.options.HasField("java_multiple_files"):
        raise ReleaseError("Canonical producer descriptor contains consumer-owned Java options")
    return file_descriptor


def inject_java_options(schema: bytes) -> bytes:
    text = schema.decode("utf-8")
    if re.search(r"(?m)^\s*option\s+java_(?:package|multiple_files)\s*=", text):
        raise ReleaseError("Canonical schema already contains consumer-owned Java options")
    package = re.search(r"(?m)^package\s+[A-Za-z_][A-Za-z0-9_.]*\s*;\s*$", text)
    if not package:
        raise ReleaseError("Canonical protobuf schema has no package declaration")
    java_options = (f'\noption java_package = "{JAVA_PACKAGE}";'
                    "\noption java_multiple_files = true;\n")
    return (text[:package.end()] + java_options + text[package.end():]).encode("utf-8")


def verify_local_executable(assets: list[dict], local_folder: Path, kind: str) -> None:
    if kind not in ("tool", "plugin"):
        raise ReleaseError(f"Unsupported local executable kind {kind!r}")
    host = platform.system().lower()
    platform_name = {"darwin": "macos"}.get(host, host)
    machine = platform.machine().lower()
    arch = "x64" if machine in ("x86_64", "amd64", "x64") else (
        "arm64" if machine in ("arm64", "aarch64") else machine)
    matching = [asset for asset in assets if asset.get("kind") == kind
                and asset.get("platform") == platform_name and asset.get("arch") == arch]
    if len(matching) != 1:
        raise ReleaseError(
            f"Local manifest must contain one {kind} asset for {platform_name}/{arch}, found {len(matching)}")
    asset = matching[0]
    path = local_folder / safe_basename(asset["filename"], "local executable")
    if not path.is_file():
        raise ReleaseError(f"Local clang-dumper executable is missing: {path}")
    actual_hash = sha256_file(path)
    if actual_hash != asset["sha256"]:
        raise ReleaseError(
            f"Local clang-dumper executable hash mismatch for {path.name}: "
            f"expected {asset['sha256']}, got {actual_hash}")


def resolve(args: argparse.Namespace) -> None:
    selected, local_folder = parse_selection(args.tag_file)
    manifest = read_manifest(selected, local_folder)
    validated = validate_manifest(manifest, selected)
    if local_folder is not None:
        verify_local_executable(validated["assets"], local_folder, args.executable_kind)

    protocol = validated["protocol"]
    cache_root = args.cache_root
    schema_bytes = verified_artifact(SCHEMA_NAME, protocol["schema_sha256"], selected,
                                     local_folder, cache_root)
    descriptor_bytes = verified_artifact(DESCRIPTOR_NAME, protocol["descriptor_sha256"], selected,
                                         local_folder, cache_root)
    descriptor = parse_descriptor(descriptor_bytes)
    validate_generic_payload_descriptor(validated["generic_payload_contracts"], descriptor)
    java_schema_bytes = inject_java_options(schema_bytes)

    args.schema_out.parent.mkdir(parents=True, exist_ok=True)
    args.descriptor_out.parent.mkdir(parents=True, exist_ok=True)
    args.java_schema_out.parent.mkdir(parents=True, exist_ok=True)
    args.manifest_out.parent.mkdir(parents=True, exist_ok=True)
    args.schema_out.write_bytes(schema_bytes)
    args.descriptor_out.write_bytes(descriptor_bytes)
    args.java_schema_out.write_bytes(java_schema_bytes)
    resolved = {
        "selected": selected,
        "local": local_folder is not None,
        "schema_sha256": protocol["schema_sha256"],
        "descriptor_sha256": protocol["descriptor_sha256"],
        "semantic_contract": protocol["semantic_contract"],
        "llvm_major": protocol["llvm_major"],
        "native_protobuf_version": validated["toolchain"]["protobuf_version"],
        "native_protoc_version": validated["toolchain"]["protoc_version"],
        "java_protobuf_version": JAVA_RUNTIME_VERSION,
        "java_protoc_version": JAVA_PROTOC_VERSION,
        "minimum_java_runtime_version": validated["compatibility"]["minimum_java_runtime_version"],
        "minimum_java_protoc_version": validated["compatibility"]["minimum_java_protoc_version"],
        "protocol_major": protocol["major"],
        "protocol_minor": protocol["minor"],
        "producer_version": protocol["producer_version"],
        "generic_payload_contracts": validated["generic_payload_contracts"],
    }
    args.manifest_out.write_text(json.dumps(resolved, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def validate_generic_payload_descriptor(contracts: dict, descriptor: descriptor_pb2.FileDescriptorProto) -> None:
    node = next((message for message in descriptor.message_type if message.name == "Node"), None)
    if node is None:
        raise ReleaseError("Canonical descriptor has no Node message for generic payload checks")
    payload_messages = {field.type_name.rsplit(".", 1)[-1] for field in node.field
                       if field.type == field.TYPE_MESSAGE}
    for group in ("attributes", "openmp"):
        payload = contracts[group]["payload"]
        if payload not in payload_messages:
            raise ReleaseError(f"Generic {group} payload contract names absent Node payload {payload}")
    enums = {enum.name for enum in descriptor.enum_type}
    if "AttributeKind" not in enums:
        raise ReleaseError("Canonical descriptor has no closed AttributeKind enum for generic attributes")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--tag-file", type=Path, required=True)
    parser.add_argument("--cache-root", type=Path, required=True)
    parser.add_argument("--schema-out", type=Path, required=True)
    parser.add_argument("--descriptor-out", type=Path, required=True)
    parser.add_argument("--java-schema-out", type=Path, required=True)
    parser.add_argument("--manifest-out", type=Path, required=True)
    parser.add_argument("--executable-kind", default="tool")
    args = parser.parse_args()
    try:
        resolve(args)
    except (OSError, ReleaseError, urllib.error.URLError) as error:
        parser.error(str(error))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

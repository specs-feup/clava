#!/usr/bin/env python3
"""Resolve the selected clang-dumper release's v2 schema and pinned flatc."""

import argparse
import hashlib
import json
import os
import platform
import shutil
import stat
import subprocess
import tempfile
import tarfile
import urllib.request
import zipfile
from pathlib import Path, PurePosixPath


RELEASE_ROOT = "https://github.com/specs-feup/clang-dumper/releases/download"
FLATBUFFERS_VERSION = "25.12.19"
FLATBUFFERS_COMMIT = "7e163021e59cca4f8e1e35a7c828b5c6b7915953"
REFLECTION_FBS_SHA256 = "3b9319066a4a8e1839fe159a7f321099fff2a602bf8599705708d5f49ac66c04"
SOURCE_ARCHIVE_SHA256 = "4236c5d22309abeac2384d3800febcc8d60423e8cb2f573abfa8f798352c4536"
FLATC_ASSETS = {
    ("Linux", "x86_64"): (
        "Linux.flatc.binary.clang++-18.zip",
        "50c1915deeeb714f2a05c8ec795bd1af898d251a62e2774067703b29188efc90",
    ),
    ("Darwin", "arm64"): (
        "Mac.flatc.binary.zip",
        "9340a5f9900b95e34ccadcb06bceec91180cc8b83098d5e966ed6d8d590cbba2",
    ),
    ("Darwin", "aarch64"): (
        "Mac.flatc.binary.zip",
        "9340a5f9900b95e34ccadcb06bceec91180cc8b83098d5e966ed6d8d590cbba2",
    ),
    ("Darwin", "x86_64"): (
        "MacIntel.flatc.binary.zip",
        "b1b0c5bd2b4a19282d461e5ba725f41399af23ef42f4277605b75148996f2f4b",
    ),
    ("Windows", "AMD64"): (
        "Windows.flatc.binary.zip",
        "fff9445c9db907227bc64b54cc98743084c4949282aa4e576cff6a955724ddc8",
    ),
}


def sha256(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def fetch(url: str) -> bytes:
    request = urllib.request.Request(url, headers={"User-Agent": "Clava-AST-wire-build"})
    with urllib.request.urlopen(request, timeout=90) as response:
        return response.read()


def safe_basename(value: str, label: str) -> str:
    path = PurePosixPath(value)
    if path.name != value or value in ("", ".", "..") or "\\" in value:
        raise ValueError(f"Invalid {label} filename: {value!r}")
    return value


def validate_manifest(manifest: dict) -> dict:
    if manifest.get("schema_version") != 2:
        raise ValueError(f"Unsupported release manifest schema: {manifest.get('schema_version')!r}")
    flatbuffers = manifest.get("flatbuffers") or {}
    if flatbuffers != {"version": FLATBUFFERS_VERSION, "commit": FLATBUFFERS_COMMIT}:
        raise ValueError(f"Release uses an unsupported FlatBuffers toolchain: {flatbuffers!r}")
    schema = manifest.get("wire_schema") or {}
    if (schema.get("version") != 2
            or schema.get("entrypoint") != "wire/v2/complete.fbs"
            or schema.get("asset") != "clang-dumper-wire-schema-v2.zip"
            or schema.get("flatbuffers_version") != FLATBUFFERS_VERSION):
        raise ValueError(f"Release does not publish the supported v2 schema: {schema!r}")
    for key in ("sha256", "asset_sha256"):
        value = schema.get(key)
        if not isinstance(value, str) or len(value) != 64 or any(ch not in "0123456789abcdef" for ch in value):
            raise ValueError(f"Invalid wire_schema.{key}: {value!r}")
    safe_basename(schema["asset"], "wire schema asset")
    if not isinstance(manifest.get("assets"), list) or not manifest["assets"]:
        raise ValueError("Release manifest has no tool assets")
    return schema


def resolve_manifest(selected: str) -> tuple[dict, str, Path | None]:
    selected_path = Path(selected)
    if selected_path.is_absolute():
        folder = selected_path.resolve()
        manifest_file = folder / "clang-dumper-release-manifest.json"
        manifest = json.loads(manifest_file.read_text(encoding="utf-8"))
        return manifest, "local", folder
    if selected in (".", "..") or "/" in selected or "\\" in selected or not selected:
        raise ValueError(f"Expected a release tag or an absolute local build path, got {selected!r}")
    data = fetch(f"{RELEASE_ROOT}/{selected}/clang-dumper-release-manifest.json")
    return json.loads(data), selected, None


def canonical_schema_hash(schema_dir: Path) -> str:
    digest = hashlib.sha256()
    files = sorted((schema_dir / "wire" / "v2").glob("*.fbs"), key=lambda path: path.as_posix())
    if not files:
        raise ValueError("Schema archive has no wire/v2/*.fbs files")
    for path in files:
        relative = path.relative_to(schema_dir).as_posix().encode("utf-8")
        digest.update(relative + b"\0" + path.read_bytes() + b"\0")
    return digest.hexdigest()


def extract_schema(data: bytes, destination: Path) -> None:
    destination.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(__import__("io").BytesIO(data)) as archive:
        seen = set()
        for entry in archive.infolist():
            if entry.is_dir():
                continue
            name = PurePosixPath(entry.filename)
            if (name.is_absolute() or ".." in name.parts or "\\" in entry.filename
                    or name.parent != PurePosixPath("wire/v2")
                    or name.as_posix() != entry.filename):
                raise ValueError(f"Unsafe or unexpected schema archive entry {entry.filename!r}")
            if name.suffix != ".fbs" or entry.filename in seen:
                raise ValueError(f"Unexpected or duplicate schema archive entry {entry.filename!r}")
            seen.add(entry.filename)
            target = destination.joinpath(*name.parts)
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(archive.read(entry))
    if "wire/v2/complete.fbs" not in seen:
        raise ValueError("Schema archive does not contain wire/v2/complete.fbs")


def flatc_asset() -> tuple[str, str]:
    key = (platform.system(), platform.machine())
    if key not in FLATC_ASSETS:
        raise ValueError(f"No pinned flatc binary for {key[0]} {key[1]}")
    return FLATC_ASSETS[key]


def verified_source_archive(cache_root: Path) -> bytes:
    cached = cache_root / f"flatbuffers-{FLATBUFFERS_COMMIT}.tar.gz"
    cached.parent.mkdir(parents=True, exist_ok=True)
    data = cached.read_bytes() if cached.is_file() else b""
    if sha256(data) != SOURCE_ARCHIVE_SHA256:
        data = fetch(f"https://codeload.github.com/google/flatbuffers/tar.gz/{FLATBUFFERS_COMMIT}")
    if sha256(data) != SOURCE_ARCHIVE_SHA256:
        raise ValueError("Pinned FlatBuffers Java runtime source archive hash mismatch")
    cached.write_bytes(data)
    return data


def build_flatc_from_source(cache_root: Path) -> Path:
    data = verified_source_archive(cache_root)
    version_dir = cache_root / "flatc-source" / f"{FLATBUFFERS_COMMIT}-{platform.system()}-{platform.machine()}"
    exe = version_dir / ("flatc.exe" if platform.system() == "Windows" else "flatc")
    digest = version_dir / "compiler.sha256"
    if exe.is_file() and digest.is_file() and sha256(exe.read_bytes()) == digest.read_text().strip():
        verify_flatc(exe)
        return exe
    source = version_dir / "source"
    shutil.rmtree(source, ignore_errors=True)
    prefix = f"flatbuffers-{FLATBUFFERS_COMMIT}/"
    with tarfile.open(fileobj=__import__("io").BytesIO(data), mode="r:gz") as archive:
        for entry in archive:
            if not entry.isfile():
                continue
            if not entry.name.startswith(prefix):
                raise ValueError(f"Unexpected compiler source path: {entry.name!r}")
            name = PurePosixPath(entry.name[len(prefix):])
            if name.is_absolute() or ".." in name.parts:
                raise ValueError(f"Unsafe compiler source path: {entry.name!r}")
            target = source.joinpath(*name.parts)
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(archive.extractfile(entry).read())
    build = version_dir / "build"
    command = ["cmake", "-S", str(source), "-B", str(build), "-DCMAKE_BUILD_TYPE=Release",
               "-DFLATBUFFERS_BUILD_TESTS=OFF", "-DFLATBUFFERS_BUILD_FLATC=ON"]
    if platform.system() == "Windows" and platform.machine().lower() in ("arm64", "aarch64"):
        command += ["-A", "ARM64"]
    subprocess.run(command, check=True)
    candidates = [build / "flatc", build / "flatc.exe", build / "Release/flatc.exe"]
    for candidate in candidates:
        candidate.unlink(missing_ok=True)
    subprocess.run(["cmake", "--build", str(build), "--target", "flatc", "--config", "Release",
                    "--parallel", "2"], check=True)
    compiled = next((path for path in candidates if path.is_file()), None)
    if compiled is None:
        raise ValueError("Pinned flatc source build produced no compiler")
    shutil.copy2(compiled, exe)
    verify_flatc(exe)
    digest.write_text(sha256(exe.read_bytes()) + "\n", encoding="utf-8")
    return exe


def ensure_flatc(cache_root: Path) -> Path:
    key = (platform.system(), platform.machine())
    if key not in FLATC_ASSETS and key[0] in ("Linux", "Windows") and key[1].lower() in ("arm64", "aarch64"):
        return build_flatc_from_source(cache_root)
    asset, expected = flatc_asset()
    version_dir = cache_root / "flatc" / f"{FLATBUFFERS_VERSION}-{FLATBUFFERS_COMMIT}"
    exe = version_dir / ("flatc.exe" if platform.system() == "Windows" else "flatc")
    version_dir.mkdir(parents=True, exist_ok=True)
    url = f"https://github.com/google/flatbuffers/releases/download/v{FLATBUFFERS_VERSION}/{asset}"
    cached_archive = version_dir / asset
    archive_data = cached_archive.read_bytes() if cached_archive.is_file() else b""
    if sha256(archive_data) != expected:
        archive_data = fetch(url)
    if sha256(archive_data) != expected:
        raise ValueError(f"Flatc archive SHA-256 mismatch for {asset}")
    cached_archive.write_bytes(archive_data)
    with zipfile.ZipFile(__import__("io").BytesIO(archive_data)) as archive:
        matches = [entry for entry in archive.infolist()
                   if not entry.is_dir() and PurePosixPath(entry.filename).name == exe.name]
        if len(matches) != 1:
            raise ValueError(f"Expected one {exe.name} in {asset}, found {len(matches)}")
        entry = matches[0]
        if PurePosixPath(entry.filename).is_absolute() or ".." in PurePosixPath(entry.filename).parts:
            raise ValueError(f"Unsafe flatc archive path: {entry.filename!r}")
        compiler_data = archive.read(entry)
        if not exe.is_file() or exe.read_bytes() != compiler_data:
            exe.write_bytes(compiler_data)
    if platform.system() != "Windows":
        exe.chmod(exe.stat().st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)
    verify_flatc(exe)
    return exe


def verify_flatc(exe: Path) -> None:
    output = subprocess.run([str(exe), "--version"], check=True, capture_output=True, text=True).stdout.strip()
    if output != f"flatc version {FLATBUFFERS_VERSION}":
        raise ValueError(f"Expected flatc {FLATBUFFERS_VERSION}, got {output!r}")


def resolve_java_runtime(cache_root: Path, destination: Path) -> None:
    data = verified_source_archive(cache_root)
    prefix = f"flatbuffers-{FLATBUFFERS_COMMIT}/java/src/main/java/"
    sources = {}
    with tarfile.open(fileobj=__import__("io").BytesIO(data), mode="r:gz") as archive:
        for entry in archive:
            if not entry.name.startswith(prefix) or not entry.isfile():
                continue
            name = PurePosixPath(entry.name[len(prefix):])
            if name.is_absolute() or ".." in name.parts or name.suffix != ".java":
                raise ValueError(f"Unsafe runtime source path: {entry.name!r}")
            sources[name] = archive.extractfile(entry).read()
    constants = sources.get(PurePosixPath("com/google/flatbuffers/Constants.java"), b"")
    if f"FLATBUFFERS_{FLATBUFFERS_VERSION.replace('.', '_')}".encode() not in constants:
        raise ValueError("Pinned Java runtime lacks the expected compiler version marker")
    shutil.rmtree(destination, ignore_errors=True)
    for name, content in sources.items():
        target = destination.joinpath(*name.parts)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(content)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--tag-file", type=Path, required=True)
    parser.add_argument("--cache-root", type=Path, required=True)
    parser.add_argument("--schema-out", type=Path, required=True)
    parser.add_argument("--flatc-out", type=Path, required=True)
    parser.add_argument("--reflection-fbs-out", type=Path, required=True)
    parser.add_argument("--manifest-out", type=Path, required=True)
    parser.add_argument("--java-runtime-out", type=Path, required=True)
    args = parser.parse_args()

    selected = args.tag_file.read_text(encoding="utf-8").strip()
    manifest, release_tag, local_folder = resolve_manifest(selected)
    schema = validate_manifest(manifest)
    if local_folder:
        archive_path = local_folder / safe_basename(schema["asset"], "wire schema asset")
        archive_data = archive_path.read_bytes()
    else:
        archive_data = fetch(f"{RELEASE_ROOT}/{release_tag}/{safe_basename(schema['asset'], 'wire schema asset')}")
    if sha256(archive_data) != schema["asset_sha256"]:
        raise ValueError("Published wire schema archive SHA-256 does not match the release manifest")

    temp_schema = args.schema_out.with_name(args.schema_out.name + ".tmp")
    shutil.rmtree(temp_schema, ignore_errors=True)
    extract_schema(archive_data, temp_schema)
    actual_hash = canonical_schema_hash(temp_schema)
    if actual_hash != schema["sha256"]:
        shutil.rmtree(temp_schema, ignore_errors=True)
        raise ValueError(f"Canonical schema hash mismatch: manifest={schema['sha256']}, extracted={actual_hash}")
    shutil.rmtree(args.schema_out, ignore_errors=True)
    temp_schema.replace(args.schema_out)

    flatc = ensure_flatc(args.cache_root)
    resolve_java_runtime(args.cache_root, args.java_runtime_out)
    args.flatc_out.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(flatc, args.flatc_out)
    if platform.system() != "Windows":
        args.flatc_out.chmod(args.flatc_out.stat().st_mode | stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH)

    reflection_url = f"https://raw.githubusercontent.com/google/flatbuffers/{FLATBUFFERS_COMMIT}/reflection/reflection.fbs"
    reflection_data = fetch(reflection_url)
    if sha256(reflection_data) != REFLECTION_FBS_SHA256:
        raise ValueError("Pinned FlatBuffers reflection.fbs hash mismatch")
    args.reflection_fbs_out.parent.mkdir(parents=True, exist_ok=True)
    args.reflection_fbs_out.write_bytes(reflection_data)
    args.manifest_out.parent.mkdir(parents=True, exist_ok=True)
    args.manifest_out.write_text(json.dumps({
        "selected": selected,
        "release_tag": release_tag,
        "local": local_folder is not None,
        "schema_version": schema["version"],
        "schema_hash": schema["sha256"],
        "schema_asset_sha256": schema["asset_sha256"],
        "flatbuffers_version": FLATBUFFERS_VERSION,
        "flatbuffers_commit": FLATBUFFERS_COMMIT,
        "toolchain_asset": FLATC_ASSETS.get((platform.system(), platform.machine()),
                                             (f"source:{FLATBUFFERS_COMMIT}", None))[0],
        "flatc_sha256": sha256(args.flatc_out.read_bytes()),
        "java_runtime_source_sha256": SOURCE_ARCHIVE_SHA256,
    }, indent=2) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()

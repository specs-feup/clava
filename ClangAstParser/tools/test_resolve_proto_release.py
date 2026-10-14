#!/usr/bin/env python3
"""Unit tests for release metadata compatibility and artifact cache isolation."""

from __future__ import annotations

import hashlib
import importlib.util
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from google.protobuf import descriptor_pb2


SCRIPT = Path(__file__).with_name("resolve_proto_release.py")
SPEC = importlib.util.spec_from_file_location("resolve_proto_release", SCRIPT)
resolver = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(resolver)


def manifest() -> dict:
    schema_hash = "a" * 64
    descriptor_hash = "b" * 64
    return {
        "schema_version": 1,
        "toolchain": {"protobuf_version": "28.3", "protoc_version": "28.3"},
        "compatibility": {
            "minimum_java_protoc_version": "4.28.3",
            "minimum_java_runtime_version": "4.28.3",
        },
        "protocol": {
            "id": resolver.PROTOCOL_ID,
            "major": resolver.PROTOCOL_MAJOR,
            "minor": resolver.PROTOCOL_MINOR,
            "semantic_contract": resolver.SEMANTIC_CONTRACT,
            "framing": resolver.FRAMING,
            "max_record_bytes": resolver.MAX_RECORD_BYTES,
            "llvm_major": resolver.SUPPORTED_LLVM_MAJOR,
            "producer_version": f"clang-dumper-{resolver.SUPPORTED_LLVM_MAJOR}",
            "schema_sha256": schema_hash,
            "descriptor_sha256": descriptor_hash,
        },
        "assets": [
            {"filename": resolver.SCHEMA_NAME, "kind": "protocol", "llvm_major": 18,
             "sha256": schema_hash},
            {"filename": resolver.DESCRIPTOR_NAME, "kind": "protocol", "llvm_major": 18,
             "sha256": descriptor_hash},
            {"filename": "tool", "kind": "tool", "platform": "linux", "arch": "x64",
             "llvm_major": 18, "sha256": "c" * 64},
        ],
        "generic_payload_contracts": {
            "attributes": {"class_name_pattern": resolver.ATTRIBUTE_CLASS_PATTERN,
                           "payload": "AttributeData"},
            "openmp": {"class_names": ["OMPParallelDirective"], "payload": "StmtData"},
        },
    }


class ProtoReleaseResolverTest(unittest.TestCase):

    @staticmethod
    def descriptor_bytes(name: str = "clava_ast_wire.proto", java_options: bool = False) -> bytes:
        file_descriptor = descriptor_pb2.FileDescriptorProto(name=name, package="astwire.v1")
        if java_options:
            file_descriptor.options.java_package = "pt.up.fe.specs.clang.wire"
        descriptor_set = descriptor_pb2.FileDescriptorSet()
        descriptor_set.file.add().CopyFrom(file_descriptor)
        return descriptor_set.SerializeToString()

    def test_descriptor_uses_the_canonical_producer_source_name(self) -> None:
        parsed = resolver.parse_descriptor(self.descriptor_bytes())
        self.assertEqual("clava_ast_wire.proto", parsed.name)
        with self.assertRaisesRegex(resolver.ReleaseError, "Unexpected canonical descriptor identity"):
            resolver.parse_descriptor(self.descriptor_bytes("clang-dumper-ast-wire.proto"))

    def test_canonical_descriptor_rejects_consumer_java_options(self) -> None:
        with self.assertRaisesRegex(resolver.ReleaseError, "consumer-owned Java options"):
            resolver.parse_descriptor(self.descriptor_bytes(java_options=True))

    def test_native_and_java_toolchains_are_independently_versioned(self) -> None:
        resolved = resolver.validate_manifest(manifest(), "release-tag")
        self.assertEqual((28, 3), resolver.version_tuple(resolved["toolchain"]["protoc_version"], "protoc"))
        self.assertEqual("4.28.3", resolver.JAVA_PROTOC_VERSION)

    def test_native_protoc_and_runtime_must_match(self) -> None:
        selected = manifest()
        selected["toolchain"]["protobuf_version"] = "28.2"
        with self.assertRaisesRegex(resolver.ReleaseError, "mismatched native Protobuf"):
            resolver.validate_manifest(selected, "release-tag")

    def test_release_minimum_requires_an_explicit_java_toolchain_bump(self) -> None:
        selected = manifest()
        selected["compatibility"]["minimum_java_protoc_version"] = "4.29.0"
        with self.assertRaisesRegex(resolver.ReleaseError, "explicitly bump"):
            resolver.validate_manifest(selected, "release-tag")

    def test_release_minimum_versions_compare_trailing_zero_components(self) -> None:
        selected = manifest()
        selected["compatibility"]["minimum_java_protoc_version"] = "4.28.3.0"
        selected["compatibility"]["minimum_java_runtime_version"] = "4.28.3.0"
        resolver.validate_manifest(selected, "release-tag")

    def test_java_runtime_cannot_be_older_than_generator(self) -> None:
        with patch.object(resolver, "JAVA_RUNTIME_VERSION", "4.28.2"):
            with self.assertRaisesRegex(resolver.ReleaseError, "below its Java generator"):
                resolver.validate_manifest(manifest(), "release-tag")

    def test_java_runtime_major_must_be_generator_major_or_next(self) -> None:
        with patch.object(resolver, "JAVA_RUNTIME_VERSION", "6.0.0"):
            with self.assertRaisesRegex(resolver.ReleaseError, r"V/V\+1"):
                resolver.validate_manifest(manifest(), "release-tag")

    def test_local_artifact_is_checked_even_when_valid_cache_exists(self) -> None:
        canonical = b"verified canonical artifact"
        digest = hashlib.sha256(canonical).hexdigest()
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            local = root / "local-build"
            local.mkdir()
            (local / resolver.SCHEMA_NAME).write_bytes(b"tampered local artifact")
            cache = root / "cache" / digest / resolver.SCHEMA_NAME
            cache.parent.mkdir(parents=True)
            cache.write_bytes(canonical)

            with self.assertRaisesRegex(resolver.ReleaseError, "SHA-256 mismatch for local"):
                resolver.verified_artifact(resolver.SCHEMA_NAME, digest, "local", local,
                                           root / "cache")

    def test_missing_local_artifact_is_checked_even_when_valid_cache_exists(self) -> None:
        canonical = b"verified canonical artifact"
        digest = hashlib.sha256(canonical).hexdigest()
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            local = root / "local-build"
            local.mkdir()
            cache = root / "cache" / digest / resolver.DESCRIPTOR_NAME
            cache.parent.mkdir(parents=True)
            cache.write_bytes(canonical)

            with self.assertRaisesRegex(resolver.ReleaseError, "missing protocol artifact"):
                resolver.verified_artifact(resolver.DESCRIPTOR_NAME, digest, "local", local,
                                           root / "cache")


if __name__ == "__main__":
    unittest.main()

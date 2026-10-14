"""Read provenance from producer assets installed by an ordinary parser run."""

import hashlib
import json
from pathlib import Path


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def installed_tool_metadata(work: Path) -> dict | None:
    for manifest_path in sorted(work.rglob("clang-dumper-release-manifest.json")):
        manifest = json.loads(manifest_path.read_text())
        for asset in manifest.get("assets", []):
            if asset.get("kind") != "tool":
                continue
            tool = manifest_path.parent / asset["filename"]
            if not tool.is_file():
                continue
            actual = sha256_file(tool)
            if actual != asset["sha256"]:
                raise ValueError(f"installed producer tool differs from its manifest: {tool}")
            return {
                "native_tool_sha256": actual,
                "native_tool_path": str(tool),
                "release_manifest_sha256": sha256_file(manifest_path),
                "wire_schema_sha256": manifest.get("protocol", {}).get("schema_sha256"),
                "wire_descriptor_sha256": manifest.get("protocol", {}).get("descriptor_sha256"),
                "semantic_contract": manifest.get("protocol", {}).get("semantic_contract"),
                "llvm_major": asset.get("llvm_major"),
                "toolchain": manifest.get("toolchain"),
            }
    return None

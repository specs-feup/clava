#!/usr/bin/env python3
"""Create an isolated Protobuf benchmark snapshot from the current workspace."""

from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import uuid

# This command imports helpers from the canonical checkout. Keep it read-only.
sys.dont_write_bytecode = True

if str(Path(__file__).resolve().parent) not in sys.path:
    sys.path.insert(0, str(Path(__file__).resolve().parent))
import support


RELEASE_TAG_DEFAULT = "v18.1.8_6-rc8"
NATIVE_RELEASE_COMMIT = "e874debc111ec105fa5d1efa17ecd97300613178"
LARA_CAPTURE_PATH = Path("LARAI/src/org/lara/interpreter/weaver/LaraWeaverEngine.java")
LARA_CAPTURE_SHA256 = "276ead7d821b322df07b70c0b7b1e85383fb30754aaa548522ec6b10c34de477"
DIAGNOSTICS_PATH = Path("ClangAstParser/src/pt/up/fe/specs/clang/codeparser/CodeParser.java")
DIAGNOSTICS_TRUE = 'KeyFactory.bool("showExecInfo").setDefault(() -> true)'
DIAGNOSTICS_FALSE = 'KeyFactory.bool("showExecInfo").setDefault(() -> false)'


def run(command: list[str], *, capture: bool = True) -> str:
    result = subprocess.run(command, text=True, capture_output=capture, check=False)
    if result.returncode:
        detail = result.stderr.strip() if capture else ""
        raise RuntimeError(f"command failed ({result.returncode}): {command!r}\n{detail}")
    return result.stdout.strip() if capture else ""


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def source_status(repo: Path) -> list[str]:
    return run(["git", "-C", str(repo), "status", "--porcelain=v1", "--untracked-files=all"]).splitlines()


def verify_workspace_sources(workspace: Path) -> dict[str, dict[str, object]]:
    repos = {name: workspace / name for name in ("clava", "specs-java-libs", "lara-framework", "clang-dumper")}
    for name, path in repos.items():
        if not (path / ".git").exists():
            raise RuntimeError(f"workspace is missing the {name} Git checkout: {path}")
    statuses = {name: source_status(path) for name, path in repos.items()}
    allowed_clava = [line for line in statuses["clava"] if "ClangAstParser/clang-dumper-release.tag" in line]
    if [line for line in statuses["clava"] if line not in allowed_clava]:
        raise RuntimeError("Clava checkout has unreviewed dirt outside its local release selector")
    if allowed_clava:
        selector = repos["clava"] / "ClangAstParser/clang-dumper-release.tag"
        local_value = selector.read_text(encoding="utf-8").strip()
        expected_local = str((workspace / "clang-dumper" / "build").resolve())
        if local_value != expected_local:
            raise RuntimeError("Clava local release selector does not point at this workspace's clang-dumper/build")
    allowed_lara = [line for line in statuses["lara-framework"] if LARA_CAPTURE_PATH.as_posix() in line]
    if [line for line in statuses["lara-framework"] if line not in allowed_lara]:
        raise RuntimeError("Lara checkout has unreviewed dirt outside the authorized App-capture overlay")
    if allowed_lara:
        overlay = repos["lara-framework"] / LARA_CAPTURE_PATH
        if sha256(overlay) != LARA_CAPTURE_SHA256:
            raise RuntimeError("Lara App-capture overlay hash differs from the reviewed input")
    if statuses["specs-java-libs"]:
        raise RuntimeError("specs-java-libs checkout must be clean before snapshot preparation")
    if any(line.startswith(" M ") or line.startswith("M ") for line in statuses["clang-dumper"]):
        raise RuntimeError("clang-dumper checkout has tracked edits; preserve them and prepare from its clean HEAD")
    return {
        name: {
            "root": str(path.resolve()),
            "revision": run(["git", "-C", str(path), "rev-parse", "HEAD"]),
            "status": statuses[name],
        }
        for name, path in repos.items()
    }


def _map_workspace_link(target: Path, workspace: Path, snapshot: Path) -> Path:
    try:
        relative = target.resolve().relative_to(workspace.resolve())
    except ValueError:
        return target.resolve()
    if not relative.parts or relative.parts[0] not in {
            "clava", "specs-java-libs", "lara-framework", "clang-dumper"}:
        return target.resolve()
    return snapshot / relative


def _link_node_modules(source: Path, destination: Path,
                       workspace: Path, snapshot: Path) -> None:
    if not source.is_dir():
        raise RuntimeError(f"shared dependency directory is missing: {source}")
    destination.mkdir(parents=True, exist_ok=False)
    for entry in source.iterdir():
        target = destination / entry.name
        if entry.name == "@specs-feup" and entry.is_dir():
            target.mkdir()
            for package in entry.iterdir():
                package_target = _map_workspace_link(package, workspace, snapshot)
                (target / package.name).symlink_to(package_target, target_is_directory=True)
            continue
        mapped_target = _map_workspace_link(entry, workspace, snapshot)
        target.symlink_to(mapped_target, target_is_directory=entry.is_dir())


def _stage_workspace_manifest(workspace: Path, snapshot: Path) -> str:
    source = workspace / "package.json"
    if not source.is_file():
        raise RuntimeError(f"workspace npm manifest is missing: {source}")
    manifest = json.loads(source.read_text(encoding="utf-8"))
    expected_workspaces = ["lara-framework/Lara-JS", "clava/Clava-JS"]
    if manifest.get("workspaces") != expected_workspaces:
        raise RuntimeError("workspace npm manifest does not declare the pinned Clava and Lara workspaces")
    destination = snapshot / "package.json"
    shutil.copy2(source, destination)
    if sha256(source) != sha256(destination):
        raise RuntimeError("copied npm workspace manifest changed")
    return sha256(destination)


def prepare_snapshot(workspace: Path, output: Path, assets_root: Path,
                     release_tag: str = RELEASE_TAG_DEFAULT) -> dict[str, object]:
    workspace = workspace.expanduser().resolve()
    output = output.expanduser().resolve()
    assets_root = assets_root.expanduser().resolve()
    sources = verify_workspace_sources(workspace)
    if output.exists():
        raise RuntimeError(f"refusing to reuse an existing snapshot: {output}")
    if not re.fullmatch(r"v[0-9]+(?:\.[0-9]+)+_[0-9]+-rc[0-9]+", release_tag):
        raise RuntimeError("benchmark selector must be a published release-candidate tag")
    manifest_path = assets_root / "clang-dumper-release-manifest.json"
    if not manifest_path.is_file():
        raise RuntimeError(f"published release manifest is missing: {manifest_path}")
    manifest_bytes = manifest_path.read_bytes()
    manifest = json.loads(manifest_bytes)
    output.mkdir(parents=True)
    worktrees: list[tuple[Path, Path]] = []
    try:
        workspace_manifest_sha256 = _stage_workspace_manifest(workspace, output)
        repo_names = ("clava", "specs-java-libs", "lara-framework", "clang-dumper")
        for name in repo_names:
            source = workspace / name
            destination = output / name
            commit = NATIVE_RELEASE_COMMIT if name == "clang-dumper" else str(sources[name]["revision"])
            run(["git", "-C", str(source), "worktree", "add", "--detach", str(destination), commit])
            worktrees.append((source, destination))

        clava = output / "clava"
        js_workload_overlay = support.stage_pinned_js_workload(
            clava / "Clava-JS", str(sources["clava"]["revision"]), workspace / "clava",
        )
        tag_file = clava / "ClangAstParser/clang-dumper-release.tag"
        original_committed_tag = run(["git", "-C", str(clava), "show", f"HEAD:{tag_file.relative_to(clava).as_posix()}"])
        tag_file.write_text(release_tag + "\n", encoding="utf-8")
        diagnostics_file = clava / DIAGNOSTICS_PATH
        original_diagnostics = diagnostics_file.read_text(encoding="utf-8")
        if original_diagnostics.count(DIAGNOSTICS_TRUE) == 1:
            diagnostics_file.write_text(original_diagnostics.replace(DIAGNOSTICS_TRUE, DIAGNOSTICS_FALSE, 1),
                                        encoding="utf-8")
        elif original_diagnostics.count(DIAGNOSTICS_FALSE) != 1:
            raise RuntimeError("CodeParser diagnostics default changed; refusing an unreviewed snapshot overlay")

        lara_overlay_source = workspace / "lara-framework" / LARA_CAPTURE_PATH
        lara_overlay_destination = output / "lara-framework" / LARA_CAPTURE_PATH
        lara_overlay_hash = sha256(lara_overlay_source)
        copied_lara_overlay = lara_overlay_hash != sha256(lara_overlay_destination)
        if lara_overlay_hash != sha256(lara_overlay_destination):
            if lara_overlay_hash != LARA_CAPTURE_SHA256:
                raise RuntimeError("Lara App-capture overlay differs from the reviewed input")
            shutil.copy2(lara_overlay_source, lara_overlay_destination)

        dependency_root = workspace / "node_modules"
        _link_node_modules(dependency_root, output / "node_modules", workspace, output)
        _link_node_modules(workspace / "clava/Clava-JS/node_modules",
                           clava / "Clava-JS/node_modules", workspace, output)
        _link_node_modules(workspace / "lara-framework/Lara-JS/node_modules",
                           output / "lara-framework/Lara-JS/node_modules", workspace, output)

        manifest_assets = manifest.get("assets")
        if not isinstance(manifest_assets, list):
            raise RuntimeError("published release manifest has no asset list")
        tool_matches = [asset for asset in manifest_assets if isinstance(asset, dict)
                        and asset.get("kind") == "tool" and asset.get("platform") == "linux"
                        and asset.get("arch") == "x64"]
        if len(tool_matches) != 1:
            raise RuntimeError("published manifest must contain exactly one Linux x64 tool")
        required_names = {
            "clang-dumper-ast-wire.proto", "clang-dumper-ast-wire.pb",
            tool_matches[0]["filename"],
        }
        assets = [asset for asset in manifest_assets
                  if isinstance(asset, dict) and asset.get("filename") in required_names]
        if {asset["filename"] for asset in assets} != required_names:
            raise RuntimeError("published manifest is missing a required Protobuf release asset")
        native_assets = output / "clang-dumper/build"
        native_assets.mkdir(parents=True)
        copied_assets: dict[str, str] = {}
        for asset in assets:
            if not isinstance(asset, dict) or not isinstance(asset.get("filename"), str):
                raise RuntimeError("published release contains a malformed asset entry")
            name = asset["filename"]
            source = assets_root / name
            if not source.is_file() or sha256(source) != asset.get("sha256"):
                raise RuntimeError(f"published release asset missing or hash-mismatched: {name}")
            shutil.copy2(source, native_assets / name)
            copied_assets[name] = sha256(native_assets / name)
        shutil.copy2(manifest_path, native_assets / manifest_path.name)
        manifest_sha = hashlib.sha256(manifest_bytes).hexdigest()
        if sha256(native_assets / manifest_path.name) != manifest_sha:
            raise RuntimeError("copied published release manifest changed")

        capture = {
            "created_utc": dt.datetime.now(dt.timezone.utc).isoformat(),
            "snapshot": str(output),
            "sources": sources,
            "isolated_overlays": {
                "release_selector": {
                    "committed_value": original_committed_tag,
                    "selected_value": release_tag,
                    "snapshot_sha256": sha256(tag_file),
                },
                "show_exec_info_default_false": {
                    "source_sha256": hashlib.sha256(original_diagnostics.encode()).hexdigest(),
                    "snapshot_sha256": sha256(diagnostics_file),
                },
                "lara_app_capture": {
                    "sha256": lara_overlay_hash,
                    "copied_from_working_tree": copied_lara_overlay,
                },
                "javascript_workload": js_workload_overlay,
            },
            "published_release": {
                "tag": release_tag,
                "manifest_sha256": manifest_sha,
                "native_repository_revision": NATIVE_RELEASE_COMMIT,
                "asset_sha256": copied_assets,
            },
            "dependencies": {
                "workspace_manifest_sha256": workspace_manifest_sha256,
                "node_modules_source": str(dependency_root.resolve()),
                "local_workspace_packages_retargeted": True,
                "clava_js_node_modules_source": str((workspace / "clava/Clava-JS/node_modules").resolve()),
                "lara_js_node_modules_source": str((workspace / "lara-framework/Lara-JS/node_modules").resolve()),
            },
        }
        (output / "snapshot-identity.json").write_text(json.dumps(capture, indent=2) + "\n", encoding="utf-8")
        return capture
    except Exception:
        for source, destination in reversed(worktrees):
            subprocess.run(["git", "-C", str(source), "worktree", "remove", "--force", str(destination)],
                           text=True, capture_output=True, check=False)
        shutil.rmtree(output, ignore_errors=True)
        raise


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--workspace-root", type=Path, required=True,
                        help="workspace containing clava, specs-java-libs, lara-framework, clang-dumper")
    parser.add_argument("--release-assets-root", type=Path, required=True,
                        help="verified published RC asset directory with manifest, tool, schema, descriptor")
    parser.add_argument("--output-root", type=Path,
                        help="new snapshot directory; defaults to a unique cache path")
    parser.add_argument("--release-tag", default=RELEASE_TAG_DEFAULT)
    args = parser.parse_args(argv)
    if args.output_root is None:
        stamp = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%SZ")
        args.output_root = (Path.home() / ".cache/protobuf-production-validation/snapshots"
                            / f"protobuf-{stamp}-{uuid.uuid4().hex[:8]}")
    try:
        result = prepare_snapshot(args.workspace_root, args.output_root,
                                  args.release_assets_root, args.release_tag)
    except (OSError, RuntimeError, ValueError, json.JSONDecodeError) as error:
        print(f"prepare_snapshot.py: {error}", file=sys.stderr)
        return 2
    print(json.dumps({"snapshot": result["snapshot"], "release_tag": result["published_release"]["tag"],
                      "manifest_sha256": result["published_release"]["manifest_sha256"]}, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

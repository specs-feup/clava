"""Consumer-owned helpers for the Protobuf-only benchmark capture."""

from __future__ import annotations

import csv
import hashlib
import json
import os
from pathlib import Path
import platform
import re
import subprocess
import xml.etree.ElementTree as ET
from typing import Any


COMPILE_TASK_NAMES = {
    "classes", "compileJava", "compileTestJava", "generateCompleteWire",
    "generateWireBindingInventory", "generateWireReflection", "processResources",
    "processTestResources", "resolveWireRelease", "testClasses",
}
JAVA_IDS_SHA256 = "67741eb11b38882b68c51b7e496e9cb82cd9ed9bf4539168208ea111e23681ff"
JS_FILE_ORDERS_SHA256 = "c15c573b534ad291ebc94fcebbd0568c56def4cc78760ed951fa0da1dbfc335b"
COHORT_BASELINE_SHA256 = "b1036cab6f893d0a00361f193b7435ac16259b8ab1c2853ee97e9cf746bd52b9"
MEMORY_WORKLOADS = (
    {"key": "nas-lu", "relative_source": "c/bench/nas_lu.c", "standard": "c11",
     "expected_sha256": "26409fb2ace4dbb9e350b2990199c6b38f91814797bb93e8ef2770f36a121cc8"},
    {"key": "templates", "relative_source": "cxx/templates.cpp", "standard": "c++17",
     "expected_sha256": "b78d4a848806942649ac0127b657f7c60b679ea52e0b1bfa5f60b6fa5f328460"},
)
JS_FILE_SET = {
    "api/LegacyIntegrationTests - C.test.ts",
    "api/LegacyIntegrationTests - CXX.test.ts",
    "api/LegacyIntegrationTests - Issues.test.ts",
    "api/Query.test.ts",
    "api/Issues.test.ts",
    "api/clava/ClavaJoinPoints.test.ts",
    "code/Sandbox.test.ts",
    "code/ClangPlugin/ClangPlugin.test.ts",
    "api/clava/analysis/AnalyserResult.test.ts",
}


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def digest(value: Any) -> str:
    encoded = json.dumps(value, sort_keys=True, separators=(",", ":")).encode()
    return hashlib.sha256(encoded).hexdigest()


def load_cohort_baseline(path: Path | None = None) -> dict[str, Any]:
    path = path or Path(__file__).with_name("cohort-baseline.json")
    if sha256_file(path) != COHORT_BASELINE_SHA256:
        raise RuntimeError("pinned Protobuf cohort baseline hash changed")
    baseline = json.loads(path.read_text(encoding="utf-8"))
    if set(baseline.get("suites", {})) != {"java", "clava-js"}:
        raise RuntimeError("pinned Protobuf baseline must contain Java and Clava-JS cohorts")
    source = Path(__file__).resolve().parents[2] / "validation/evidence/benchmark-20261008-rc8/updated-protobuf-results.json"
    if sha256_file(source) != baseline.get("baseline_results_sha256"):
        raise RuntimeError("pinned Protobuf cohort source evidence hash changed")
    orders = Path(__file__).with_name("js-file-orders.json")
    if sha256_file(orders) != baseline.get("js_file_orders_sha256"):
        raise RuntimeError("pinned JavaScript file-order fixture differs from cohort baseline")
    return baseline


def selected_java_test_sources(test_root: Path, identities: list[str]) -> dict[str, str]:
    files = sorted({identity.split("#", 1)[0].replace(".", "/") + ".java"
                    for identity in identities})
    return {relative: sha256_file(test_root / relative) for relative in files}


def js_test_sources(clava_js_root: Path, orders: dict[str, list[str]]) -> dict[str, str]:
    files = sorted(set(orders.get("app", [])) | set(orders.get("wall", [])))
    if set(files) != JS_FILE_SET:
        raise RuntimeError("JavaScript source manifest does not match the pinned ordered test cohort")
    return {relative: sha256_file(clava_js_root / relative) for relative in files}


def tree_manifest(root: Path) -> dict[str, str]:
    if not root.is_dir():
        raise RuntimeError(f"required source tree is missing: {root}")
    return {
        path.relative_to(root).as_posix(): sha256_file(path)
        for path in sorted(root.rglob("*")) if path.is_file()
    }


def git(root: Path, *arguments: str) -> str:
    result = subprocess.run(["git", "-C", str(root), *arguments],
                            text=True, capture_output=True, check=False)
    if result.returncode:
        raise RuntimeError(f"git {' '.join(arguments)} failed in {root}: {result.stderr.strip()}")
    return result.stdout.strip()


def git_snapshot(root: Path) -> dict[str, Any] | None:
    if not root.is_dir() or not (root / ".git").exists():
        return None
    patch = subprocess.run(["git", "-C", str(root), "diff", "--binary", "HEAD"],
                           capture_output=True, check=False)
    if patch.returncode:
        raise RuntimeError(f"could not hash working-tree patch in {root}")
    status = git(root, "status", "--porcelain=v1", "--untracked-files=all").splitlines()
    untracked = subprocess.run(
        ["git", "-C", str(root), "ls-files", "--others", "--exclude-standard", "-z"],
        capture_output=True, check=False,
    )
    if untracked.returncode:
        raise RuntimeError(f"could not inventory untracked files in {root}")
    untracked_manifest = {}
    for item in untracked.stdout.split(b"\0"):
        if not item:
            continue
        path = root / os.fsdecode(item)
        if path.is_symlink():
            untracked_manifest[path.relative_to(root).as_posix()] = hashlib.sha256(
                os.fsencode(os.readlink(path))).hexdigest()
        elif path.is_file():
            untracked_manifest[path.relative_to(root).as_posix()] = sha256_file(path)
    return {
        "root": str(root.resolve()),
        "revision": git(root, "rev-parse", "HEAD"),
        "dirty": bool(status),
        "status": status,
        "patch_sha256": hashlib.sha256(patch.stdout).hexdigest(),
        "untracked_files": untracked_manifest,
        "untracked_manifest_sha256": digest(untracked_manifest),
    }


def load_pinned_test_ids(path: Path) -> list[str]:
    if sha256_file(path) != JAVA_IDS_SHA256:
        raise RuntimeError("pinned Java identity fixture hash changed")
    identities = json.loads(path.read_text(encoding="utf-8"))
    if (not isinstance(identities, list) or len(identities) != 116
            or identities != sorted(set(identities))
            or any(not isinstance(identity, str) or "#" not in identity
                   for identity in identities)):
        raise RuntimeError("Java identity fixture must contain the frozen 116 sorted test IDs")
    return identities


def load_js_file_orders(path: Path) -> dict[str, list[str]]:
    if sha256_file(path) != JS_FILE_ORDERS_SHA256:
        raise RuntimeError("pinned JavaScript file-order fixture hash changed")
    orders = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(orders, dict) or set(orders) != {"app", "wall"}:
        raise RuntimeError("JS order fixture must contain app and wall orders")
    for phase, order in orders.items():
        if (not isinstance(order, list) or len(order) != len(JS_FILE_SET)
                or set(order) != JS_FILE_SET):
            raise RuntimeError(f"{phase} JS order must contain the frozen nine relative test files")
    return orders


def normalize_memory_workload(record: dict[str, Any], workload_key: str) -> dict[str, Any]:
    workload = next((item for item in record.get("workloads", [])
                     if item.get("key") == workload_key), None)
    if workload is None:
        raise RuntimeError(f"memory record is missing workload {workload_key}")
    observations = []
    for raw in record.get("observations", []):
        if raw.get("workload") != workload_key:
            continue
        observations.append({
            "repeat": raw["repeat"],
            "return_code": raw["return_code"],
            "wall_s": raw["wall_s"],
            "gnu_time": raw["gnu_time"],
            "heap_phases": raw["heap_phases"],
            "command": raw["command"],
            "log": raw["log"],
            "release_tag": record["selector"]["tag"],
            "release_manifest_sha256": record["published_manifest"]["sha256"],
            "resource_cache_tree_sha256": raw["resource_cache_tree_sha256"],
            "release_asset_sha256": raw["release_asset_sha256"],
        })
    return {
        "created_utc": record["created_utc"],
        "source": workload["source"],
        "source_sha256": workload["source_sha256"],
        "input": None,
        "input_sha256": None,
        "repeats": 3,
        "retained_heap_contract": "production command emits phase used_bytes after explicit GC",
        "peak_rss_contract": "GNU time max_rss_kb",
        "observations": observations,
        "failed": [],
        "release_tag": record["selector"]["tag"],
        "release_manifest_sha256": record["published_manifest"]["sha256"],
        "runtime": {
            "path": record["runtime"]["path"],
            "jar_sha256": record["runtime"]["jars"],
            "classes": record["runtime"]["classes"],
            "validation_probe_source_sha256": record["runtime"]["validation_probe_source_sha256"],
        },
    }


def stage_roots(checkout: Path) -> dict[str, Path]:
    checkout = checkout.resolve()
    return {
        "clava": checkout,
        "specs-java-libs": checkout.parent / "specs-java-libs",
        "lara-framework": checkout.parent / "lara-framework",
        "clang-dumper": checkout.parent / "clang-dumper",
    }


def issue15_line(fixture_root: Path) -> str:
    path = fixture_root / "cxx/issues/clava_issue15.cpp.txt"
    if not path.is_file():
        raise RuntimeError(f"missing assembly golden: {path}")
    matches = [line.strip() for line in path.read_text(encoding="utf-8").splitlines()
               if "movl " in line and "%eax" in line and "%ebx" in line]
    if len(matches) != 1:
        raise RuntimeError(f"expected one Issue 15 assembly line in {path}, found {len(matches)}")
    return matches[0]


def selected_release(checkout: Path) -> dict[str, Any]:
    tag_file = checkout / "ClangAstParser/clang-dumper-release.tag"
    if not tag_file.is_file():
        raise RuntimeError(f"missing clang-dumper release tag: {tag_file}")
    tag = tag_file.read_text(encoding="utf-8").strip()
    selected: dict[str, Any] = {
        "tag": tag,
        "tag_file_sha256": sha256_file(tag_file),
        "tag_file": str(tag_file.resolve()),
    }
    if not Path(tag).is_absolute():
        selected["kind"] = "published_release_tag"
        return selected
    local_root = Path(tag)
    manifest = local_root / "clang-dumper-release-manifest.json"
    if not manifest.is_file():
        raise RuntimeError(f"local release has no manifest: {manifest}")
    payload = json.loads(manifest.read_text(encoding="utf-8"))
    system = {"linux": "linux", "darwin": "macos", "windows": "windows"}.get(
        platform.system().lower())
    arch = "arm64" if platform.machine().lower() in {"aarch64", "arm64"} else "x64"
    matches = [asset for asset in payload.get("assets", [])
               if asset.get("kind") == "tool" and asset.get("platform") == system
               and asset.get("arch") == arch]
    if len(matches) != 1:
        raise RuntimeError(f"local release has no unique {system}/{arch} tool asset")
    tool = local_root / matches[0]["filename"]
    if not tool.is_file() or sha256_file(tool) != matches[0].get("sha256"):
        raise RuntimeError(f"local release tool hash differs from its manifest: {tool}")
    selected.update({"kind": "local_build", "release_root": str(local_root.resolve()),
                     "manifest_sha256": sha256_file(manifest), "tool_asset": matches[0],
                     "tool_path": str(tool.resolve()), "tool_sha256": sha256_file(tool)})
    return selected


def stage_snapshot(name: str, checkout: Path) -> dict[str, Any]:
    parser = checkout / "ClangAstParser"
    source_trees = {
        key: tree_manifest(parser / key) for key in ("src", "test", "test-resources")
    }
    return {
        "stage": name,
        "checkout": str(checkout.resolve()),
        "repositories": {key: git_snapshot(path) for key, path in stage_roots(checkout).items()},
        "clang_ast_parser_source_tree_sha256": digest(source_trees["src"]),
        "clang_ast_parser_test_tree_sha256": digest(source_trees["test"]),
        "fixture_manifest": source_trees["test-resources"],
        "fixture_manifest_sha256": digest(source_trees["test-resources"]),
        "issue15_assembly_line": issue15_line(parser / "test-resources"),
        "selected_release": selected_release(checkout),
    }


def parse_junit_results(xml_root: Path) -> dict[str, Any]:
    if not xml_root.is_dir():
        raise RuntimeError(f"JUnit XML output is missing: {xml_root}")
    cases = []
    for path in sorted(xml_root.glob("**/*.xml")):
        root = ET.parse(path).getroot()
        for case in root.iter("testcase"):
            skipped = case.find("skipped") is not None
            failed = case.find("failure") is not None or case.find("error") is not None
            cases.append({
                "id": f"{case.attrib.get('classname', '')}#{case.attrib.get('name', '')}",
                "classname": case.attrib.get("classname", ""),
                "name": case.attrib.get("name", ""),
                "time_s": float(case.attrib.get("time", "0") or 0),
                "status": "failed" if failed else "skipped" if skipped else "passed",
            })
    identities = sorted(case["id"] for case in cases)
    counts = {
        "total": len(cases),
        "passed": sum(case["status"] == "passed" for case in cases),
        "failed": sum(case["status"] == "failed" for case in cases),
        "skipped": sum(case["status"] == "skipped" for case in cases),
    }
    with (xml_root.parent / "testcases.csv").open("w", newline="", encoding="utf-8") as stream:
        writer = csv.DictWriter(stream, fieldnames=("id", "classname", "name", "time_s", "status"))
        writer.writeheader()
        writer.writerows(cases)
    return {
        "counts": counts,
        "identities": identities,
        "identity_sha256": digest(identities),
        "testcase_duration_s": sum(case["time_s"] for case in cases),
        "cases": cases,
    }


def parse_task_states(log_path: Path) -> dict[str, str]:
    pattern = re.compile(r"^> Task :([^\s]+)(?: ([A-Z-]+))?$")
    states: dict[str, str] = {}
    for line in log_path.read_text(encoding="utf-8", errors="replace").splitlines():
        match = pattern.match(line.strip())
        if match:
            task = match.group(1).rsplit(":", 1)[-1]
            if task in COMPILE_TASK_NAMES:
                states[match.group(1)] = match.group(2) or "EXECUTED"
    return states

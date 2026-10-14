#!/usr/bin/env python3
"""Build owned Clava runtimes and Java test classes before deadline measurements."""

from __future__ import annotations

import hashlib
import json
import os
from pathlib import Path
import subprocess
import time
import xml.etree.ElementTree as ET


SCRIPT_ROOT = Path(__file__).resolve().parent
ORCHESTRATION_ROOT = SCRIPT_ROOT.parents[1]
DEADLINE_ROOT = SCRIPT_ROOT.parents[2]
SHARED_ROOT = DEADLINE_ROOT / "shared"
SPECS_ROOT = SHARED_ROOT / "specs-java-libs"
LARA_ROOT = SHARED_ROOT / "lara-framework"
JAVA_INIT = SCRIPT_ROOT / "java-suite.init.gradle"
STAGES = {
    "before-cache": {"native": None, "flat": False},
    "ccache-text": {"native": None, "flat": False},
    "protobuf": {
        "native": Path("/home/lmsousa/Documents/Projects/SPeCS/ast-protobuf/clang-dumper"),
        "flat": False,
    },
    "flatbuffers": {
        "native": Path("/home/lmsousa/Documents/Projects/SPeCS/ast-flatbuffers/clang-dumper"),
        "flat": True,
    },
}


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def git(path: Path, *args: str) -> str:
    result = subprocess.run(
        ["git", "-C", str(path), *args], text=True, capture_output=True, check=True
    )
    return result.stdout.strip()


def git_diff_sha(path: Path) -> str:
    diff = subprocess.run(["git", "-C", str(path), "diff", "--binary", "HEAD"], capture_output=True, check=True)
    return hashlib.sha256(diff.stdout).hexdigest()


def junit_audit(root: Path) -> dict[str, object]:
    records: list[dict[str, str]] = []
    if not root.is_dir():
        return {"available": False, "root_relative": root.relative_to(DEADLINE_ROOT).as_posix()}
    for path in sorted(root.glob("TEST-*.xml")):
        suite = ET.parse(path).getroot()
        for case in suite.findall("testcase"):
            failure = case.find("failure")
            failure = failure if failure is not None else case.find("error")
            status = "failed" if failure is not None else "skipped" if case.find("skipped") is not None else "passed"
            records.append({
                "class": case.attrib.get("classname", ""),
                "name": case.attrib.get("name", ""),
                "status": status,
                "failure_type": failure.attrib.get("type", "") if failure is not None else "",
                "failure_message": failure.attrib.get("message", "") if failure is not None else "",
                "failure_trace": (failure.text or "") if failure is not None else "",
            })
    records.sort(key=lambda item: (item["class"], item["name"], item["status"]))
    identity = [
        {key: item[key] for key in ("class", "name", "status")}
        for item in records
    ]
    failure_identity = [
        {key: item[key] for key in ("class", "name", "failure_type", "failure_message")}
        for item in records if item["status"] == "failed"
    ]
    identity_sha = hashlib.sha256(json.dumps(identity, sort_keys=True, separators=(",", ":")).encode()).hexdigest()
    failure_sha = hashlib.sha256(json.dumps(failure_identity, sort_keys=True, separators=(",", ":")).encode()).hexdigest()
    return {
        "available": bool(records),
        "root_relative": root.relative_to(DEADLINE_ROOT).as_posix(),
        "total": len(records),
        "passed": sum(item["status"] == "passed" for item in records),
        "failed": sum(item["status"] == "failed" for item in records),
        "skipped": sum(item["status"] == "skipped" for item in records),
        "test_identity_sha256": identity_sha,
        "failure_identity_sha256": failure_sha,
        "failures": [item for item in records if item["status"] == "failed"],
    }


def main() -> int:
    env = os.environ.copy()
    env["SPECS_JAVA_LIBS_HOME"] = str(SPECS_ROOT)
    env["LARA_FRAMEWORK_HOME"] = str(LARA_ROOT)
    records: list[dict[str, object]] = []
    records.append({
        "task": "common_dependency_identity",
        "specs_java_libs_revision": git(SPECS_ROOT, "rev-parse", "HEAD"),
        "specs_java_libs_branch": git(SPECS_ROOT, "branch", "--show-current"),
        "specs_java_libs_diff_sha256": git_diff_sha(SPECS_ROOT),
        "specsutils_jar_sha256": sha256(SPECS_ROOT / "SpecsUtils/build/libs/SpecsUtils.jar"),
        "joptions_jar_sha256": sha256(SPECS_ROOT / "jOptions/build/libs/jOptions.jar"),
        "lara_framework_revision": git(LARA_ROOT, "rev-parse", "HEAD"),
        "lara_framework_diff_sha256": git_diff_sha(LARA_ROOT),
    })
    for key, config in STAGES.items():
        clava = DEADLINE_ROOT / "stages" / key / "clava"
        base = ["gradle", "--no-daemon", "--offline"]
        if key == "protobuf":
            base.append(f"-PclangDumperRoot={config['native']}")
        stage_env = env.copy()
        if config["flat"]:
            stage_env["FLAT_NATIVE"] = str(config["native"])

        for name, command in (
            ("runtime_install", [*base, "-p", str(clava / "ClavaWeaver"), "installDist"]),
            (
                "java_test_classes",
                [*base, "--init-script", str(JAVA_INIT), "-p", str(clava / "ClangAstParser"), "testClasses"],
            ),
        ):
            started = time.time()
            result = subprocess.run(command, cwd=clava, env=stage_env, text=True, capture_output=True)
            record: dict[str, object] = {
                "stage": key,
                "task": name,
                "command": command,
                "return_code": result.returncode,
                "elapsed_s": time.time() - started,
                "stdout": result.stdout,
                "stderr": result.stderr,
            }
            records.append(record)
            print(json.dumps({k: v for k, v in record.items() if k not in {"stdout", "stderr"}}), flush=True)
            if result.returncode != 0:
                print(result.stdout)
                print(result.stderr)
                return result.returncode

        runtime = clava / "Clava-JS" / "java-binaries"
        parser_jar = runtime / "lib" / "ClangAstParser.jar"
        specs_jar = runtime / "lib" / "SpecsUtils.jar"
        if not parser_jar.is_file() or not specs_jar.is_file():
            raise RuntimeError(f"missing frozen runtime artifact for {key}: {runtime}")
        records.append({
            "stage": key,
            "task": "frozen_runtime_identity",
            "clava_revision": git(clava, "rev-parse", "HEAD"),
            "clava_patch_sha256": hashlib.sha256(
                subprocess.run(["git", "-C", str(clava), "diff", "--binary", "HEAD"], capture_output=True, check=True).stdout
            ).hexdigest(),
            "native_revision": git(config["native"], "rev-parse", "HEAD") if config["native"] else "published v18.1.8_4",
            "parser_jar_sha256": sha256(parser_jar),
            "specsutils_jar_sha256": sha256(specs_jar),
            "runtime_manifest_jars": {
                str(path.relative_to(runtime)): sha256(path)
                for path in sorted(runtime.rglob("*.jar"))
                if path.is_file()
            },
        })

    validation_root = DEADLINE_ROOT / "validation"
    records.append({
        "task": "shared_dependency_test_audit",
        "overlay_full_run": junit_audit(validation_root / "overlay-full"),
        "clean_19c8_selected_classes": junit_audit(validation_root / "clean-19c8-selected"),
        "overlay_focused_store_tests": junit_audit(validation_root / "overlay-focused"),
    })

    output = DEADLINE_ROOT / "preparation.json"
    output.write_text(json.dumps({"records": records}, indent=2) + "\n")
    print(f"Preparation manifest: {output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

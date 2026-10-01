#!/usr/bin/env python3
"""Run a paired, warm-cache Java full-suite JaCoCo-agent control.

The only treatment is the JaCoCo javaagent on the Gradle Test worker. Both
JaCoCo report tasks remain disabled by the shared suite init script. The
launcher clones the already-warm primary ccache payloads into a fresh
diagnostic tree and directs JUnit output there, leaving matrix-r2 untouched.
"""

from __future__ import annotations

import argparse
import csv
import datetime as dt
import getpass
import hashlib
import json
import os
from pathlib import Path
import re
import shlex
import shutil
import statistics
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

sys.dont_write_bytecode = True

import run_comparison as base


SCRIPT_ROOT = Path(__file__).resolve().parent
DEFAULT_MATRIX = SCRIPT_ROOT / "results/deadline-20260930/matrix-r2/results.json"
DEADLINE_ROOT = SCRIPT_ROOT / "results/deadline-20260930"
SHARED_ROOT = DEADLINE_ROOT / "shared"
SPECS_JAVA_LIBS_ROOT = SHARED_ROOT / "specs-java-libs"
LARA_FRAMEWORK_ROOT = SHARED_ROOT / "lara-framework"
DEFAULT_OUTPUT = SCRIPT_ROOT / "results/deadline-20260930/matrix-r2/java-jacoco-agent-control-r3"
EXPECTED_JAVA = {"total_tests": 116, "passed_tests": 116, "failed_tests": 0, "skipped_tests": 0}
EXPECTED_JAVA_PLAN = {"total": 116, "passed": 116, "failed": 0, "skipped": 0}
EXPECTED_PRIMARY_CACHE = {"cacheable_calls": 208, "hits": 207, "misses": 1, "uncacheable_calls": 0}
STAGE_KEYS = ("ccache-text", "protobuf")
NAMESPACE = {
    "ccache-text": "clang-dumper-ccache",
    "protobuf": "clang-dumper-protobuf-ccache-v1",
}
ORDER = (
    ("ccache-text", "on"),
    ("protobuf", "off"),
    ("protobuf", "on"),
    ("ccache-text", "off"),
    ("ccache-text", "off"),
    ("protobuf", "on"),
    ("protobuf", "off"),
    ("ccache-text", "on"),
    ("protobuf", "on"),
    ("ccache-text", "off"),
    ("ccache-text", "on"),
    ("protobuf", "off"),
    ("protobuf", "off"),
    ("ccache-text", "on"),
    ("ccache-text", "off"),
    ("protobuf", "on"),
)
JVM_CONTROL_FLAGS = ("-Xms", "-Xmx", "-Xmn", "-XX:", "-javaagent:", "-agentlib:",
                     "-agentpath:", "-Xrun", "-Xlog:gc", "-Xloggc", "-verbose:gc")


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--matrix", type=Path, default=DEFAULT_MATRIX)
    parser.add_argument("--output-root", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--max-cells", type=int, default=len(ORDER),
                        help="run only the first N fixed-order cells for an acceptance pilot")
    parser.add_argument("--resume-existing", action="store_true",
                        help="resume a preserved partial output root after strict guard revalidation")
    parser.add_argument("--dry-run", action="store_true", help="validate identities and print the fixed order")
    return parser.parse_args()


def canonical_hash(value: object) -> str:
    return hashlib.sha256(json.dumps(value, sort_keys=True, separators=(",", ":")).encode()).hexdigest()


def git(root: Path, *args: str) -> str:
    result = subprocess.run(["git", "-C", str(root), *args], text=True, capture_output=True, check=False)
    if result.returncode != 0:
        raise RuntimeError(f"git {' '.join(args)} failed in {root}: {result.stderr.strip()}")
    return result.stdout.strip()


def git_status(root: Path) -> list[str]:
    result = subprocess.run(
        ["git", "-C", str(root), "status", "--porcelain=v1", "--untracked-files=all"],
        text=True, capture_output=True, check=False,
    )
    if result.returncode != 0:
        raise RuntimeError(f"git status failed in {root}: {result.stderr.strip()}")
    return result.stdout.splitlines()


def diff_sha256(root: Path) -> str:
    result = subprocess.run(["git", "-C", str(root), "diff", "--binary", "HEAD"], capture_output=True, check=False)
    if result.returncode != 0:
        raise RuntimeError(f"could not fingerprint source diff in {root}")
    return hashlib.sha256(result.stdout).hexdigest()


def load_matrix(path: Path) -> dict:
    data = json.loads(path.read_text())
    if data.get("identity_preflight", {}).get("passed") is not True:
        raise RuntimeError("primary matrix identity preflight did not pass")
    plan = data.get("plan", {})
    if plan.get("expected_tests", {}).get("java") != EXPECTED_JAVA_PLAN:
        raise RuntimeError("primary matrix does not declare the exact 116-pass Java suite")
    stages = plan.get("stages", {})
    rows = data.get("results", [])
    for key in STAGE_KEYS:
        stage = stages.get(key)
        if not isinstance(stage, dict):
            raise RuntimeError(f"primary matrix is missing frozen stage {key}")
        measured = [
            row for row in rows
            if row.get("suite") == "java" and row.get("mode") == "warm"
            and row.get("measured") is True and row.get("stage") == key
        ]
        if len(measured) != 4:
            raise RuntimeError(f"expected four valid primary warm runs for {key}, found {len(measured)}")
        for row in measured:
            if (not row.get("valid") or any(row.get(k) != v for k, v in EXPECTED_JAVA.items())
                    or not row.get("cache_validation", {}).get("passed")
                    or row.get("cache_hits", 0) <= 0):
                raise RuntimeError(f"invalid primary warm run in {key}: {row.get('run_dir')}")
            observed_cache = {
                "cacheable_calls": int(row.get("cacheable_calls", 0)),
                "hits": int(row.get("cache_hits", 0)),
                "misses": int(row.get("cache_misses", 0)),
                "uncacheable_calls": int(row.get("cache_validation", {}).get("uncacheable_calls", 0)),
            }
            if observed_cache != EXPECTED_PRIMARY_CACHE:
                raise RuntimeError(
                    f"primary warm cache outcome drifted for {key}: "
                    f"expected={EXPECTED_PRIMARY_CACHE}, observed={observed_cache}"
                )
    return data


def audit_inherited_jvm_options() -> dict[str, object]:
    audit: dict[str, object] = {}
    violations: list[str] = []
    for name in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "JAVA_OPTS"):
        raw = os.environ.get(name, "")
        try:
            arguments = shlex.split(raw)
        except ValueError as exc:
            raise RuntimeError(f"could not safely parse inherited {name}: {exc}") from exc
        relevant = [arg for arg in arguments if arg.startswith(JVM_CONTROL_FLAGS)]
        audit[name] = {
            "present": bool(raw),
            "argument_count": len(arguments),
            # Persist only heap/GC/agent switches, never arbitrary -D values.
            "heap_gc_agent_flags": relevant,
        }
        if relevant:
            violations.append(f"{name}: {relevant}")
    if violations:
        raise RuntimeError("inherited JVM options would confound the control: " + "; ".join(violations))
    return audit


def verify_stage(stage: dict) -> dict[str, object]:
    root = Path(stage["root"]).resolve()
    clava = root / "clava"
    runtime = clava / "Clava-JS/java-binaries"
    parser_jar = runtime / "lib/ClangAstParser.jar"
    native_tool = Path(stage["dumper"]).resolve()
    observations: dict[str, object] = {
        "clava_revision": git(clava, "rev-parse", "HEAD"),
        "clava_status": git_status(clava),
        "clava_patch_sha256": diff_sha256(clava),
        "parser_jar_sha256": base.sha256_file(parser_jar),
        "runtime_manifest": base.runtime_manifest(runtime),
        "native_revision": git(Path(stage["native_root"]), "rev-parse", "HEAD"),
        "native_status": git_status(Path(stage["native_root"])),
        "native_tool_sha256": base.sha256_file(native_tool),
    }
    expected = {
        "clava_revision": stage["clava_revision"],
        "clava_status": stage["clava_status"],
        "clava_patch_sha256": stage["clava_patch_sha256"],
        "parser_jar_sha256": stage["parser_jar_sha256"],
        "runtime_manifest": {
            "jar_count": stage["runtime_manifest"]["jar_count"],
            "sha256": stage["runtime_manifest_sha256"],
        },
        "native_revision": stage["native_revision"],
        "native_status": stage["dumper_status"],
        "native_tool_sha256": stage["native_binary_sha256"],
    }
    if observations != expected:
        raise RuntimeError(
            f"frozen stage identity changed for {stage['key']}: "
            f"expected={json.dumps(expected, sort_keys=True)} "
            f"observed={json.dumps(observations, sort_keys=True)}"
        )
    dependency = stage["java_build_dependencies"]["specs_java_libs"]
    specs_root = Path(dependency["root"])
    if specs_root.resolve() != SPECS_JAVA_LIBS_ROOT.resolve():
        raise RuntimeError(f"frozen SpecsUtils root is not the shared build root: {specs_root}")
    observed_dependency = {
        "revision": git(specs_root, "rev-parse", "HEAD"),
        "status": git_status(specs_root),
        "diff_sha256": diff_sha256(specs_root),
        "specsutils_jar_sha256": base.sha256_file(Path(dependency["specsutils_jar"])),
    }
    expected_dependency = {
        "revision": dependency["revision"],
        "status": dependency["status"],
        "diff_sha256": dependency["diff_sha256"],
        "specsutils_jar_sha256": dependency["specsutils_jar_sha256"],
    }
    if observed_dependency != expected_dependency:
        raise RuntimeError(f"frozen SpecsUtils build input changed for {stage['key']}")
    lara = stage["java_build_dependencies"]["lara_framework"]
    lara_root = Path(lara["root"])
    if lara_root.resolve() != LARA_FRAMEWORK_ROOT.resolve():
        raise RuntimeError(f"frozen Lara root is not the shared build root: {lara_root}")
    observed_lara = {
        "revision": git(lara_root, "rev-parse", "HEAD"),
        "status": git_status(lara_root),
        "diff_sha256": diff_sha256(lara_root),
    }
    expected_lara = {
        "revision": lara["revision"], "status": lara["status"], "diff_sha256": lara["diff_sha256"],
    }
    if observed_lara != expected_lara:
        raise RuntimeError(f"frozen Lara framework build input changed for {stage['key']}")
    return observations


def cache_manifest(root: Path) -> dict[str, str]:
    return {
        path.relative_to(root).as_posix(): base.sha256_file(path)
        for path in sorted(root.rglob("*")) if path.is_file()
    }


def ensure_cache_clone(stage: dict, source_cache: Path, temp_root: Path) -> tuple[Path, str]:
    expected = temp_root / f"clang_ast_exe_{getpass.getuser()}" / NAMESPACE[stage["key"]]
    if source_cache.resolve() == expected.resolve():
        raise RuntimeError("diagnostic cache must not alias primary cache")
    if expected.exists():
        raise RuntimeError(f"diagnostic cache path already exists: {expected}")
    if not source_cache.is_dir():
        raise RuntimeError(f"primary warm cache directory is missing: {source_cache}")
    expected.parent.mkdir(parents=True, exist_ok=True)
    before = cache_manifest(source_cache)
    shutil.copytree(source_cache, expected)
    copied = cache_manifest(expected)
    if before != copied:
        raise RuntimeError(f"warm cache copy differs from its source for {stage['key']}")
    return expected, canonical_hash(before)


def ccache_stats(cache_dir: Path) -> dict[str, int]:
    stats = base.read_ccache_stats(cache_dir)
    if not stats or "stats_error" in stats:
        raise RuntimeError(f"could not read ccache stats from {cache_dir}: {stats}")
    return {key: int(stats.get(key, 0)) for key in ("cacheable_calls", "hits", "misses", "uncacheable_calls")}


def junit_counts(root: Path) -> dict[str, int | float]:
    tests = failures = skipped = 0
    duration = 0.0
    for path in sorted(root.glob("TEST-*.xml")):
        suite = ET.parse(path).getroot()
        tests += int(suite.attrib.get("tests", 0))
        failures += int(suite.attrib.get("failures", 0)) + int(suite.attrib.get("errors", 0))
        skipped += int(suite.attrib.get("skipped", 0))
        duration += float(suite.attrib.get("time", 0.0))
    return {"total_tests": tests, "passed_tests": tests - failures - skipped,
            "failed_tests": failures, "skipped_tests": skipped, "junit_aggregate_s": duration}


def junit_identity_sha256(root: Path) -> str:
    records = []
    for path in sorted(root.glob("TEST-*.xml")):
        suite = ET.parse(path).getroot()
        for case in suite.findall("testcase"):
            failed = case.find("failure") is not None or case.find("error") is not None
            skipped = case.find("skipped") is not None
            status = "failed" if failed else "skipped" if skipped else "passed"
            records.append({
                "class": case.attrib.get("classname", ""),
                "name": case.attrib.get("name", ""),
                "status": status,
            })
    records.sort(key=lambda item: (item["class"], item["name"], item["status"]))
    canonical = json.dumps(records, sort_keys=True, separators=(",", ":")).encode()
    return hashlib.sha256(canonical).hexdigest()


def parse_time(path: Path) -> dict[str, float | int]:
    values: dict[str, float | int] = {}
    if not path.is_file():
        return values
    for line in path.read_text().splitlines():
        key, sep, value = line.partition("=")
        if sep:
            values[key] = int(float(value)) if key in {"exit_status", "max_rss_kb"} else float(value)
    return values


def task_compilation_lines(log_path: Path) -> list[str]:
    pattern = re.compile(r"^> Task .*:(?:compileJava|compileTestJava|generateProto(?:JavaBindings|SchemaHash|DescriptorHash)?)(?:\s+(.*))?$")
    lines = []
    for line in log_path.read_text(errors="replace").splitlines():
        match = pattern.match(line.strip())
        if match and (match.group(1) or "").strip() not in {"UP-TO-DATE", "NO-SOURCE", "SKIPPED"}:
            lines.append(line.strip())
    return lines


def test_task_was_executed(log_path: Path) -> bool:
    pattern = re.compile(r"^> Task :(?:ClangAstParser:)?test(?:\s+(.*))?$")
    for line in log_path.read_text(errors="replace").splitlines():
        match = pattern.match(line.strip())
        if match and (match.group(1) or "").strip() not in {"UP-TO-DATE", "NO-SOURCE", "SKIPPED"}:
            return True
    return False


def observed_native_tool(stage: dict, log_path: Path) -> tuple[str, str]:
    marker = "Using local clang-dumper build:"
    paths = {
        line.split(marker, 1)[1].strip()
        for line in log_path.read_text(errors="replace").splitlines()
        if marker in line
    }
    if len(paths) != 1:
        raise RuntimeError(f"expected one actual native tool path in {log_path}, found {sorted(paths)}")
    actual_path = Path(next(iter(paths))).resolve()
    expected_path = Path(stage["dumper"]).resolve()
    if actual_path != expected_path:
        raise RuntimeError(f"test used unexpected native tool: expected={expected_path}, actual={actual_path}")
    actual_sha256 = base.sha256_file(actual_path)
    if actual_sha256 != stage["native_binary_sha256"]:
        raise RuntimeError(f"actual native tool hash changed for {stage['key']}: {actual_sha256}")
    return str(actual_path), actual_sha256


def observed_test_executor_args(log_path: Path) -> list[str]:
    marker = "Command: "
    commands = []
    for line in log_path.read_text(errors="replace").splitlines():
        if "Gradle Test Executor" not in line or marker not in line:
            continue
        try:
            arguments = shlex.split(line.split(marker, 1)[1].strip())
        except ValueError as exc:
            raise RuntimeError(f"could not parse Gradle Test Executor command: {line}") from exc
        if arguments:
            commands.append(arguments)
    if len(commands) != 1:
        raise RuntimeError(f"expected one actual Gradle Test Executor command in {log_path}, found {len(commands)}")
    return commands[0]


def normalize_executor_args(arguments: list[str]) -> tuple[list[str], list[dict[str, str]]]:
    normalized = []
    response_files = []
    for argument in arguments:
        if argument.startswith("@"):
            response_file = Path(argument[1:]).resolve()
            if not response_file.is_file():
                raise RuntimeError(f"Gradle worker classpath response file is missing: {response_file}")
            response_sha256 = base.sha256_file(response_file)
            response_files.append({"path": str(response_file), "sha256": response_sha256})
            normalized.append(f"@response-file-sha256:{response_sha256}")
        elif argument.startswith("-javaagent:"):
            continue
        else:
            normalized.append(argument)
    return normalized, response_files


def write_csv(rows: list[dict[str, object]], path: Path) -> None:
    fields = ("ordinal", "round", "stage", "agent", "valid", "elapsed_s", "junit_aggregate_s",
              "wall_minus_junit_residual_s", "max_rss_kb", "cacheable_calls_delta", "cache_hits_delta",
              "cache_misses_delta", "uncacheable_calls_delta", "total_tests", "passed_tests",
              "failed_tests", "skipped_tests")
    with path.open("w", newline="") as output:
        writer = csv.DictWriter(output, fieldnames=fields, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


def summarize(results: list[dict[str, object]]) -> dict[str, object]:
    metrics = ("elapsed_s", "junit_aggregate_s", "wall_minus_junit_residual_s")
    by_stage: dict[str, object] = {}
    treatment_effects: dict[str, dict[str, float]] = {}
    for key in STAGE_KEYS:
        rows = [row for row in results if row["stage"] == key]
        stage_metrics: dict[str, object] = {}
        treatment_effects[key] = {}
        for metric in metrics:
            on = [float(row[metric]) for row in rows if row["agent"] == "on"]
            off = [float(row[metric]) for row in rows if row["agent"] == "off"]
            deltas = [
                float(next(row for row in rows if row["round"] == repeat and row["agent"] == "on")[metric])
                - float(next(row for row in rows if row["round"] == repeat and row["agent"] == "off")[metric])
                for repeat in range(1, 5)
            ]
            on_median = statistics.median(on)
            off_median = statistics.median(off)
            paired_median = statistics.median(deltas)
            treatment_effects[key][metric] = paired_median
            stage_metrics[metric] = {
                "n_per_arm": min(len(on), len(off)),
                "agent_on_median": on_median,
                "agent_off_median": off_median,
                "on_minus_off_paired_median": paired_median,
                "on_minus_off_paired_values": deltas,
                "percent_of_off_median": 100.0 * (on_median - off_median) / off_median if off_median else None,
            }
        by_stage[key] = stage_metrics
    effect_difference = {}
    for metric in metrics:
        per_round = []
        for repeat in range(1, 5):
            protobuf_effect = (
                float(next(row for row in results if row["stage"] == "protobuf" and row["round"] == repeat and row["agent"] == "on")[metric])
                - float(next(row for row in results if row["stage"] == "protobuf" and row["round"] == repeat and row["agent"] == "off")[metric])
            )
            text_effect = (
                float(next(row for row in results if row["stage"] == "ccache-text" and row["round"] == repeat and row["agent"] == "on")[metric])
                - float(next(row for row in results if row["stage"] == "ccache-text" and row["round"] == repeat and row["agent"] == "off")[metric])
            )
            per_round.append(protobuf_effect - text_effect)
        effect_difference[metric] = {
            "per_round_difference_in_differences": per_round,
            "median_difference_in_differences": statistics.median(per_round),
        }
    return {
        "n_per_arm_per_stage": 4,
        "units": "seconds; positive on-minus-off means the JaCoCo agent made the observed timing larger",
        "residual_definition": "max(0, elapsed_s - junit_aggregate_s); a timing-boundary residual, not a Gradle task timer",
        "by_stage": by_stage,
        "protobuf_minus_text_agent_effect_difference_in_differences_s": effect_difference,
    }


def revalidate_prefix(results: list[dict[str, object]], stages: dict[str, dict],
                      expected_cache: dict[str, dict[str, int]], reference_test_sha256: str,
                      output_root: Path) -> tuple[dict[str, dict[str, object]], list[dict[str, object]]]:
    if not results or len(results) >= len(ORDER):
        raise RuntimeError(f"resume requires a nonempty, incomplete result prefix; found {len(results)} rows")
    original_path = output_root / "results.json"
    original_sha256 = base.sha256_file(original_path)
    snapshot = output_root / "results-before-revalidation.json"
    if snapshot.exists():
        raise RuntimeError(f"refusing to overwrite existing result snapshot: {snapshot}")
    shutil.copy2(original_path, snapshot)
    snapshot_sha256 = base.sha256_file(snapshot)
    if snapshot_sha256 != original_sha256:
        raise RuntimeError("immutable pre-revalidation results snapshot hash mismatch")

    worker_baseline: dict[str, dict[str, object]] = {}
    proofs = []
    for index, row in enumerate(results):
        ordinal = index + 1
        expected_stage, expected_agent = ORDER[index]
        if row.get("ordinal") != ordinal or row.get("stage") != expected_stage or row.get("agent") != expected_agent:
            raise RuntimeError(f"partial result prefix diverged from the fixed order at cell {ordinal}")
        stage = stages[expected_stage]
        executor_args = row.get("actual_test_executor_args")
        if not isinstance(executor_args, list):
            raise RuntimeError(f"cell {ordinal} has no recorded actual executor argv")
        normalized_executor, response_files = normalize_executor_args(executor_args)
        worker = row.get("worker_configuration", {})
        worker_args = worker.get("jvm_args", [])
        normalized_worker = [arg for arg in worker_args if not arg.startswith("-javaagent:")]
        identity = {
            "max_heap_size": worker.get("max_heap_size"),
            "jvm_args_without_javaagent": normalized_worker,
            "actual_executor_args_without_javaagent": normalized_executor,
        }
        baseline = worker_baseline.setdefault(expected_stage, identity)
        worker_stable = identity == baseline
        all_worker_agents = [arg for arg in worker_args
                             if arg.startswith(("-javaagent:", "-agentlib:", "-agentpath:", "-Xrun"))]
        worker_only_jacoco = (
            len(all_worker_agents) == 1 and all_worker_agents[0].startswith("-javaagent:")
            and "jacocoagent" in all_worker_agents[0]
        ) if expected_agent == "on" else len(all_worker_agents) == 0
        executor_agents = [arg for arg in executor_args
                           if arg.startswith(("-javaagent:", "-agentlib:", "-agentpath:", "-Xrun"))]
        executor_only_jacoco = (
            len(executor_agents) == 1 and executor_agents[0].startswith("-javaagent:")
            and "jacocoagent" in executor_agents[0]
        ) if expected_agent == "on" else len(executor_agents) == 0
        cache_delta = {
            "cacheable_calls": int(row.get("cacheable_calls_delta", -1)),
            "hits": int(row.get("cache_hits_delta", -1)),
            "misses": int(row.get("cache_misses_delta", -1)),
            "uncacheable_calls": int(row.get("uncacheable_calls_delta", -1)),
        }
        native_path = Path(str(row.get("actual_native_tool", ""))).resolve()
        native_sha256 = base.sha256_file(native_path) if native_path.is_file() else ""
        expected_valid = (
            row.get("return_code") == 0
            and {name: row.get(name) for name in EXPECTED_JAVA} == EXPECTED_JAVA
            and cache_delta == expected_cache[expected_stage]
            and row.get("test_identity_sha256") == reference_test_sha256
            and native_path == Path(stage["dumper"]).resolve()
            and native_sha256 == stage["native_binary_sha256"]
            and not row.get("compile_tasks_not_up_to_date")
            and worker.get("requested_agent") == expected_agent
            and worker.get("jacoco_enabled") is (expected_agent == "on")
            and worker_only_jacoco and executor_only_jacoco
            and [arg for arg in executor_args if arg.startswith("-Xmx")] == ["-Xmx512m"]
            and row.get("test_task_executed") is True
            and all(row.get("report_tasks_skipped", {}).values())
            and worker_stable
        )
        old_valid = row.get("valid")
        old_stability = row.get("worker_args_stable_except_agent")
        row["original_guard_valid"] = old_valid
        row["original_worker_args_stable_except_agent"] = old_stability
        row["worker_args_stable_except_agent"] = worker_stable
        row["actual_executor_classpath_response_files"] = response_files
        row["valid"] = expected_valid
        proof = {
            "ordinal": ordinal,
            "stage": expected_stage,
            "agent": expected_agent,
            "original_guard_valid": old_valid,
            "original_worker_args_stable_except_agent": old_stability,
            "revalidated_worker_args_stable_except_agent": worker_stable,
            "revalidated_valid": expected_valid,
            "normalization": "replace Gradle @response-file path with SHA-256 of response-file contents; remove only the per-run JaCoCo agent token",
            "executor_classpath_response_files": response_files,
            "executor_argv_without_agent": normalized_executor,
        }
        proofs.append(proof)
        if not expected_valid:
            raise RuntimeError(f"resume revalidation failed for cell {ordinal}: {json.dumps(proof, sort_keys=True)}")

    proof_record = {
        "created_at": dt.datetime.now(dt.timezone.utc).isoformat(),
        "original_results_sha256": original_sha256,
        "immutable_original_results_snapshot": str(snapshot),
        "immutable_snapshot_sha256": snapshot_sha256,
        "revalidated_prefix_length": len(results),
        "rows": proofs,
    }
    (output_root / "revalidation.json").write_text(json.dumps(proof_record, indent=2) + "\n")
    return worker_baseline, proofs


def main() -> int:
    args = parse_args()
    matrix_path = args.matrix.resolve()
    output_root = args.output_root.resolve()
    if not 1 <= args.max_cells <= len(ORDER):
        raise SystemExit(f"--max-cells must be between 1 and {len(ORDER)}")
    if output_root.exists() and not args.resume_existing:
        raise SystemExit(f"refusing to reuse diagnostic output root: {output_root}")
    if args.resume_existing and not output_root.is_dir():
        raise SystemExit(f"--resume-existing requires an existing output root: {output_root}")
    matrix = load_matrix(matrix_path)
    plan = matrix["plan"]
    stages = {key: plan["stages"][key] for key in STAGE_KEYS}
    reference_test_identity = matrix["identity_preflight"]["reference_test_ids"]["java"]
    if reference_test_identity.get("count") != EXPECTED_JAVA["total_tests"]:
        raise RuntimeError("primary Java test identity does not contain exactly 116 test cases")
    for stage in stages.values():
        verify_stage(stage)

    primary_rows = {
        key: [row for row in matrix["results"] if row.get("suite") == "java" and row.get("mode") == "warm"
              and row.get("measured") is True and row.get("stage") == key]
        for key in STAGE_KEYS
    }
    source_caches = {}
    expected_cache = {}
    for key, rows in primary_rows.items():
        cache_paths = {Path(row["cache_validation"]["cache_dir"]).resolve() for row in rows}
        if len(cache_paths) != 1:
            raise RuntimeError(f"primary warm runs do not share one cache path for {key}")
        source_caches[key] = next(iter(cache_paths))
        outcomes = {
            (int(row.get("cacheable_calls", 0)), int(row.get("cache_hits", 0)),
             int(row.get("cache_misses", 0)),
             int(row.get("cache_validation", {}).get("uncacheable_calls", 0)))
            for row in rows
        }
        if outcomes != {(208, 207, 1, 0)}:
            raise RuntimeError(f"warm cache outcomes are not repeatable for {key}: {outcomes}")
        expected_cache[key] = EXPECTED_PRIMARY_CACHE.copy()

    inherited_jvm_options = audit_inherited_jvm_options()

    if args.dry_run:
        print(json.dumps({"primary_matrix": str(matrix_path), "output_root": str(output_root),
                          "rounds": len(ORDER) // 4, "max_cells": args.max_cells, "commands": [
                              {"ordinal": i + 1, "round": i // 4 + 1, "stage": stage, "agent": agent}
                              for i, (stage, agent) in enumerate(ORDER[:args.max_cells])]}, indent=2))
        return 0

    temp_roots = {key: output_root / "temp/java" / key for key in STAGE_KEYS}
    cache_dirs: dict[str, Path] = {}
    cache_source_hashes: dict[str, str] = {}
    if args.resume_existing:
        if args.max_cells != len(ORDER):
            raise RuntimeError("resuming an existing partial control requires the complete 16-cell schedule")
        plan_path = output_root / "plan.json"
        results_path = output_root / "results.json"
        if not plan_path.is_file() or not results_path.is_file():
            raise RuntimeError("existing output root is missing its frozen plan or partial results")
        plan_record = json.loads(plan_path.read_text())
        expected_matrix_sha256 = base.sha256_file(matrix_path)
        if plan_record.get("primary_matrix_sha256") != expected_matrix_sha256:
            raise RuntimeError("resume primary-matrix identity differs from the frozen run")
        results_data = json.loads(results_path.read_text())
        if results_data.get("plan", {}).get("primary_matrix_sha256") != expected_matrix_sha256:
            raise RuntimeError("partial results do not belong to the frozen primary matrix")
        results = results_data.get("results", [])
        for key in STAGE_KEYS:
            stage_plan = plan_record.get("stages", {}).get(key, {})
            if Path(stage_plan.get("cache_source", "")).resolve() != source_caches[key]:
                raise RuntimeError(f"resume source cache path changed for {key}")
            cache_dirs[key] = Path(stage_plan.get("cache_clone", "")).resolve()
            if not cache_dirs[key].is_dir() or output_root not in cache_dirs[key].parents:
                raise RuntimeError(f"resume cache clone is missing or escapes output root: {cache_dirs[key]}")
            cache_source_hashes[key] = str(stage_plan.get("cache_source_manifest_sha256", ""))
            current_source_hash = canonical_hash(cache_manifest(source_caches[key]))
            if cache_source_hashes[key] != current_source_hash:
                raise RuntimeError(f"primary cache payload changed since the partial control for {key}")
        worker_baseline, _ = revalidate_prefix(
            results, stages, expected_cache, reference_test_identity["sha256"], output_root)
        (output_root / "results.json").write_text(json.dumps({"plan": plan_record, "results": results}, indent=2) + "\n")
        write_csv(results, output_root / "results.csv")
    else:
        output_root.mkdir(parents=True)
        for key in STAGE_KEYS:
            temp_roots[key].mkdir(parents=True, exist_ok=True)
            cache_dirs[key], cache_source_hashes[key] = ensure_cache_clone(
                stages[key], source_caches[key], temp_roots[key])
            zero = subprocess.run(["ccache", "--zero-stats"],
                                  env={**os.environ, "CCACHE_DIR": str(cache_dirs[key]), "LC_ALL": "C"},
                                  text=True, capture_output=True, check=False)
            if zero.returncode != 0:
                raise RuntimeError(f"could not zero cloned-cache stats: {zero.stderr.strip()}")

        plan_record = {
            "created_at": dt.datetime.now(dt.timezone.utc).isoformat(),
            "primary_matrix": str(matrix_path),
            "primary_matrix_sha256": base.sha256_file(matrix_path),
            "output_root": str(output_root),
            "control": "JaCoCo Test-worker javaagent enabled vs disabled; reporting tasks disabled in both arms",
            "rounds": len(ORDER) // 4,
            "max_cells": args.max_cells,
            "expected_tests": EXPECTED_JAVA,
            "reference_test_identity_sha256": reference_test_identity["sha256"],
            "inherited_jvm_environment_audit": inherited_jvm_options,
            "expected_per_command_cache_delta": expected_cache,
            "order": [{"ordinal": i + 1, "round": i // 4 + 1, "stage": stage, "agent": agent}
                      for i, (stage, agent) in enumerate(ORDER[:args.max_cells])],
            "stages": {key: {"identity": verify_stage(stage), "runtime_manifest_sha256": stage["runtime_manifest_sha256"],
                              "cache_source": str(source_caches[key]), "cache_clone": str(cache_dirs[key]),
                              "cache_source_manifest_sha256": cache_source_hashes[key],
                              "source_metadata": stage}
                       for key, stage in stages.items()},
            "init_scripts": [str(SCRIPT_ROOT / "java-suite.init.gradle"),
                             str(SCRIPT_ROOT / "java-suite-jacoco-agent-control.init.gradle")],
            "timing": "GNU time elapsed for Gradle test with --info logging in both arms; JUnit XML testcase duration reported separately",
        }
        (output_root / "plan.json").write_text(json.dumps(plan_record, indent=2) + "\n")
        results = []
        worker_baseline: dict[str, dict[str, object]] = {}
    try:
        active_order = ORDER[:args.max_cells]
        for ordinal, (key, agent) in enumerate(active_order, start=1):
            if ordinal <= len(results):
                continue
            stage = stages[key]
            verify_stage(stage)
            run_dir = output_root / "runs" / "java" / f"{ordinal:02d}-r{(ordinal - 1) // 4 + 1}-{key}-jacoco-{agent}"
            run_dir.mkdir(parents=True)
            diagnostic_dir = run_dir / "gradle-output"
            cache_dir = cache_dirs[key]
            stats_before = ccache_stats(cache_dir)
            inherited_jvm_options_now = audit_inherited_jvm_options()
            if inherited_jvm_options_now != inherited_jvm_options:
                raise RuntimeError("inherited JVM environment changed after the plan was frozen")
            java_options = os.environ.get("JAVA_TOOL_OPTIONS", "")
            java_options += f" -Djava.io.tmpdir={temp_roots[key]} -Dclava.astWire={stage['wire']}"
            environment = os.environ.copy()
            environment.update({
                "TMPDIR": str(temp_roots[key]), "TMP": str(temp_roots[key]), "TEMP": str(temp_roots[key]),
                "XDG_CACHE_HOME": str(output_root / "cache/java" / key),
                "JAVA_TOOL_OPTIONS": java_options,
                "DEADLINE_JACOCO_AGENT": agent,
                "DEADLINE_JAVA_DIAGNOSTIC_DIR": str(diagnostic_dir),
                "SPECS_JAVA_LIBS_HOME": str(SPECS_JAVA_LIBS_ROOT.resolve()),
                "LARA_FRAMEWORK_HOME": str(LARA_FRAMEWORK_ROOT.resolve()),
            })
            command = ["/usr/bin/time", "-f", base.TIME_FORMAT, "-o", str(run_dir / "time.txt"), "--",
                       "gradle", "--no-daemon", "--offline", "--info",
                       "--init-script", str(SCRIPT_ROOT / "java-suite.init.gradle"),
                       "--init-script", str(SCRIPT_ROOT / "java-suite-jacoco-agent-control.init.gradle")]
            if key == "protobuf":
                command.append(f"-PclangDumperRoot={stage['native_root']}")
            command.extend(["-p", "ClangAstParser", "test"])
            (run_dir / "command.json").write_text(json.dumps({"command": command, "cwd": str(Path(stage['root']) / 'clava'),
                                                               "agent": agent, "stage": key}, indent=2) + "\n")
            log_path = run_dir / "run.log"
            run_started_at = dt.datetime.now(dt.timezone.utc).isoformat()
            started = time.perf_counter()
            with log_path.open("w") as log:
                process = subprocess.run(command, cwd=Path(stage["root"]) / "clava", env=environment,
                                         stdout=log, stderr=subprocess.STDOUT, check=False)
            elapsed = time.perf_counter() - started
            run_finished_at = dt.datetime.now(dt.timezone.utc).isoformat()
            stats_after = ccache_stats(cache_dir)
            delta = {
                "cacheable_calls_delta": stats_after["cacheable_calls"] - stats_before["cacheable_calls"],
                "cache_hits_delta": stats_after["hits"] - stats_before["hits"],
                "cache_misses_delta": stats_after["misses"] - stats_before["misses"],
                "uncacheable_calls_delta": stats_after["uncacheable_calls"] - stats_before["uncacheable_calls"],
            }
            counts = junit_counts(diagnostic_dir / "junit-xml")
            test_identity_sha256 = junit_identity_sha256(diagnostic_dir / "junit-xml")
            test_identity_match = test_identity_sha256 == reference_test_identity["sha256"]
            actual_native_path, actual_native_sha256 = observed_native_tool(stage, log_path)
            worker_path = diagnostic_dir / "worker-configuration.json"
            worker = json.loads(worker_path.read_text()) if worker_path.is_file() else {}
            executor_args = observed_test_executor_args(log_path)
            compilation = task_compilation_lines(log_path)
            agent_present = bool(worker.get("javaagent_args"))
            all_agent_flags = [arg for arg in worker.get("jvm_args", [])
                               if arg.startswith(("-javaagent:", "-agentlib:", "-agentpath:", "-Xrun"))]
            only_jacoco_agent = (
                len(all_agent_flags) == 1 and all_agent_flags[0].startswith("-javaagent:")
                and "jacocoagent" in all_agent_flags[0]
            ) if agent == "on" else len(all_agent_flags) == 0
            normalized_worker_args = [arg for arg in worker.get("jvm_args", []) if not arg.startswith("-javaagent:")]
            executor_args_normalized, executor_classpath_response_files = normalize_executor_args(executor_args)
            worker_identity = {
                "max_heap_size": worker.get("max_heap_size"),
                "jvm_args_without_javaagent": normalized_worker_args,
                "actual_executor_args_without_javaagent": executor_args_normalized,
            }
            if key not in worker_baseline:
                worker_baseline[key] = worker_identity
            worker_stable = worker_identity == worker_baseline[key]
            executor_heap_flags = [arg for arg in executor_args if arg.startswith("-Xmx")]
            heap_is_512m = executor_heap_flags == ["-Xmx512m"]
            executor_agent_flags = [arg for arg in executor_args
                                    if arg.startswith(("-javaagent:", "-agentlib:", "-agentpath:", "-Xrun"))]
            executor_only_jacoco_agent = (
                len(executor_agent_flags) == 1 and executor_agent_flags[0].startswith("-javaagent:")
                and "jacocoagent" in executor_agent_flags[0]
            ) if agent == "on" else len(executor_agent_flags) == 0
            test_executed = test_task_was_executed(log_path)
            log_text = log_path.read_text(errors="replace")
            report_tasks_skipped = {
                task: bool(re.search(rf"{task}\s+SKIPPED", log_text))
                for task in ("jacocoTestReport", "jacocoTestCoverageVerification")
            }
            observed_cache_delta = {
                "cacheable_calls": delta["cacheable_calls_delta"],
                "hits": delta["cache_hits_delta"],
                "misses": delta["cache_misses_delta"],
                "uncacheable_calls": delta["uncacheable_calls_delta"],
            }
            cache_passed = observed_cache_delta == expected_cache[key]
            valid = (process.returncode == 0 and counts == {**EXPECTED_JAVA,
                     "junit_aggregate_s": counts.get("junit_aggregate_s")} and cache_passed
                     and test_identity_match
                     and not compilation and worker.get("requested_agent") == agent
                     and worker.get("jacoco_enabled") is (agent == "on")
                     and agent_present is (agent == "on") and only_jacoco_agent and worker_stable
                     and executor_only_jacoco_agent and heap_is_512m
                     and test_executed and all(report_tasks_skipped.values()))
            time_data = parse_time(run_dir / "time.txt")
            row: dict[str, object] = {
                "ordinal": ordinal, "round": (ordinal - 1) // 4 + 1, "stage": key, "agent": agent,
                "return_code": process.returncode, "valid": valid, "elapsed_s": time_data.get("elapsed_s", elapsed),
                "run_started_at": run_started_at, "run_finished_at": run_finished_at,
                "driver_elapsed_s": elapsed, "junit_aggregate_s": counts.get("junit_aggregate_s", 0.0),
                "wall_minus_junit_residual_s": max(0.0, float(time_data.get("elapsed_s", elapsed))
                                                     - float(counts.get("junit_aggregate_s", 0.0))),
                **time_data, **counts, **delta, "cache_validation_passed": cache_passed,
                "cache_dir": str(cache_dir), "worker_configuration": worker,
                "test_identity_sha256": test_identity_sha256, "test_identity_match": test_identity_match,
                "actual_native_tool": actual_native_path, "actual_native_tool_sha256": actual_native_sha256,
                "worker_args_stable_except_agent": worker_stable,
                "test_task_executed": test_executed, "test_worker_xmx_512m": heap_is_512m,
                "only_expected_jacoco_agent": only_jacoco_agent,
                "actual_test_executor_args": executor_args,
                "actual_executor_classpath_response_files": executor_classpath_response_files,
                "actual_test_executor_only_jacoco_agent": executor_only_jacoco_agent,
                "report_tasks_skipped": report_tasks_skipped,
                "expected_cache_delta": expected_cache[key],
                "observed_cache_delta": observed_cache_delta,
                "compile_tasks_not_up_to_date": compilation, "command": command,
                "run_dir": str(run_dir),
            }
            (run_dir / "summary.json").write_text(json.dumps(row, indent=2) + "\n")
            results.append(row)
            write_csv(results, output_root / "results.csv")
            (output_root / "results.json").write_text(json.dumps({"plan": plan_record, "results": results}, indent=2) + "\n")
            if len(results) == len(ORDER):
                summary = summarize(results)
                (output_root / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
            print(json.dumps({k: row[k] for k in ("ordinal", "round", "stage", "agent", "valid",
                                                  "elapsed_s", "junit_aggregate_s", "cache_hits_delta",
                                                  "cache_misses_delta", "total_tests", "failed_tests")}), flush=True)
            if not valid:
                raise RuntimeError(f"JaCoCo control cell failed acceptance checks; see {run_dir}")
            verify_stage(stage)
        if len(results) != len(active_order):
            raise RuntimeError(f"expected {len(active_order)} diagnostic commands, completed {len(results)}")
        if len(results) == len(ORDER):
            summary = summarize(results)
            (output_root / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
    except Exception as exc:
        (output_root / "failure.txt").write_text(f"{type(exc).__name__}: {exc}\n")
        raise
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

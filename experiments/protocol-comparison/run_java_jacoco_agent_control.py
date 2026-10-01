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
DEFAULT_OUTPUT = SCRIPT_ROOT / "results/deadline-20260930/matrix-r2/java-jacoco-agent-control-r1"
EXPECTED_JAVA = {"total_tests": 116, "passed_tests": 116, "failed_tests": 0, "skipped_tests": 0}
EXPECTED_JAVA_PLAN = {"total": 116, "passed": 116, "failed": 0, "skipped": 0}
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


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--matrix", type=Path, default=DEFAULT_MATRIX)
    parser.add_argument("--output-root", type=Path, default=DEFAULT_OUTPUT)
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
    return git(root, "status", "--porcelain=v1", "--untracked-files=all").splitlines()


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
    return data


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


def write_csv(rows: list[dict[str, object]], path: Path) -> None:
    fields = ("ordinal", "round", "stage", "agent", "valid", "elapsed_s", "junit_aggregate_s",
              "gradle_non_test_elapsed_s", "max_rss_kb", "cacheable_calls_delta", "cache_hits_delta",
              "cache_misses_delta", "uncacheable_calls_delta", "total_tests", "passed_tests",
              "failed_tests", "skipped_tests")
    with path.open("w", newline="") as output:
        writer = csv.DictWriter(output, fieldnames=fields, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)


def summarize(results: list[dict[str, object]]) -> dict[str, object]:
    metrics = ("elapsed_s", "junit_aggregate_s", "gradle_non_test_elapsed_s")
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
    effect_difference = {
        metric: treatment_effects["protobuf"][metric] - treatment_effects["ccache-text"][metric]
        for metric in metrics
    }
    return {
        "n_per_arm_per_stage": 4,
        "units": "seconds; positive on-minus-off means the JaCoCo agent made the observed timing larger",
        "by_stage": by_stage,
        "protobuf_minus_text_agent_effect_s": effect_difference,
    }


def main() -> int:
    args = parse_args()
    matrix_path = args.matrix.resolve()
    output_root = args.output_root.resolve()
    if output_root.exists():
        raise SystemExit(f"refusing to reuse diagnostic output root: {output_root}")
    matrix = load_matrix(matrix_path)
    plan = matrix["plan"]
    stages = {key: plan["stages"][key] for key in STAGE_KEYS}
    for stage in stages.values():
        verify_stage(stage)

    primary_rows = {
        key: [row for row in matrix["results"] if row.get("suite") == "java" and row.get("mode") == "warm"
              and row.get("measured") is True and row.get("stage") == key]
        for key in STAGE_KEYS
    }
    source_caches = {}
    for key, rows in primary_rows.items():
        cache_paths = {Path(row["cache_validation"]["cache_dir"]).resolve() for row in rows}
        if len(cache_paths) != 1:
            raise RuntimeError(f"primary warm runs do not share one cache path for {key}")
        source_caches[key] = next(iter(cache_paths))

    if args.dry_run:
        print(json.dumps({"primary_matrix": str(matrix_path), "output_root": str(output_root),
                          "rounds": len(ORDER) // 4, "commands": [
                              {"ordinal": i + 1, "round": i // 4 + 1, "stage": stage, "agent": agent}
                              for i, (stage, agent) in enumerate(ORDER)]}, indent=2))
        return 0

    output_root.mkdir(parents=True)
    temp_roots = {key: output_root / "temp/java" / key for key in STAGE_KEYS}
    cache_dirs: dict[str, Path] = {}
    cache_source_hashes: dict[str, str] = {}
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
        "expected_tests": EXPECTED_JAVA,
        "order": [{"ordinal": i + 1, "round": i // 4 + 1, "stage": stage, "agent": agent}
                  for i, (stage, agent) in enumerate(ORDER)],
        "stages": {key: {"identity": verify_stage(stage), "runtime_manifest_sha256": stage["runtime_manifest_sha256"],
                          "cache_source": str(source_caches[key]), "cache_clone": str(cache_dirs[key]),
                          "cache_source_manifest_sha256": cache_source_hashes[key],
                          "source_metadata": stage}
                   for key, stage in stages.items()},
        "init_scripts": [str(SCRIPT_ROOT / "java-suite.init.gradle"),
                         str(SCRIPT_ROOT / "java-suite-jacoco-agent-control.init.gradle")],
        "timing": "GNU time elapsed for Gradle test; JUnit XML testcase duration reported separately",
    }
    (output_root / "plan.json").write_text(json.dumps(plan_record, indent=2) + "\n")
    results: list[dict[str, object]] = []
    worker_baseline: dict[str, dict[str, object]] = {}
    try:
        for ordinal, (key, agent) in enumerate(ORDER, start=1):
            stage = stages[key]
            verify_stage(stage)
            run_dir = output_root / "runs" / "java" / f"{ordinal:02d}-r{(ordinal - 1) // 4 + 1}-{key}-jacoco-{agent}"
            run_dir.mkdir(parents=True)
            diagnostic_dir = run_dir / "gradle-output"
            cache_dir = cache_dirs[key]
            stats_before = ccache_stats(cache_dir)
            java_options = os.environ.get("JAVA_TOOL_OPTIONS", "")
            java_options += f" -Djava.io.tmpdir={temp_roots[key]} -Dclava.astWire={stage['wire']}"
            environment = os.environ.copy()
            environment.update({
                "TMPDIR": str(temp_roots[key]), "TMP": str(temp_roots[key]), "TEMP": str(temp_roots[key]),
                "XDG_CACHE_HOME": str(output_root / "cache/java" / key),
                "JAVA_TOOL_OPTIONS": java_options,
                "DEADLINE_JACOCO_AGENT": agent,
                "DEADLINE_JAVA_DIAGNOSTIC_DIR": str(diagnostic_dir),
            })
            command = ["/usr/bin/time", "-f", base.TIME_FORMAT, "-o", str(run_dir / "time.txt"), "--",
                       "gradle", "--no-daemon", "--offline",
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
            delta = {f"{name}_delta": stats_after[name] - stats_before[name] for name in stats_before}
            counts = junit_counts(diagnostic_dir / "junit-xml")
            worker_path = diagnostic_dir / "worker-configuration.json"
            worker = json.loads(worker_path.read_text()) if worker_path.is_file() else {}
            compilation = task_compilation_lines(log_path)
            agent_present = bool(worker.get("javaagent_args"))
            normalized_worker_args = [arg for arg in worker.get("jvm_args", []) if not arg.startswith("-javaagent:")]
            worker_identity = {
                "max_heap_size": worker.get("max_heap_size"),
                "jvm_args_without_javaagent": normalized_worker_args,
            }
            if key not in worker_baseline:
                worker_baseline[key] = worker_identity
            worker_stable = worker_identity == worker_baseline[key]
            heap_flags = [arg for arg in worker.get("jvm_args", []) if arg.startswith("-Xmx")]
            heap_is_512m = heap_flags == ["-Xmx512m"]
            test_executed = test_task_was_executed(log_path)
            log_text = log_path.read_text(errors="replace")
            report_tasks_skipped = {
                task: bool(re.search(rf"{task}\s+SKIPPED", log_text))
                for task in ("jacocoTestReport", "jacocoTestCoverageVerification")
            }
            cache_passed = delta["cacheable_calls_delta"] > 0 and delta["cache_hits_delta"] > 0
            valid = (process.returncode == 0 and counts == {**EXPECTED_JAVA,
                     "junit_aggregate_s": counts.get("junit_aggregate_s")} and cache_passed
                     and not compilation and worker.get("requested_agent") == agent
                     and worker.get("jacoco_enabled") is (agent == "on")
                     and agent_present is (agent == "on") and worker_stable
                     and heap_is_512m and test_executed and all(report_tasks_skipped.values()))
            time_data = parse_time(run_dir / "time.txt")
            row: dict[str, object] = {
                "ordinal": ordinal, "round": (ordinal - 1) // 4 + 1, "stage": key, "agent": agent,
                "return_code": process.returncode, "valid": valid, "elapsed_s": time_data.get("elapsed_s", elapsed),
                "run_started_at": run_started_at, "run_finished_at": run_finished_at,
                "driver_elapsed_s": elapsed, "junit_aggregate_s": counts.get("junit_aggregate_s", 0.0),
                "gradle_non_test_elapsed_s": max(0.0, float(time_data.get("elapsed_s", elapsed))
                                                   - float(counts.get("junit_aggregate_s", 0.0))),
                **time_data, **counts, **delta, "cache_validation_passed": cache_passed,
                "cache_dir": str(cache_dir), "worker_configuration": worker,
                "worker_args_stable_except_agent": worker_stable,
                "test_task_executed": test_executed, "test_worker_xmx_512m": heap_is_512m,
                "report_tasks_skipped": report_tasks_skipped,
                "compile_tasks_not_up_to_date": compilation, "command": command,
                "run_dir": str(run_dir),
            }
            (run_dir / "summary.json").write_text(json.dumps(row, indent=2) + "\n")
            results.append(row)
            write_csv(results, output_root / "results.csv")
            (output_root / "results.json").write_text(json.dumps({"plan": plan_record, "results": results}, indent=2) + "\n")
            if len(results) == 16:
                summary = summarize(results)
                (output_root / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
            print(json.dumps({k: row[k] for k in ("ordinal", "round", "stage", "agent", "valid",
                                                  "elapsed_s", "junit_aggregate_s", "cache_hits_delta",
                                                  "cache_misses_delta", "total_tests", "failed_tests")}), flush=True)
            if not valid:
                raise RuntimeError(f"JaCoCo control cell failed acceptance checks; see {run_dir}")
            verify_stage(stage)
        if len(results) != 16:
            raise RuntimeError(f"expected 16 diagnostic commands, completed {len(results)}")
        summary = summarize(results)
        (output_root / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
    except Exception as exc:
        (output_root / "failure.txt").write_text(f"{type(exc).__name__}: {exc}\n")
        raise
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

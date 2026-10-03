#!/usr/bin/env python3
"""Run matched bypass observations for eager FlatBuffers, Text, and Protobuf.

The eager runner comes from this checkout. Text and Protobuf are historical
controls loaded from the frozen validation checkout. All three runners use
this checkout's Clava-JS tests and Vitest config. Each control receives a fresh
copy of its supplied Java runtime and its matching native dumper.

This driver only supports bypass observations. It sets CCACHE_DISABLE=true and
builds a PATH link farm that omits the ccache executable, so cacheable commands
cannot silently run in pass-through mode. The runner summaries retain the
Vitest wall-time boundary; setup, staging, and report writing are outside it.
"""

from __future__ import annotations

import argparse
import contextlib
import csv
import datetime as dt
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import sys
import shutil
import time
from types import ModuleType
from typing import Any, Callable


SCRIPT_ROOT = Path(__file__).resolve().parent
EXPERIMENT_ROOT = SCRIPT_ROOT.parent
CLAVA_ROOT = EXPERIMENT_ROOT.parents[1]
CLAVA_JS_ROOT = CLAVA_ROOT / "Clava-JS"
RESULTS_ROOT = SCRIPT_ROOT / "results"
VALIDATION_ROOT = Path.home() / ".cache" / "ast-flatbuffers-release-validation"

EAGER_RUNNER = SCRIPT_ROOT / "run_suite.py"
TEXT_CHECKOUT = VALIDATION_ROOT / "text-build"
PROTOBUF_CHECKOUT = VALIDATION_ROOT / "protobuf-build"
DEFAULT_EAGER_RUNTIME = VALIDATION_ROOT / "eager-runtime"

JS_TEST_FILTER = (
    r"^(?!(?:CxxTest OmpThreadsExplore|CudaTest Cuda|CudaTest CudaMatrixMul|"
    r"CudaTest CudaQuery)$).*$"
)
ENVIRONMENT_EXCLUSIONS = {
    "CxxTest OmpThreadsExplore",
    "CudaTest Cuda",
    "CudaTest CudaMatrixMul",
    "CudaTest CudaQuery",
}
EXPECTED_COUNTS = {
    "total_tests": 164,
    "passed_tests": 158,
    "failed_tests": 0,
    "pending_tests": 6,
}
NO_CCACHE_STATS = "ccache is intentionally absent from PATH for this bypass observation\n"
TIMING_BOUNDARY = (
    "runner wall_s: monotonic elapsed time from immediately before launching the GNU-time-wrapped "
    "npm/Vitest command until that command exits; excludes runtime staging, setup, and report parsing"
)
HOST_ENVIRONMENT = os.environ.copy()

sys.dont_write_bytecode = True
sys.path.insert(0, str(EXPERIMENT_ROOT))
from benchmark_environment import make_path_without_ccache


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--text-checkout", type=Path, default=TEXT_CHECKOUT)
    parser.add_argument("--protobuf-checkout", type=Path, default=PROTOBUF_CHECKOUT)
    parser.add_argument("--eager-runtime", type=Path, default=DEFAULT_EAGER_RUNTIME)
    parser.add_argument("--text-runtime", type=Path)
    parser.add_argument("--protobuf-runtime", type=Path)
    parser.add_argument("--text-dumper", type=Path)
    parser.add_argument("--protobuf-dumper", type=Path)
    parser.add_argument(
        "--output-root",
        type=Path,
        help="New result directory below suite/results; defaults to a UTC timestamped directory.",
    )
    parser.add_argument(
        "--vitest-arg",
        action="append",
        default=[],
        help="Scheduling argument applied to all runs. Use only the listed safe scheduler options.",
    )
    args = parser.parse_args()
    args.text_runtime = args.text_runtime or (
        args.text_checkout / "clava" / "ClavaWeaver" / "build" / "install" / "ClavaWeaver"
    )
    args.protobuf_runtime = args.protobuf_runtime or (
        args.protobuf_checkout / "clava" / "ClavaWeaver" / "build" / "install" / "ClavaWeaver"
    )
    args.text_dumper = args.text_dumper or args.text_checkout / "clang-dumper" / "build" / "tool"
    args.protobuf_dumper = args.protobuf_dumper or args.protobuf_checkout / "clang-dumper" / "build" / "tool"
    args.text_runner = (
        args.text_checkout / "clava" / "experiments" / "flatbuffers" / "suite" / "run_suite.py"
    )
    args.protobuf_runner = (
        args.protobuf_checkout / "clava" / "experiments" / "protobuf" / "suite" / "run_suite.py"
    )
    validate_vitest_args(args.vitest_arg)
    return args


def validate_vitest_args(arguments: list[str]) -> None:
    boolean_scheduler_args = {"--fileParallelism", "--no-fileParallelism"}
    valued_scheduler_args = ("--maxWorkers=", "--minWorkers=", "--pool=")
    invalid_scheduler_args = [
        arg for arg in arguments
        if arg not in boolean_scheduler_args
        and not any(arg.startswith(prefix) and len(arg) > len(prefix) for prefix in valued_scheduler_args)
    ]
    if invalid_scheduler_args:
        raise SystemExit(
            "--vitest-arg only accepts --maxWorkers=N, --minWorkers=N, --pool=NAME, "
            "--fileParallelism, or --no-fileParallelism"
        )


def git_status(repo: Path) -> dict[str, Any]:
    revision = subprocess_text(["git", "-C", str(repo), "rev-parse", "HEAD"])
    status = subprocess_text(["git", "-C", str(repo), "status", "--porcelain=v1", "--untracked-files=all"])
    lines = status.splitlines()
    return {
        "root": str(repo.resolve()),
        "revision": revision.strip() or "unknown",
        "dirty": bool(lines),
        "porcelain": lines,
    }


def subprocess_text(command: list[str]) -> str:
    import subprocess

    completed = subprocess.run(command, text=True, capture_output=True, check=False)
    return completed.stdout if completed.returncode == 0 else ""


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def jar_manifest(root: Path) -> dict[str, Any]:
    entries = {
        str(path.relative_to(root)): sha256_file(path)
        for path in sorted(root.rglob("*.jar"))
        if path.is_file()
    }
    canonical = json.dumps(entries, sort_keys=True, separators=(",", ":")).encode()
    return {
        "root": str(root.resolve()),
        "jar_count": len(entries),
        "files": entries,
        "manifest_sha256": hashlib.sha256(canonical).hexdigest(),
    }


def require_inputs(args: argparse.Namespace) -> None:
    for path, label in (
        (EAGER_RUNNER, "eager runner"),
        (args.text_runner, "historical Text runner"),
        (args.protobuf_runner, "historical Protobuf runner"),
        (args.text_checkout / "clava", "historical Text Clava checkout"),
        (args.protobuf_checkout / "clava", "historical Protobuf Clava checkout"),
        (args.eager_runtime, "eager runtime"),
        (args.text_runtime, "Text runtime"),
        (args.protobuf_runtime, "Protobuf runtime"),
        (args.text_dumper, "Text clang-dumper"),
        (args.protobuf_dumper, "Protobuf clang-dumper"),
        (CLAVA_JS_ROOT / "package.json", "current Clava-JS package"),
    ):
        if not path.exists():
            raise SystemExit(f"{label} does not exist: {path}")
    for runtime, label in (
        (args.eager_runtime, "eager runtime"),
        (args.text_runtime, "Text runtime"),
        (args.protobuf_runtime, "Protobuf runtime"),
    ):
        if not runtime.is_dir():
            raise SystemExit(f"{label} is not a directory: {runtime}")
        parser_jar = runtime / "lib" / "ClangAstParser.jar"
        if not parser_jar.is_file():
            raise SystemExit(f"{label} has no lib/ClangAstParser.jar: {runtime}")
    for dumper, label in (
        (args.text_dumper, "Text clang-dumper"),
        (args.protobuf_dumper, "Protobuf clang-dumper"),
    ):
        if not dumper.is_file() or not os.access(dumper, os.X_OK):
            raise SystemExit(f"{label} is not an executable file: {dumper}")
def load_module(script: Path, module_name: str) -> ModuleType:
    sys.dont_write_bytecode = True
    spec = importlib.util.spec_from_file_location(module_name, script)
    if spec is None or spec.loader is None:
        raise RuntimeError(f"cannot load runner module: {script}")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def invoke_module(
    module: ModuleType,
    argv: list[str],
    log_path: Path,
    before: set[Path],
    output_root: Path,
) -> tuple[int, Path | None, str | None, float]:
    previous_argv = sys.argv
    started = time.perf_counter()
    try:
        sys.argv = [str(module.__file__), *argv]
        with log_path.open("w") as log, contextlib.redirect_stdout(log), contextlib.redirect_stderr(log):
            return_code = int(module.main())
    except SystemExit as error:
        return_code = int(error.code) if isinstance(error.code, int) else 1
    except Exception as error:
        return_code = 1
        with log_path.open("a") as log:
            log.write(f"comparison driver caught {type(error).__name__}: {error}\n")
    finally:
        sys.argv = previous_argv
    elapsed = time.perf_counter() - started
    summaries = set(output_root.glob("*/summary.json")) - before
    summary_path = next(iter(summaries)) if len(summaries) == 1 else (sorted(summaries)[-1] if summaries else None)
    error_text = None if summary_path is not None else "runner did not produce a new summary.json"
    return return_code, summary_path, error_text, elapsed


def load_rows(summary_path: Path) -> tuple[list[dict[str, str]], str | None]:
    rows_path = summary_path.parent / "per_test_timings.csv"
    if not rows_path.is_file():
        return [], f"missing per-test results file: {rows_path}"
    with rows_path.open(newline="") as source:
        return list(csv.DictReader(source)), None


def summary_failed_count(summary: dict[str, Any]) -> int | None:
    value = summary.get("failed_tests")
    if isinstance(value, list):
        return len(value)
    if isinstance(value, int):
        return value
    return None


def validate_observation(
    summary: dict[str, Any], rows: list[dict[str, str]], path_is_clean: bool
) -> list[str]:
    errors: list[str] = []
    for key, expected in EXPECTED_COUNTS.items():
        actual = summary_failed_count(summary) if key == "failed_tests" else summary.get(key)
        if actual != expected:
            errors.append(f"{key}={actual!r}, expected {expected}")
    if len(rows) != EXPECTED_COUNTS["total_tests"]:
        errors.append(f"per-test row count={len(rows)}, expected {EXPECTED_COUNTS['total_tests']}")

    by_name: dict[str, list[dict[str, str]]] = {}
    for row in rows:
        by_name.setdefault(row.get("test_name", ""), []).append(row)
    for excluded in sorted(ENVIRONMENT_EXCLUSIONS):
        matches = by_name.get(excluded, [])
        if len(matches) != 1:
            errors.append(f"expected one filtered environment test named {excluded!r}, found {len(matches)}")
        elif matches[0].get("status", "").lower() not in {"skipped", "pending", "todo"}:
            errors.append(f"environment test {excluded!r} was not excluded")
    pending_names = {
        name
        for name, matches in by_name.items()
        if any(row.get("status", "").lower() in {"skipped", "pending", "todo"} for row in matches)
    }
    if len(pending_names - ENVIRONMENT_EXCLUSIONS) != 2:
        errors.append(
            f"original pending test count={len(pending_names - ENVIRONMENT_EXCLUSIONS)}, expected 2"
        )
    if summary.get("mode") != "bypass":
        errors.append(f"mode={summary.get('mode')!r}, expected bypass")
    environment = summary.get("environment", {})
    if str(environment.get("CCACHE_DISABLE", "")).lower() not in {"1", "true", "yes", "on"}:
        errors.append("runner summary does not record CCACHE_DISABLE=true")
    counters = summary.get("ccache_stats", {})
    for key in ("cacheable_calls", "direct", "misses", "uncacheable_calls"):
        if int(counters.get(key, 0) or 0) != 0:
            errors.append(f"ccache {key}={counters.get(key)!r}, expected zero")
    for key in ("ccache_cacheable_calls", "ccache_direct_hits", "ccache_misses", "ccache_uncacheable_calls"):
        if int(summary.get(key, 0) or 0) != 0:
            errors.append(f"{key}={summary.get(key)!r}, expected zero")
    if not path_is_clean:
        errors.append("ccache resolves from the comparison PATH")
    if not isinstance(summary.get("wall_s"), (int, float)) or summary["wall_s"] <= 0:
        errors.append("runner summary has no positive wall_s measurement")
    return errors


def test_identities(rows: list[dict[str, str]]) -> list[tuple[str, str]]:
    return sorted((row.get("suite_file", ""), row.get("test_name", "")) for row in rows)


def base_environment(no_ccache_path: str) -> dict[str, str]:
    environment = HOST_ENVIRONMENT.copy()
    environment["PATH"] = no_ccache_path
    environment["CCACHE_DISABLE"] = "true"
    environment.pop("AST_WIRE_FLAT", None)
    environment.pop("AST_WIRE_DENSE_TEXT", None)
    java_options = environment.get("JAVA_TOOL_OPTIONS", "")
    environment["JAVA_TOOL_OPTIONS"] = " ".join(
        item for item in java_options.split() if not item.startswith("-Dclava.astWire=")
    )
    return environment


def run_eager(args: argparse.Namespace, run_root: Path, path: str) -> dict[str, Any]:
    module = load_module(EAGER_RUNNER, "flatbuffers_eager_suite_runner")
    module.ccache = lambda _command, _cache: NO_CCACHE_STATS
    output_root = run_root / "eager"
    output_root.mkdir()
    log_path = run_root / "runner-logs" / "eager.log"
    before = set(output_root.glob("*/summary.json"))
    argv = [
        "--mode", "bypass",
        "--runtime-root", str(args.eager_runtime.resolve()),
        "--output-root", str(output_root),
    ]
    for item in args.vitest_arg:
        argv.append(f"--vitest-arg={item}")
    os.environ.clear()
    os.environ.update(base_environment(path))
    code, summary_path, error, driver_s = invoke_module(module, argv, log_path, before, output_root)
    return observation_result(
        "eager-flatbuffers", code, summary_path, error, driver_s, args.eager_runtime,
        CLAVA_ROOT, EAGER_RUNNER, None, path,
    )


def configure_historical_module(
    module: ModuleType, *, runtime: Path,
    experiment_root: Path, result_root: Path, runtime_kind: str,
) -> None:
    module.CLAVA_ROOT = CLAVA_ROOT
    module.CLAVA_JS_ROOT = CLAVA_JS_ROOT
    module.EXPERIMENT_ROOT = experiment_root
    module.RESULTS_ROOT = result_root
    module.git_revision = lambda repo: git_status(Path(repo))["revision"] if Path(repo).exists() else "unknown"

    if runtime_kind == "text":
        original_stage = module.stage_java_runtime
        module.stage_java_runtime = lambda _source, destination, selected_dumper: original_stage(
            runtime.resolve(), destination, selected_dumper
        )
        original_manifest = module.runtime_jar_manifest
        module.runtime_jar_manifest = lambda _root: original_manifest(runtime.resolve())
        module.ccache = lambda _command, _cache: NO_CCACHE_STATS
    else:
        module.run_ccache = lambda _args, _cache: NO_CCACHE_STATS


def run_text(args: argparse.Namespace, run_root: Path, path: str) -> dict[str, Any]:
    module = load_module(args.text_runner, "historical_text_suite_runner")
    output_root = run_root / "text"
    output_root.mkdir()
    logs = run_root / "runner-logs"
    log_path = logs / "text.log"
    configure_historical_module(
        module, runtime=args.text_runtime, experiment_root=EXPERIMENT_ROOT, result_root=output_root,
        runtime_kind="text",
    )
    before = set(output_root.glob("*/summary.json"))
    argv = [
        "--mode", "bypass", "--format", "text",
        "--dumper", str(args.text_dumper.resolve()),
        "--dumper-repo", str((args.text_checkout / "clang-dumper").resolve()),
        "--output-root", str(output_root),
    ]
    for item in ["--testNamePattern", JS_TEST_FILTER, *args.vitest_arg]:
        argv.append(f"--vitest-arg={item}")
    os.environ.clear()
    os.environ.update(base_environment(path))
    code, summary_path, error, driver_s = invoke_module(module, argv, log_path, before, output_root)
    return observation_result(
        "text", code, summary_path, error, driver_s, args.text_runtime,
        args.text_checkout / "clava", args.text_runner, args.text_dumper, path,
    )


def run_protobuf(args: argparse.Namespace, run_root: Path, path: str) -> dict[str, Any]:
    module = load_module(args.protobuf_runner, "historical_protobuf_suite_runner")
    output_root = run_root / "protobuf"
    output_root.mkdir()
    log_path = run_root / "runner-logs" / "protobuf.log"
    configure_historical_module(
        module, runtime=args.protobuf_runtime, experiment_root=EXPERIMENT_ROOT, result_root=output_root,
        runtime_kind="protobuf",
    )
    before = set(output_root.glob("*/summary.json"))
    argv = [
        "--implementation", "protobuf", "--mode", "bypass",
        "--dumper", str(args.protobuf_dumper.resolve()),
        "--runtime", str(args.protobuf_runtime.resolve()),
        "--dumper-repo", str((args.protobuf_checkout / "clang-dumper").resolve()),
        "--runtime-repo", str((args.protobuf_checkout / "clava").resolve()),
        "--wire-property", "clava.astWire", "--wire-value", "protobuf",
        "--output-root", str(output_root),
    ]
    for item in ["--testNamePattern", JS_TEST_FILTER, *args.vitest_arg]:
        argv.append(f"--vitest-arg={item}")
    os.environ.clear()
    os.environ.update(base_environment(path))
    code, summary_path, error, driver_s = invoke_module(module, argv, log_path, before, output_root)
    return observation_result(
        "protobuf", code, summary_path, error, driver_s, args.protobuf_runtime,
        args.protobuf_checkout / "clava", args.protobuf_runner, args.protobuf_dumper, path,
    )


def observation_result(
    implementation: str, return_code: int, summary_path: Path | None, error: str | None,
    driver_elapsed_s: float, runtime_root: Path, source_repo: Path, runner_path: Path,
    dumper_path: Path | None, path: str,
) -> dict[str, Any]:
    result: dict[str, Any] = {
        "implementation": implementation,
        "driver_return_code": return_code,
        "driver_elapsed_s": driver_elapsed_s,
        "summary_path": str(summary_path) if summary_path else None,
        "error": error,
        "mode": "bypass",
        "cache_state": {
            "mode": "bypass",
            "CCACHE_DISABLE": "true",
            "ccache_resolves_from_PATH": shutil.which("ccache", path=path) is not None,
            "ccache_counters_expected": 0,
        },
        "timing_boundary": TIMING_BOUNDARY,
        "source": {
            "repo": git_status(source_repo),
            "clava_repo": git_status(CLAVA_ROOT),
            "runner": str(runner_path.resolve()),
            "runner_sha256": sha256_file(runner_path),
            "runtime_root": str(runtime_root.resolve()),
            "runtime_jars": jar_manifest(runtime_root),
            "dumper": str(dumper_path.resolve()) if dumper_path else None,
            "dumper_sha256": sha256_file(dumper_path) if dumper_path and dumper_path.is_file() else None,
        },
        "validation_errors": [],
    }
    if summary_path is None:
        result["validation_errors"] = [error or "summary missing"]
        return result
    summary = json.loads(summary_path.read_text())
    rows, row_error = load_rows(summary_path)
    errors = validate_observation(summary, rows, path_is_clean=not result["cache_state"]["ccache_resolves_from_PATH"])
    if row_error:
        errors.append(row_error)
    if return_code != 0:
        errors.append(f"runner return code={return_code}")
    staged_runtime = next(
        (candidate for candidate in (summary_path.parent / "runtime", summary_path.parent / "java-binaries") if candidate.is_dir()),
        None,
    )
    result.update({
        "summary": str(summary_path),
        "test_counts": {
            key: (summary_failed_count(summary) if key == "failed_tests" else summary.get(key))
            for key in EXPECTED_COUNTS
        },
        "wall_s": summary.get("wall_s"),
        "ccache_stats": summary.get("ccache_stats", {}),
        "ccache_counters": {
            "cacheable_calls": summary.get("ccache_cacheable_calls", 0),
            "direct_hits": summary.get("ccache_direct_hits", 0),
            "misses": summary.get("ccache_misses", 0),
            "uncacheable_calls": summary.get("ccache_uncacheable_calls", 0),
        },
        "runtime_summary": summary.get("release") or summary.get("runtime_jar_manifest"),
        "staged_runtime_jars": jar_manifest(staged_runtime) if staged_runtime else None,
        "dumper_sha256_reported_by_runner": summary.get("dumper_sha256"),
        "test_identities": test_identities(rows),
        "pending_test_names": sorted({
            row.get("test_name", "") for row in rows
            if row.get("status", "").lower() in {"skipped", "pending", "todo"}
        }),
        "validation_errors": errors,
    })
    return result


def main() -> int:
    args = parse_args()
    require_inputs(args)
    results_root = RESULTS_ROOT.resolve()
    if args.output_root is None:
        stamp = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
        output_root = results_root / f"runtime-comparison-bypass-{stamp}"
    else:
        output_root = args.output_root.resolve()
    if results_root not in output_root.parents:
        raise SystemExit(f"output root must be below ignored results directory: {results_root}")
    if output_root.exists() and any(output_root.iterdir()):
        raise SystemExit(f"refusing to reuse non-empty result directory: {output_root}")
    output_root.mkdir(parents=True, exist_ok=True)
    (output_root / "runner-logs").mkdir()
    comparison_path, _path_root = make_path_without_ccache(output_root)
    os.environ.clear()
    os.environ.update(base_environment(comparison_path))

    plan = {
        "output_root": str(output_root),
        "workload": {
            "vitest_filter": JS_TEST_FILTER,
            "vitest_config": str((SCRIPT_ROOT / "vitest.suite.config.ts").resolve()),
            "vitest_config_sha256": sha256_file(SCRIPT_ROOT / "vitest.suite.config.ts"),
            "expected_counts": EXPECTED_COUNTS,
            "environment_exclusions": sorted(ENVIRONMENT_EXCLUSIONS),
            "original_pending_count": 2,
        },
        "cache_state": {
            "mode": "bypass",
            "CCACHE_DISABLE": "true",
            "ccache_resolves_from_PATH": False,
            "path": comparison_path,
        },
        "timing_boundary": TIMING_BOUNDARY,
        "sequential": True,
        "source_revisions": {
            "clava_current": git_status(CLAVA_ROOT),
            "text_control": git_status(args.text_checkout / "clava"),
            "protobuf_control": git_status(args.protobuf_checkout / "clava"),
        },
        "runtime_roots": {
            "eager_flatbuffers": str(args.eager_runtime.resolve()),
            "text": str(args.text_runtime.resolve()),
            "protobuf": str(args.protobuf_runtime.resolve()),
        },
        "control_checkouts": {
            "text": str(args.text_checkout.resolve()),
            "protobuf": str(args.protobuf_checkout.resolve()),
        },
        "vitest_args": args.vitest_arg,
        "observations": ["eager-flatbuffers", "text", "protobuf"],
    }
    (output_root / "comparison-plan.json").write_text(json.dumps(plan, indent=2) + "\n")

    runners: tuple[Callable[[argparse.Namespace, Path, str], dict[str, Any]], ...] = (
        run_eager,
        run_text,
        run_protobuf,
    )
    observations: list[dict[str, Any]] = []
    reference_identities: list[tuple[str, str]] | None = None
    for runner in runners:
        result = runner(args, output_root, comparison_path)
        identities = result.get("test_identities")
        if isinstance(identities, list):
            normalized = [tuple(item) for item in identities]
            if reference_identities is None:
                reference_identities = normalized
            elif normalized != reference_identities:
                result.setdefault("validation_errors", []).append(
                    "test identities differ from the first observation"
                )
        observations.append(result)
        progress = {**plan, "completed": observations, "complete": False}
        (output_root / "comparison-progress.json").write_text(json.dumps(progress, indent=2) + "\n")
        print(json.dumps({
            "implementation": result["implementation"],
            "wall_s": result.get("wall_s"),
            "test_counts": result.get("test_counts"),
            "validation_errors": result.get("validation_errors"),
        }, sort_keys=True), flush=True)

    final = {**plan, "completed": observations, "complete": True}
    (output_root / "comparison-results.json").write_text(json.dumps(final, indent=2) + "\n")
    return 0 if all(not item.get("validation_errors") for item in observations) else 1


if __name__ == "__main__":
    raise SystemExit(main())

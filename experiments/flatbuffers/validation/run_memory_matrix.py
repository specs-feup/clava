#!/usr/bin/env python3
"""Compare isolated repeated-parse memory across prebuilt Clava runtimes."""

from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import re
import statistics
import subprocess
import sys
import time
from typing import Any

from artifact_metadata import installed_tool_metadata
import zipfile


ROOT = Path(__file__).resolve().parents[1]
CLAVA_ROOT = ROOT.parents[1]
sys.path.insert(0, str(ROOT))
from benchmark_environment import make_path_without_ccache
PROBE = Path(__file__).with_name("java") / "ValidationProbe.java"
RESULTS_ROOT = ROOT / "results" / "validation"
TIME_FORMAT = "elapsed_s=%e\\nuser_s=%U\\nsys_s=%S\\nmax_rss_kb=%M\\nexit_status=%x"
JVM_OPTION_ENV = ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS")
GC_FLAGS = ("-XX:+UseG1GC", "-XX:-DisableExplicitGC")


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def runtime_metadata(runtime: Path) -> dict[str, Any]:
    parser_jar = runtime / "lib" / "ClangAstParser.jar"
    if not parser_jar.is_file():
        raise SystemExit(f"missing ClangAstParser.jar in runtime: {runtime}")
    with zipfile.ZipFile(parser_jar) as archive:
        try:
            tag = archive.read("clang-dumper-release.tag").decode().strip()
        except KeyError:
            tag = None
    jars = {
        str(path.relative_to(runtime)): sha256_file(path)
        for path in sorted(runtime.rglob("*.jar")) if path.is_file()
    }
    canonical = json.dumps(jars, sort_keys=True, separators=(",", ":")).encode()
    source_revisions = None
    source_revisions_path = None
    for parent in (runtime, *runtime.parents):
        candidate = parent / "source-revisions.json"
        if candidate.is_file():
            source_revisions_path = candidate
            source_revisions = json.loads(candidate.read_text())
            break
    return {
        "runtime_root": str(runtime),
        "release_tag": tag,
        "parser_jar_sha256": sha256_file(parser_jar),
        "native_tool_sha256": next((sha256_file(Path(tag) / name)
            for name in ("tool", "tool.exe") if tag and Path(tag).is_absolute()
            and (Path(tag) / name).is_file()), None),
        "jar_count": len(jars),
        "jar_manifest_sha256": hashlib.sha256(canonical).hexdigest(),
        "source_revisions_file": str(source_revisions_path) if source_revisions_path else None,
        "source_revisions_sha256": sha256_file(source_revisions_path) if source_revisions_path else None,
        "source_revisions": source_revisions,
    }


def git_revision() -> str | None:
    result = subprocess.run(["git", "rev-parse", "HEAD"], cwd=CLAVA_ROOT,
                            capture_output=True, text=True, check=False)
    return result.stdout.strip() if result.returncode == 0 else None


def git_state() -> dict[str, Any]:
    status = subprocess.run(["git", "status", "--porcelain=v1", "--untracked-files=all"],
                            cwd=CLAVA_ROOT, capture_output=True, text=True, check=False)
    return {"revision": git_revision(), "dirty": status.returncode != 0 or bool(status.stdout.strip()),
            "porcelain": status.stdout.splitlines()}


def host_state() -> dict[str, Any]:
    state: dict[str, Any] = {}
    try:
        state["loadavg"] = Path("/proc/loadavg").read_text().strip()
    except OSError:
        pass
    try:
        state["meminfo_kib"] = {
            key: int(value.split()[0]) for line in Path("/proc/meminfo").read_text().splitlines()
            for key, _, value in [line.partition(":")]
            if key in {"MemTotal", "MemAvailable", "SwapTotal", "SwapFree"}
        }
    except OSError:
        pass
    ps = subprocess.run(["ps", "-eo", "pid,pcpu,rss,args", "--sort=-pcpu"],
                        capture_output=True, text=True, check=False)
    state["top_processes"] = ps.stdout.splitlines()[:13]
    return state


def parse_time(path: Path) -> dict[str, float | int]:
    values: dict[str, float | int] = {}
    for line in path.read_text().splitlines():
        key, separator, raw = line.partition("=")
        if separator:
            values[key] = int(raw) if key in {"max_rss_kb", "exit_status"} else float(raw)
    return values


def java_environment_audit() -> dict[str, Any]:
    """Record ambient JVM options without copying potentially sensitive values."""
    return {
        name: {
            "present": name in os.environ,
            "value_sha256": hashlib.sha256(os.environ[name].encode()).hexdigest()
            if name in os.environ else None,
        }
        for name in JVM_OPTION_ENV
    }


def parse_heap(output: str) -> list[dict[str, Any]]:
    rows = []
    for line in output.splitlines():
        match = re.match(r"CLAVA_HEAP (\{.*\})$", line)
        if match:
            rows.append(json.loads(match.group(1)))
    return rows


def parse_runtime(raw: str) -> tuple[str, Path]:
    label, separator, path = raw.partition("=")
    if not separator or not label or not path:
        raise argparse.ArgumentTypeError("runtime must be LABEL=/path/to/ClavaWeaver")
    return label, Path(path).resolve()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--runtime", action="append", type=parse_runtime, required=True,
                        help="Prebuilt distribution; repeat for eager, text and protobuf controls.")
    parser.add_argument("--source", type=Path, required=True,
                        help="One representative C or C++ source parsed repeatedly by each JVM.")
    parser.add_argument("--standard", default=None)
    parser.add_argument("--output-root", type=Path)
    parser.add_argument("--repeat-count", type=int, default=3,
                        help="Independent JVM observations per runtime (default 3).")
    parser.add_argument("--parse-repeats", type=int, default=20,
                        help="Repeated parse/GC cycles inside each measured JVM (default 20).")
    parser.add_argument("--strict-cleanup-label", default="eager",
                        help="Only this runtime label must have zero maps and temp files (default eager).")
    parser.add_argument("--heap", default="4g", help="Java maximum heap, default 4g.")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    if len(args.runtime) < 2:
        raise SystemExit("at least two isolated runtimes are needed for a comparison")
    if len({label for label, _ in args.runtime}) != len(args.runtime):
        raise SystemExit("runtime labels must be unique")
    if args.repeat_count < 1 or args.parse_repeats < 2:
        raise SystemExit("repeat counts must be positive, with at least two parse repeats")
    source = args.source.resolve()
    if not source.is_file():
        raise SystemExit(f"source file does not exist: {source}")
    output = args.output_root.resolve() if args.output_root else RESULTS_ROOT / dt.datetime.now(
        dt.timezone.utc
    ).strftime("memory-%Y%m%dT%H%M%SZ")
    output.mkdir(parents=True, exist_ok=False)
    benchmark_path, _ = make_path_without_ccache(output, ("java", "javac"))
    java_options_audit = java_environment_audit()
    ambient_java_options = " ".join(os.environ.get(name, "") for name in JVM_OPTION_ENV)
    if re.search(r"(?<!\S)-Dclava\.astWire=\S+", ambient_java_options):
        raise SystemExit("remove obsolete AST wire-selection overrides before memory runs")
    java_version_env = os.environ.copy()
    for name in JVM_OPTION_ENV:
        java_version_env.pop(name, None)
    java_binary = shutil.which("java", path=benchmark_path)
    if java_binary is None:
        raise SystemExit("java is missing from the benchmark PATH")
    java_version = subprocess.run([java_binary, *GC_FLAGS, "-version"], env=java_version_env,
                                  capture_output=True, text=True, check=True)
    java_version_text = (java_version.stdout + java_version.stderr).strip()
    temp_root = output / "tmp"
    temp_root.mkdir()

    compiled: dict[str, tuple[Path, str, dict[str, Any]]] = {}
    for label, runtime in args.runtime:
        if not runtime.is_dir():
            raise SystemExit(f"runtime directory does not exist: {runtime}")
        metadata = runtime_metadata(runtime)
        classes = output / "classes" / label
        classes.mkdir(parents=True)
        lib = runtime / "lib"
        classpath = f"{classes}:{lib}/*"
        command = ["javac", "-cp", f"{lib}/*", "-d", str(classes), str(PROBE)]
        subprocess.run(command, cwd=CLAVA_ROOT, check=True)
        metadata["probe_compile_command"] = command
        compiled[label] = (runtime, classpath, metadata)

    labels = [label for label, _ in args.runtime]
    if args.strict_cleanup_label not in labels:
        raise SystemExit(f"strict cleanup label {args.strict_cleanup_label!r} is not in the runtime matrix")
    runs: list[dict[str, Any]] = []
    for repeat in range(1, args.repeat_count + 1):
        order = labels[repeat - 1:] + labels[:repeat - 1]
        for label in order:
            runtime, classpath, metadata = compiled[label]
            run_dir = output / f"repeat-{repeat}-{label}"
            run_dir.mkdir()
            work = run_dir / "work"
            work.mkdir()
            run_tmp = run_dir / "tmp"
            run_tmp.mkdir()
            run_cache = run_dir / "xdg-cache"
            run_cache.mkdir()
            standard = args.standard or ("c11" if source.suffix.lower() == ".c" else "c++17")
            time_path = run_dir / "time.txt"
            java = [
                "/usr/bin/time", "-f", TIME_FORMAT, "-o", str(time_path), "--",
                "java", *GC_FLAGS, "-Xms128m", f"-Xmx{args.heap}",
                f"-Djava.io.tmpdir={run_tmp}",
                "-cp", classpath, "ValidationProbe", "memory", str(source), str(work),
                standard, str(args.parse_repeats), str(label == args.strict_cleanup_label).lower(),
            ]
            env = os.environ.copy()
            for name in JVM_OPTION_ENV:
                env.pop(name, None)
            if any(
                key in env for key in ("AST_WIRE_FLAT", "AST_WIRE_DENSE_TEXT")
            ):
                raise SystemExit("remove obsolete AST wire-selection overrides before memory runs")
            env["CCACHE_DISABLE"] = "true"
            env["PATH"] = benchmark_path
            env["XDG_CACHE_HOME"] = str(run_cache)
            env.update({"TMPDIR": str(run_tmp), "TMP": str(run_tmp), "TEMP": str(run_tmp)})
            env["JAVA_TOOL_OPTIONS"] = f"-Djava.io.tmpdir={run_tmp}"
            before = host_state()
            started = time.perf_counter()
            completed = subprocess.run(java, cwd=CLAVA_ROOT, env=env, capture_output=True,
                                       text=True, check=False)
            wall_s = time.perf_counter() - started
            (run_dir / "probe.log").write_text(completed.stdout + completed.stderr)
            rows = parse_heap(completed.stdout + completed.stderr)
            time_metrics = parse_time(time_path) if time_path.is_file() else {}
            run_metadata = dict(metadata)
            if run_metadata.get("native_tool_sha256") is None:
                installed = installed_tool_metadata(work)
                if installed:
                    run_metadata.update(installed)
            result = {
                "label": label,
                "repeat": repeat,
                "order_in_repeat": order.index(label) + 1,
                "runtime": run_metadata,
                "host_before": before,
                "host_after": host_state(),
                "source": str(source),
                "source_sha256": sha256_file(source),
                "standard": standard,
                "parse_repeats": args.parse_repeats,
                "cache_policy": "CCACHE_DISABLE=true; ccache absent from PATH; AST_DUMP_CACHE=false",
                "gc_policy": {"collector": "G1", "explicit_gc": "enabled", "flags": list(GC_FLAGS)},
                "tmp_root": str(run_tmp),
                "wall_s": wall_s,
                "gnu_time": time_metrics,
                "heap_rows": rows,
                "strict_cleanup_required": label == args.strict_cleanup_label,
                "return_code": completed.returncode,
                "command": java,
                "passed": completed.returncode == 0 and len(rows) == args.parse_repeats
                    and (label != args.strict_cleanup_label or all(
                        row.get("app_collected") and row.get("mapped_paths_under_work") == 0
                        and row.get("leftover_clang_temp_folders") == 0
                        and row.get("open_parser_files") == 0 for row in rows
                    )),
            }
            (run_dir / "summary.json").write_text(json.dumps(result, indent=2) + "\n")
            runs.append(result)
            print(json.dumps({"label": label, "repeat": repeat, "passed": result["passed"],
                              "max_rss_kb": time_metrics.get("max_rss_kb"),
                              "retained_heap_bytes": [row.get("retained_heap_bytes") for row in rows]},
                             sort_keys=True), flush=True)

    aggregates: dict[str, Any] = {}
    for label in labels:
        selected = [run for run in runs if run["label"] == label]
        peaks = [max(int(row["jvm_peak_rss_bytes"]) for row in run["heap_rows"]) // 1024
                 for run in selected if run["heap_rows"] and all(
                     int(row.get("jvm_peak_rss_bytes", -1)) >= 0 for row in run["heap_rows"])]
        retained = [
            int(row["retained_heap_bytes"])
            for run in selected for row in run["heap_rows"]
            if "retained_heap_bytes" in row
        ]
        first_retained = [
            int(run["heap_rows"][0]["retained_heap_bytes"])
            for run in selected if run["heap_rows"]
        ]
        last_retained = [
            int(run["heap_rows"][-1]["retained_heap_bytes"])
            for run in selected if run["heap_rows"]
        ]
        aggregates[label] = {
            "runs": len(selected),
            "all_passed": len(selected) == args.repeat_count and all(run["passed"] for run in selected),
            "peak_rss_kib_median": statistics.median(peaks) if peaks else None,
            "peak_rss_kib_range": [min(peaks), max(peaks)] if peaks else None,
            "retained_heap_bytes_median": statistics.median(retained) if retained else None,
            "retained_heap_bytes_range": [min(retained), max(retained)] if retained else None,
            "retained_heap_growth_bytes_median": statistics.median(
                last - first for first, last in zip(first_retained, last_retained)
            ) if first_retained else None,
        }
    summary = {
        "created_utc": dt.datetime.now(dt.timezone.utc).isoformat(),
        "clava_git": git_state(),
        "source": str(source),
        "source_sha256": sha256_file(source),
        "repeat_count": args.repeat_count,
        "parse_repeats": args.parse_repeats,
        "strict_cleanup_label": args.strict_cleanup_label,
        "java_version": java_version_text,
        "java_option_environment_audit": java_options_audit,
        "measured_java_option_environment": {
            "JAVA_TOOL_OPTIONS": "-Djava.io.tmpdir=<run-specific temp directory>",
            "JDK_JAVA_OPTIONS": None,
            "_JAVA_OPTIONS": None,
        },
        "gc_policy": {"collector": "G1", "explicit_gc": "enabled", "flags": list(GC_FLAGS)},
        "timing_boundary": "GNU time covers launch, native parses, AST construction and explicit GCs; its RSS includes child maxima. peak_rss aggregates use Linux /proc/self/status VmHWM from the JVM only, sampled after each parse/collect cycle.",
        "run_order": "sequential; runtime order rotates by repeat",
        "host_activity_recorded_before_and_after_each_process": True,
        "runs": runs,
        "aggregates": aggregates,
        "passed": all(item["all_passed"] for item in aggregates.values()),
    }
    (output / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
    print(json.dumps({"output": str(output), "passed": summary["passed"],
                      "aggregates": aggregates}, indent=2))
    return 0 if summary["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""Run the gated grouped cache-warmup and JVM-context follow-up cells.

Default mode is a read-only plan. Running requires both --execute and the
manual --release-host switch. No classes or native artifacts are built here.
"""

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
import time
from typing import Any

sys.dont_write_bytecode = True

SCRIPT_DIR = Path(__file__).resolve().parent
CLAVA_ROOT = SCRIPT_DIR.parents[2]
CORPUS_ROOT = CLAVA_ROOT / "experiments/protocol-comparison/results/grouped-live-corpus-20260930-r2"
OVERLAY_ROOT = CORPUS_ROOT / "standalone-overlay/compat-r1"
CLASSES = CORPUS_ROOT / "standalone-overlay/classes"
RUNTIME = CORPUS_ROOT / "runtime-shared-19c8"
NATIVE_TOOL = CLAVA_ROOT.parent / "clang-dumper/build/tool"
SCHEDULE_ROOT = CORPUS_ROOT / "standalone-overlay/warmup-intervention-r1/schedules"
CCACHE_FOLDER = "clang-dumper-protobuf-ccache-v1"
RUNNER_CLASS = "StandaloneParseRunner"
FROZEN = {
    "native_tool_sha256": "3f3a1844b1593e820ee525f02e361600fa64637d81df9f56dee3b8825c7119e2",
    "runtime_jar_manifest_sha256": "365906d296ce6ef7ebecab98f29accbfd956ea214422633f6cce288c2e8493b9",
    "class_manifest_sha256": "167db606b53109d27902b081bd5415cd590fac5715a6d0110448818738efded8",
    "runner_class_sha256": "1527b7827d16cd8536715cf2e4c9d1cf4e062cd50b762a9d33b65286d394b7c5",
    "reader_class_sha256": "330e8b855e1381999f46c8aa05f5301c4f78269838e60de50d31d26b83b939a8",
    "compat_proto_types_sha256": "825dbb54cd7270bde5e79abc232d6a3e45e4285d1d191485182d7364e9084230",
}
JAVA_OPTION_ENV = ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "JAVA_OPTS", "GRADLE_OPTS")


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def read_jsonl(path: Path) -> list[dict[str, Any]]:
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]


def write_jsonl(path: Path, rows: list[dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("".join(json.dumps(row, separators=(",", ":")) + "\n" for row in rows), encoding="utf-8")


def write_json(path: Path, value: dict[str, Any]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def class_manifest_hash(root: Path) -> str:
    lines = []
    for path in sorted(root.rglob("*.class")):
        relative = path.relative_to(root).as_posix()
        lines.append(f"{sha256_file(path)}  ./{relative}\n")
    return hashlib.sha256("".join(lines).encode()).hexdigest()


def jar_manifest(root: Path) -> dict[str, Any]:
    jars = {path.relative_to(root).as_posix(): sha256_file(path)
            for path in sorted(root.rglob("*.jar")) if path.is_file()}
    canonical = json.dumps(jars, sort_keys=True, separators=(",", ":")).encode()
    return {"jar_count": len(jars), "sha256": hashlib.sha256(canonical).hexdigest()}


def directory_manifest_hash(root: Path) -> str:
    entries = []
    for path in sorted(root.rglob("*")):
        if path.is_file():
            entries.append(f"{sha256_file(path)}  ./{path.relative_to(root).as_posix()}\n")
    return hashlib.sha256("".join(entries).encode()).hexdigest()


def cache_payload_manifest_hash(root: Path) -> str:
    """Hash ccache payloads while excluding its intentionally mutable stats files."""
    entries = []
    for path in sorted(root.rglob("*")):
        if path.is_file() and path.name != "stats":
            entries.append(f"{sha256_file(path)}  ./{path.relative_to(root).as_posix()}\n")
    return hashlib.sha256("".join(entries).encode()).hexdigest()


def verify_frozen_artifacts() -> dict[str, Any]:
    observed = {
        "native_tool_sha256": sha256_file(NATIVE_TOOL),
        "runtime_jar_manifest_sha256": jar_manifest(RUNTIME)["sha256"],
        "class_manifest_sha256": class_manifest_hash(CLASSES),
        "runner_class_sha256": sha256_file(CLASSES / "StandaloneParseRunner.class"),
        "reader_class_sha256": sha256_file(CLASSES / "pt/up/fe/specs/clang/wire/ProtoNodeDataReader.class"),
        "compat_proto_types_sha256": sha256_file(CLAVA_ROOT.parent / "clang-dumper/src/Clava/ProtoTypes.cpp"),
    }
    mismatches = {key: {"expected": FROZEN[key], "observed": value}
                  for key, value in observed.items() if FROZEN[key] != value}
    if mismatches:
        raise RuntimeError(f"frozen artifacts changed: {mismatches}")
    return observed | {"runtime_jar_count": jar_manifest(RUNTIME)["jar_count"]}


def _schedule(path: Path, expected_suite: str, expected_protocol: str,
              expected_phase: str, expected_groups: int, expected_warmups: int = 0) -> dict[str, Any]:
    rows = read_jsonl(path)
    parse_rows = [row for row in rows if row.get("operation") is None]
    measured = [row for row in parse_rows if row.get("phase") == expected_phase]
    warmups = [row for row in parse_rows if row.get("phase") == "warmup"]
    if len(measured) != expected_groups or len(warmups) != expected_warmups:
        raise RuntimeError(f"unexpected row counts in {path.name}: measure={len(measured)}, warmup={len(warmups)}")
    if any(row.get("suite") != expected_suite for row in measured):
        raise RuntimeError(f"unexpected measured suite in {path.name}")
    if any(row.get("protocol") != expected_protocol or row.get("cache_mode") != "warm"
           or row.get("compression_policy") != "raw_control"
           or row.get("expected_compressed") is not False or row.get("disable_compression") is not True
           or row.get("parser_config", {}).get("show_exec_info") is not False for row in parse_rows):
        raise RuntimeError(f"schedule is not matched warm/raw/no-SHOW_EXEC_INFO: {path.name}")
    eligible = sum(1 for row in parse_rows for native in row.get("expected_native_calls", [])
                   if native.get("cache_enabled") is True)
    return {"path": path, "sha256": sha256_file(path), "rows": rows,
            "measure_rows": measured, "warmup_rows": warmups, "eligible_calls": eligible}


def warmup_cells() -> list[dict[str, Any]]:
    pairs = [
        ("clava-js", "text", ["control", "prefix"]),
        ("java", "protobuf", ["prefix", "control"]),
        ("java", "text", ["control", "prefix"]),
        ("clava-js", "protobuf", ["prefix", "control"]),
    ]
    order = [(suite, protocol, condition, 1) for suite, protocol, treatments in pairs for condition in treatments]
    order += [(suite, protocol, condition, 2) for suite, protocol, treatments in reversed(pairs)
              for condition in reversed(treatments)]
    cells = []
    for suite, protocol, treatment, repeat in order:
        if treatment == "prefix":
            filename = f"prefix-java-nas-{suite}-{protocol}-warm-r{repeat:02d}.jsonl"
            warmup_count = 8
        else:
            filename = f"measure-{suite}-{protocol}-warm-r{repeat:02d}.jsonl"
            warmup_count = 0
        groups = 300 if suite == "clava-js" else 216
        schedule = _schedule(SCHEDULE_ROOT / filename, suite, protocol, "measure", groups, warmup_count)
        cells.append({"name": f"{treatment}-{suite}-{protocol}-warm-r{repeat:02d}",
                      "suite": suite, "protocol": protocol, "treatment": treatment,
                      "repeat": repeat, "mode": "measure", **schedule})
    return cells


def js_jfr_cells() -> list[dict[str, Any]]:
    order = [("text", "control"), ("text", "prefix"), ("protobuf", "prefix"), ("protobuf", "control")]
    cells = []
    for protocol, treatment in order:
        filename = (f"diagnostic-clava-js-{protocol}-warm-control-r01.jsonl" if treatment == "control"
                    else f"diagnostic-prefix-js-{protocol}-warm-r01.jsonl")
        warmup_count = 8 if treatment == "prefix" else 0
        schedule = _schedule(SCHEDULE_ROOT / filename, "clava-js", protocol, "diagnostic", 300, warmup_count)
        cells.append({"name": f"jfr-{treatment}-clava-js-{protocol}-warm-r01", "suite": "clava-js",
                      "protocol": protocol, "treatment": treatment, "repeat": 1, "mode": "jfr", **schedule})
    return cells


def java_heap_cells() -> list[dict[str, Any]]:
    pairs = [("text", [None, "512m"]), ("protobuf", ["512m", None])]
    order = [(protocol, heap, 1) for protocol, heaps in pairs for heap in heaps]
    order += [(protocol, heap, 2) for protocol, heaps in reversed(pairs) for heap in reversed(heaps)]
    cells = []
    for protocol, heap, repeat in order:
        schedule = _schedule(SCHEDULE_ROOT / f"measure-java-{protocol}-warm-r{repeat:02d}.jsonl",
                             "java", protocol, "measure", 216)
        tag = "default" if heap is None else "xmx512m"
        cells.append({"name": f"java-{protocol}-{tag}-warm-r{repeat:02d}", "suite": "java",
                      "protocol": protocol, "treatment": tag, "repeat": repeat, "mode": "heap",
                      "heap": heap, **schedule})
    return cells


def java_heap_extension_cells() -> list[dict[str, Any]]:
    """Counterbalanced no-prefix Java heap-control repeats 3 through 6."""
    orders = {
        3: [("text", None), ("text", "512m"), ("protobuf", "512m"), ("protobuf", None)],
        4: [("protobuf", None), ("protobuf", "512m"), ("text", "512m"), ("text", None)],
        5: [("text", "512m"), ("text", None), ("protobuf", None), ("protobuf", "512m")],
        6: [("protobuf", "512m"), ("protobuf", None), ("text", None), ("text", "512m")],
    }
    cells = []
    for repeat, order in orders.items():
        for protocol, heap in order:
            schedule = _schedule(SCHEDULE_ROOT / f"measure-java-{protocol}-warm-r{repeat:02d}.jsonl",
                                 "java", protocol, "measure", 216)
            tag = "default" if heap is None else "xmx512m"
            cells.append({"name": f"java-{protocol}-{tag}-warm-r{repeat:02d}", "suite": "java",
                          "protocol": protocol, "treatment": tag, "repeat": repeat, "mode": "measure",
                          "heap": heap, **schedule})
    return cells


def java_heap_jfr_cells() -> list[dict[str, Any]]:
    cells = []
    for protocol in ("text", "protobuf"):
        source = SCHEDULE_ROOT / f"measure-java-{protocol}-warm-r01.jsonl"
        scheduled = _schedule(source, "java", protocol, "measure", 216)
        diagnostic_rows = [{**row, "phase": "diagnostic"} for row in scheduled["rows"]]
        cells.append({"name": f"jfr-xmx512m-java-{protocol}-warm-r01", "suite": "java",
                      "protocol": protocol, "treatment": "xmx512m", "repeat": 1, "mode": "jfr",
                      "heap": "512m", "path": source, "sha256": scheduled["sha256"],
                      "rows": diagnostic_rows, "measure_rows": diagnostic_rows, "warmup_rows": [],
                      "eligible_calls": scheduled["eligible_calls"], "derived_phase": "diagnostic"})
    return cells


def make_plan(mode: str) -> list[dict[str, Any]]:
    if mode == "warmup":
        return warmup_cells()
    if mode == "js-jfr":
        return js_jfr_cells()
    if mode == "java-heap":
        return java_heap_cells()
    if mode == "java-heap-extension":
        return java_heap_extension_cells()
    if mode == "java-heap-jfr":
        return java_heap_jfr_cells()
    return warmup_cells() + js_jfr_cells()


def cache_operation(operation: str, cache_dir: Path, suite: str, protocol: str, repeat: int) -> dict[str, Any]:
    return {"record_type": "ccache", "operation": operation, "suite": suite, "protocol": protocol,
            "cache_mode": "warm", "repeat": repeat, "input_id": "__ccache__",
            "source_label": "ccache stats", "cache_directory": str(cache_dir)}


def ccache_command(op: dict[str, Any], env: dict[str, str]) -> dict[str, Any]:
    flag = "--zero-stats" if op["operation"] == "ccache_zero" else "--show-stats"
    command = ["ccache", "-d", op["cache_directory"], flag]
    result = subprocess.run(command, env=env, capture_output=True, text=True, check=False)
    return {**op, "command": command, "command_status": result.returncode,
            "output": result.stdout + result.stderr, "valid": result.returncode == 0}


def parse_ccache_stats(output: str) -> dict[str, int]:
    def first(pattern: str) -> int:
        match = re.search(pattern, output, re.MULTILINE)
        if not match:
            raise RuntimeError(f"ccache stats missing pattern: {pattern}")
        return int(match.group(1).replace(",", ""))
    return {
        "cacheable_calls": first(r"^Cacheable calls:\s*([\d,]+)"),
        "hits": first(r"^\s*Hits:\s*([\d,]+)"),
        "misses": first(r"^\s*Misses:\s*([\d,]+)"),
    }


def command_env(scratch: Path, native_tool: Path) -> tuple[dict[str, str], dict[str, Any]]:
    env = os.environ.copy()
    removed = {}
    for name in (*JAVA_OPTION_ENV, "CLAVA_AST_CORPUS_CAPTURE_DIR", "CLAVA_AST_CORPUS_SUITE", "CCACHE_DISABLE"):
        removed[name] = name in env
        env.pop(name, None)
    env.update({"CLANG_DUMPER_TOOL": str(native_tool), "TMPDIR": str(scratch), "TMP": str(scratch),
                "TEMP": str(scratch), "XDG_CACHE_HOME": str(scratch)})
    return env, {"removed_inherited_option_vars": removed, "ccache_disable": "unset",
                 "show_exec_info": False, "forced_gc_policy_flags": [],
                 "automatic_gc_allowed": True, "runner_has_no_explicit_gc_call": True,
                 "java_agent": "none"}


def java_argv(classes: Path, runtime: Path, schedule: Path, output: Path,
              heap: str | None, jfr: Path | None, scratch: Path) -> list[str]:
    java = shutil.which("java")
    if not java:
        raise RuntimeError("java not found on PATH")
    classpath = os.pathsep.join((str(classes), str(runtime / "lib" / "*")))
    command = [java]
    if heap:
        command.append(f"-Xmx{heap}")
    command.append(f"-Djava.io.tmpdir={scratch}")
    if jfr:
        settings = jfr.parent / "profile.jfc"
        command.append(f"-XX:StartFlightRecording=filename={jfr},settings={settings},dumponexit=true")
    command += ["-cp", classpath, RUNNER_CLASS, "--schedule", str(schedule), "--output", str(output)]
    return command


def prepare_jfr_settings(profile_dir: Path) -> Path:
    profile_dir.mkdir(parents=True, exist_ok=True)
    settings = profile_dir / "profile.jfc"
    command = ["jfr", "configure", "class-loading=true", "allocation-profiling=medium",
               "compiler=detailed", "method-profiling=high", "gc=normal", "--output", str(settings)]
    result = subprocess.run(command, capture_output=True, text=True, check=False)
    if result.returncode != 0:
        raise RuntimeError(f"jfr configure failed: {result.stdout}{result.stderr}")
    return settings


def validate_observations(cell: dict[str, Any], rows: list[dict[str, Any]]) -> None:
    parse_rows = [row for row in rows if row.get("record_type") != "ccache"]
    scheduled = cell["rows"]
    if len(parse_rows) != len(scheduled):
        raise RuntimeError(f"{cell['name']}: output rows {len(parse_rows)} != schedule rows {len(scheduled)}")
    for expected, actual in zip(scheduled, parse_rows):
        for key in ("phase", "suite", "input_id", "source_label", "event_id", "protocol", "cache_mode",
                    "repeat", "source_sha256", "args_sha256", "options_sha256"):
            if key in expected and actual.get(key) != expected[key]:
                raise RuntimeError(f"{cell['name']}: {key} mismatch for {expected.get('input_id')}")
        if actual.get("valid") is not True:
            raise RuntimeError(f"{cell['name']}: invalid parse row {actual.get('input_id')}: {actual.get('error')}")


def launch_cell(cell: dict[str, Any], output_root: Path, classes: Path, runtime: Path,
                native_tool: Path, heap: str | None = None, jfr_path: Path | None = None) -> dict[str, Any]:
    name = cell["name"]
    source_schedule = cell["path"]
    source_schedule_sha = sha256_file(source_schedule)
    if source_schedule_sha != cell["sha256"]:
        raise RuntimeError(f"prepared schedule changed: {source_schedule}")
    raw_root = output_root / "runner-output"
    obs_root = output_root / "observations"
    proof_root = output_root / "warm-counters"
    manifest_root = output_root / "run-manifests"
    log_root = output_root / "logs"
    scratch = output_root / "scratch" / name
    for folder in (raw_root, obs_root, proof_root, manifest_root, log_root, scratch):
        folder.mkdir(parents=True, exist_ok=True)
    # Reuse the exact compat-r1 dumper/cache path: ccache's compiler key includes
    # path-sensitive inputs, so relocating a primed cache can create false misses.
    dumper_folder = (OVERLAY_ROOT / "schedules/parser-state" / cell["suite"] / cell["protocol"]
                     / "warm/dumper")
    cache_dir = dumper_folder / CCACHE_FOLDER
    if not cache_dir.is_dir():
        raise RuntimeError(f"missing exact compat-r1 warm cache: {cache_dir}")
    payload_hash_before = cache_payload_manifest_hash(cache_dir)
    private_rows = []
    for row in cell["rows"]:
        private = dict(row)
        private["dumper_folder"] = str(dumper_folder)
        private["parser_config"] = dict(row.get("parser_config", {}))
        private["parser_config"]["dumper_folder"] = str(dumper_folder)
        private_rows.append(private)
    schedule = output_root / "run-schedules" / f"{name}.jsonl"
    write_jsonl(schedule, private_rows)
    output = raw_root / f"{name}.jsonl"
    log = log_root / f"{name}.log"
    observation = obs_root / f"{name}.jsonl"
    proof_path = proof_root / f"counterproof-{name}.jsonl"
    manifest_path = manifest_root / f"run-{name}.json"
    env, env_policy = command_env(scratch, native_tool)
    if shutil.which("ccache", path=env.get("PATH")) is None:
        raise RuntimeError("ccache is required for warm-cache proof")
    counter_rows = []
    zero = cache_operation("ccache_zero", cache_dir, cell["suite"], cell["protocol"], cell["repeat"])
    zero_result = ccache_command(zero, env)
    counter_rows.append(zero_result)
    if zero_result["command_status"] != 0:
        raise RuntimeError(f"{name}: ccache --zero-stats failed")
    payload_hash_after_zero = cache_payload_manifest_hash(cache_dir)
    if payload_hash_after_zero != payload_hash_before:
        raise RuntimeError(f"{name}: cache payload changed during --zero-stats")

    command = java_argv(classes, runtime, schedule, output, heap, jfr_path, scratch)
    started = dt.datetime.now(dt.timezone.utc).isoformat()
    start = time.monotonic()
    with log.open("w", encoding="utf-8") as stream:
        completed = subprocess.run(command, cwd=CLAVA_ROOT, env=env, stdout=stream,
                                   stderr=subprocess.STDOUT, check=False)
    wall_seconds = time.monotonic() - start
    finished = dt.datetime.now(dt.timezone.utc).isoformat()

    stats = cache_operation("ccache_stats", cache_dir, cell["suite"], cell["protocol"], cell["repeat"])
    stats_result = ccache_command(stats, env)
    counter_rows.append(stats_result)
    payload_hash_after_run = cache_payload_manifest_hash(cache_dir)
    if payload_hash_after_run != payload_hash_before:
        raise RuntimeError(f"{name}: cache payload changed during supposedly all-hit cell")
    write_jsonl(proof_path, counter_rows)
    all_rows = read_jsonl(output) if output.is_file() else []
    write_jsonl(output, all_rows)
    observations = [row for row in all_rows if row.get("record_type") != "ccache"]
    write_jsonl(observation, observations)
    if completed.returncode != 0:
        raise RuntimeError(f"{name}: Java runner exited {completed.returncode}; see {log}")
    validate_cell = dict(cell)
    validate_cell["rows"] = private_rows
    validate_observations(validate_cell, observations)
    if stats_result["command_status"] != 0:
        raise RuntimeError(f"{name}: ccache --show-stats failed")
    parsed = parse_ccache_stats(stats_result["output"])
    if parsed["cacheable_calls"] != cell["eligible_calls"] or parsed["hits"] != cell["eligible_calls"] or parsed["misses"] != 0:
        raise RuntimeError(f"{name}: warm ccache proof mismatch expected {cell['eligible_calls']}, got {parsed}")
    manifest = {
        "schema_version": 1, "name": name, "suite": cell["suite"], "protocol": cell["protocol"],
        "treatment": cell["treatment"], "repeat": cell["repeat"], "phase": cell["mode"],
        "heap_limit": heap, "profiled": jfr_path is not None, "schedule_path": str(source_schedule),
        "schedule_sha256": source_schedule_sha, "executed_schedule_path": str(schedule),
        "executed_schedule_sha256": sha256_file(schedule), "observation_path": str(observation),
        "observation_sha256": sha256_file(observation), "counterproof_path": str(proof_path),
        "counterproof_sha256": sha256_file(proof_path), "runner_output_path": str(output),
        "runner_output_sha256": sha256_file(output), "log_path": str(log), "java_argv": command,
        "cwd": str(CLAVA_ROOT), "env_policy": env_policy, "explicit_gc_policy_flags": [],
        "gc_policy": "no explicit/forced GC options or calls in runner; JVM automatic GC allowed",
        "runner_has_no_explicit_gc_call": True, "show_exec_info": False,
        "java_agent": "none", "native_tool_sha256": FROZEN["native_tool_sha256"],
        "overlay_class_manifest_sha256": FROZEN["class_manifest_sha256"],
        "runner_class_sha256": FROZEN["runner_class_sha256"],
        "reader_class_sha256": FROZEN["reader_class_sha256"],
        "runtime_jar_manifest_sha256": FROZEN["runtime_jar_manifest_sha256"],
        "compat_proto_types_sha256": FROZEN["compat_proto_types_sha256"],
        "cache_seed_source": str(dumper_folder), "cache_seed_payload_sha256": payload_hash_before,
        "cache_payload_sha256_after_zero": payload_hash_after_zero,
        "cache_payload_sha256_after_run": payload_hash_after_run,
        "counter_reset_recorded": True,
        "cache_directory": str(cache_dir),
        "started_utc": started, "finished_utc": finished, "process_wall_seconds": wall_seconds,
        "exit_code": completed.returncode, "ccache_counters": parsed,
        "expected_eligible_hits": cell["eligible_calls"], "valid": True,
    }
    if jfr_path:
        manifest["jfr_path"] = str(jfr_path)
    write_json(manifest_path, manifest)
    if jfr_path:
        summary_path = jfr_path.with_suffix(".summary.txt")
        summary = subprocess.run(["jfr", "summary", str(jfr_path)], capture_output=True, text=True, check=False)
        summary_path.write_text(summary.stdout + summary.stderr, encoding="utf-8")
        events_path = jfr_path.with_suffix(".events.json")
        events = subprocess.run(["jfr", "print", "--json", "--events",
                                 "jdk.ExecutionSample,jdk.ObjectAllocationSample,jdk.GCPhasePause,jdk.ClassLoad,jdk.Compilation",
                                 "--stack-depth", "32", str(jfr_path)], capture_output=True, text=True, check=False)
        events_path.write_text(events.stdout + events.stderr, encoding="utf-8")
        if summary.returncode != 0 or events.returncode != 0:
            raise RuntimeError(f"{name}: JFR extraction failed")
    return manifest


def seed_prefix_cache(cell: dict[str, Any], output_root: Path, classes: Path, runtime: Path,
                      native_tool: Path) -> dict[str, Any]:
    """Untimedly populate cache keys for the eight prefix-only warmup groups."""
    name = f"seed-{cell['suite']}-{cell['protocol']}-java-nas-prefix"
    warmup_rows = [row for row in cell["rows"] if row.get("phase") == "warmup"]
    if len(warmup_rows) != 8:
        raise RuntimeError(f"{name}: expected exactly eight prefix-only groups")
    source_schedule_sha = cell["sha256"]
    dumper_folder = (OVERLAY_ROOT / "schedules/parser-state" / cell["suite"] / cell["protocol"]
                     / "warm/dumper")
    cache_dir = dumper_folder / CCACHE_FOLDER
    if not cache_dir.is_dir():
        raise RuntimeError(f"missing exact compat-r1 warm cache: {cache_dir}")
    private_rows = []
    for row in warmup_rows:
        private = dict(row)
        private["dumper_folder"] = str(dumper_folder)
        private["parser_config"] = dict(row.get("parser_config", {}))
        private["parser_config"]["dumper_folder"] = str(dumper_folder)
        private_rows.append(private)
    seed_schedule = output_root / "seed-schedules" / f"{name}.jsonl"
    write_jsonl(seed_schedule, private_rows)
    output = output_root / "seed-output" / f"{name}.jsonl"
    output.parent.mkdir(parents=True, exist_ok=True)
    log = output_root / "seed-logs" / f"{name}.log"
    log.parent.mkdir(parents=True, exist_ok=True)
    proof_path = output_root / "warm-counters" / f"{name}.jsonl"
    manifest_path = output_root / "seed-manifests" / f"{name}.json"
    scratch = output_root / "scratch" / name
    scratch.mkdir(parents=True, exist_ok=True)
    env, env_policy = command_env(scratch, native_tool)
    before_payload_sha = cache_payload_manifest_hash(cache_dir)
    before_stats = ccache_command(cache_operation("ccache_stats_before_seed", cache_dir,
                                                   cell["suite"], cell["protocol"], cell["repeat"]), env)
    zero = ccache_command(cache_operation("ccache_zero", cache_dir, cell["suite"],
                                          cell["protocol"], cell["repeat"]), env)
    if before_stats["command_status"] != 0 or zero["command_status"] != 0:
        raise RuntimeError(f"{name}: ccache stats/reset command failed")
    after_zero_sha = cache_payload_manifest_hash(cache_dir)
    if after_zero_sha != before_payload_sha:
        raise RuntimeError(f"{name}: cache payload changed during --zero-stats")
    command = java_argv(classes, runtime, seed_schedule, output, None, None, scratch)
    started = dt.datetime.now(dt.timezone.utc).isoformat()
    start = time.monotonic()
    with log.open("w", encoding="utf-8") as stream:
        completed = subprocess.run(command, cwd=CLAVA_ROOT, env=env, stdout=stream,
                                   stderr=subprocess.STDOUT, check=False)
    wall_seconds = time.monotonic() - start
    finished = dt.datetime.now(dt.timezone.utc).isoformat()
    after = ccache_command(cache_operation("ccache_stats_after_seed", cache_dir,
                                           cell["suite"], cell["protocol"], cell["repeat"]), env)
    after_payload_sha = cache_payload_manifest_hash(cache_dir)
    runner_rows = read_jsonl(output) if output.is_file() else []
    if completed.returncode != 0 or after["command_status"] != 0:
        raise RuntimeError(f"{name}: seed process or stats failed; see {log}")
    validate_observations({**cell, "rows": private_rows}, runner_rows)
    parsed = parse_ccache_stats(after["output"])
    expected_calls = sum(1 for row in private_rows for native in row.get("expected_native_calls", [])
                         if native.get("cache_enabled") is True)
    if parsed["cacheable_calls"] != expected_calls or parsed["hits"] + parsed["misses"] != expected_calls:
        raise RuntimeError(f"{name}: unexpected seed counter totals {parsed}; expected {expected_calls}")
    proof = [before_stats, zero, after]
    write_jsonl(proof_path, proof)
    manifest = {
        "schema_version": 1, "record_type": "untimed_cache_seed", "name": name,
        "suite": cell["suite"], "protocol": cell["protocol"], "source_schedule": str(cell["path"]),
        "source_schedule_sha256": source_schedule_sha,
        "seed_schedule_path": str(seed_schedule), "seed_schedule_sha256": sha256_file(seed_schedule),
        "seed_row_count": len(private_rows), "seed_rows_unmeasured": True,
        "observation_path": str(output), "observation_sha256": sha256_file(output),
        "counterproof_path": str(proof_path), "counterproof_sha256": sha256_file(proof_path),
        "log_path": str(log), "java_argv": command, "cwd": str(CLAVA_ROOT),
        "env_policy": env_policy, "explicit_gc_policy_flags": [],
        "gc_policy": "untimed cache-key seed; no explicit/forced GC options or calls; automatic GC allowed",
        "runner_has_no_explicit_gc_call": True, "show_exec_info": False, "java_agent": "none",
        "cache_directory": str(cache_dir), "cache_payload_sha256_before_seed": before_payload_sha,
        "cache_payload_sha256_after_zero": after_zero_sha,
        "cache_payload_sha256_after_seed": after_payload_sha,
        "seed_cache_counters": parsed, "eligible_seed_calls": expected_calls,
        "process_wall_seconds": wall_seconds, "started_utc": started, "finished_utc": finished,
        "exit_code": completed.returncode, "valid": True,
    }
    write_json(manifest_path, manifest)
    return manifest


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--mode", choices=("warmup", "js-jfr", "java-heap", "java-heap-extension", "java-heap-jfr", "authorized-all", "seed-prefix"),
                        default="warmup")
    parser.add_argument("--execute", action="store_true", help="launch the planned JVM cells")
    parser.add_argument("--release-host", action="store_true", help="manual parent handoff required")
    parser.add_argument("--output-root", type=Path)
    parser.add_argument("--corpus-root", type=Path, default=CORPUS_ROOT)
    parser.add_argument("--classes", type=Path, default=CLASSES)
    parser.add_argument("--runtime", type=Path, default=RUNTIME)
    parser.add_argument("--native-tool", type=Path, default=NATIVE_TOOL)
    args = parser.parse_args()
    if args.execute and not args.release_host:
        parser.error("execution is guarded: pass --release-host only after explicit parent handoff")
    if args.release_host and not args.execute:
        parser.error("--release-host must be paired with --execute")
    cells = make_plan(args.mode)
    if args.mode == "authorized-all":
        cells = warmup_cells() + js_jfr_cells()
    elif args.mode == "seed-prefix":
        cells = [cell for cell in warmup_cells() if cell["treatment"] == "prefix" and cell["repeat"] == 1]
    plan = {"mode": args.mode, "cell_count": len(cells), "cells": [
        {"name": cell["name"], "suite": cell["suite"], "protocol": cell["protocol"],
         "treatment": cell["treatment"], "repeat": cell["repeat"], "schedule": str(cell["path"]),
         "schedule_sha256": cell["sha256"], "rows": len(cell["rows"]),
         "eligible_calls": cell["eligible_calls"], "profiled": cell["mode"] == "jfr"}
        for cell in cells]}
    if not args.execute:
        print(json.dumps(plan, indent=2, sort_keys=True))
        return 0
    artifacts = verify_frozen_artifacts()
    if args.corpus_root.resolve() != CORPUS_ROOT.resolve():
        raise RuntimeError("this frozen driver only supports the prepared captured corpus root")
    from group_workload import verify_workload
    verify_workload(args.corpus_root)
    default_folder = ("prefix-cache-seed-r1" if args.mode == "seed-prefix" else
                      "java-heap-extension-r1" if args.mode == "java-heap-extension" else "execution-r4")
    output_root = args.output_root or (args.corpus_root / f"standalone-overlay/warmup-intervention-r1/{default_folder}")
    output_root.mkdir(parents=True, exist_ok=False)
    settings_dir = output_root / "profiles"
    if any(cell["mode"] == "jfr" for cell in cells):
        prepare_jfr_settings(settings_dir)
    write_json(output_root / "execution-plan.json", plan | {"frozen_artifacts": artifacts})
    manifests = []
    if args.mode == "seed-prefix":
        for cell in cells:
            manifest = seed_prefix_cache(cell, output_root, args.classes, args.runtime, args.native_tool)
            manifests.append(manifest)
            print(json.dumps({"seeded": manifest["name"], "wall_s": manifest["process_wall_seconds"],
                              "cache": manifest["seed_cache_counters"]}), flush=True)
        write_json(output_root / "seed-results.json", {"valid": all(row["valid"] for row in manifests),
                                                         "seeds": manifests})
        return 0
    for cell in cells:
        heap = cell.get("heap")
        jfr_path = settings_dir / f"{cell['name']}.jfr" if cell["mode"] == "jfr" else None
        manifests.append(launch_cell(cell, output_root, args.classes, args.runtime, args.native_tool,
                                     heap=heap, jfr_path=jfr_path))
        print(json.dumps({"completed": cell["name"], "wall_s": manifests[-1]["process_wall_seconds"],
                          "cache": manifests[-1]["ccache_counters"]}), flush=True)
    from group_workload import verify_workload
    verify_workload(args.corpus_root)
    write_json(output_root / "execution-results.json", {"valid": all(row["valid"] for row in manifests),
                                                            "batches": manifests})
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

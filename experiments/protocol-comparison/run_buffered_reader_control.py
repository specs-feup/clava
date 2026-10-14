#!/usr/bin/env python3
"""Gated raw-Protobuf BufferedInputStream control for two NAS-LU groups.

Planning is read-only. Execution requires both --execute and --host-released;
it compiles only the benchmark reader overlay into a new class directory.
"""

from __future__ import annotations

import argparse
import copy
import hashlib
import json
import os
from pathlib import Path
import shutil
import statistics
import subprocess
import sys
import time
from typing import Any

sys.dont_write_bytecode = True

CLAVA_ROOT = Path("/home/lmsousa/Documents/Projects/SPeCS/ast-wire-ab-LkMaccmu/clava")
CORPUS_ROOT = CLAVA_ROOT / "experiments/protocol-comparison/results/grouped-live-corpus-20260930-r2"
OVERLAY_ROOT = CORPUS_ROOT / "standalone-overlay/compat-r1"
SCHEDULE_ROOT = OVERLAY_ROOT / "schedules"
CLASSES = CORPUS_ROOT / "standalone-overlay/classes"
RUNTIME = CORPUS_ROOT / "runtime-shared-19c8"
NATIVE_TOOL = CLAVA_ROOT.parent / "clang-dumper/build/tool"
RUNNER_CLASS = "StandaloneParseRunner"
READER_SOURCE = Path(__file__).resolve().parent / "reader-buffer-overlay/FramedProtobufReader.java"
DEFAULT_OUTPUT = CORPUS_ROOT / "standalone-overlay/buffered-reader-control-r1"

FROZEN = {
    "native_tool_sha256": "3f3a1844b1593e820ee525f02e361600fa64637d81df9f56dee3b8825c7119e2",
    "runtime_jar_manifest_sha256": "365906d296ce6ef7ebecab98f29accbfd956ea214422633f6cce288c2e8493b9",
    "class_manifest_sha256": "167db606b53109d27902b081bd5415cd590fac5715a6d0110448818738efded8",
    "runner_class_sha256": "1527b7827d16cd8536715cf2e4c9d1cf4e062cd50b762a9d33b65286d394b7c5",
    "reader_class_sha256": "330e8b855e1381999f46c8aa05f5301c4f78269838e60de50d31d26b83b939a8",
    "compat_proto_types_sha256": "825dbb54cd7270bde5e79abc232d6a3e45e4285d1d191485182d7364e9084230",
}
JAVA_OPTION_ENV = ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "JAVA_OPTS", "GRADLE_OPTS")
GROUPS = (
    {
        "suite": "clava-js", "input_id": "js-group-0026", "label": "inline_nas_lu.c",
        "group_sha256": "44b20778da8d738e9d6cd8c9bd6c9d246a4597c7cf7b8f4b18306348c31639a5",
        "args_sha256": "47440fda256ce4c802e84f121172205ac0a4d9e7fde3d04b10689d308979f09e",
        "options_sha256": "1c7a96fd2cb1d5719e843ec4e2a094f988c2707180b094316362e0c0ae1866af",
        "source_sha256": "26409fb2ace4dbb9e350b2990199c6b38f91814797bb93e8ef2770f36a121cc8",
    },
    {
        "suite": "java", "input_id": "java-group-0007", "label": "nas_lu.c",
        "group_sha256": "6772a6a2a6458dfa3fbd1f6d3e49367b0ef50d0c6537dd4301bd97d17afda6cb",
        "args_sha256": "23420503d3d556860760f8dd9e9b4d742dd168150ab5393dc23dd5b01f9c9b60",
        "options_sha256": "62686071eb537b695eb2f3f976216239f3ccc64845f1b61323633ec54baaacc9",
        "source_sha256": "26409fb2ace4dbb9e350b2990199c6b38f91814797bb93e8ef2770f36a121cc8",
    },
)


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def class_manifest_hash(root: Path) -> str:
    entries = [f"{sha256_file(path)}  ./{path.relative_to(root).as_posix()}\n"
               for path in sorted(root.rglob("*.class"))]
    return hashlib.sha256("".join(entries).encode()).hexdigest()


def jar_manifest_hash(root: Path) -> str:
    jars = {path.relative_to(root).as_posix(): sha256_file(path)
            for path in sorted(root.rglob("*.jar")) if path.is_file()}
    canonical = json.dumps(jars, sort_keys=True, separators=(",", ":")).encode()
    return hashlib.sha256(canonical).hexdigest()


def verify_frozen_artifacts() -> dict[str, str]:
    observed = {
        "native_tool_sha256": sha256_file(NATIVE_TOOL),
        "runtime_jar_manifest_sha256": jar_manifest_hash(RUNTIME),
        "class_manifest_sha256": class_manifest_hash(CLASSES),
        "runner_class_sha256": sha256_file(CLASSES / "StandaloneParseRunner.class"),
        "reader_class_sha256": sha256_file(CLASSES / "pt/up/fe/specs/clang/wire/ProtoNodeDataReader.class"),
        "compat_proto_types_sha256": sha256_file(CLAVA_ROOT.parent / "clang-dumper/src/Clava/ProtoTypes.cpp"),
    }
    mismatch = {key: {"expected": value, "observed": observed.get(key)}
                for key, value in FROZEN.items() if observed.get(key) != value}
    if mismatch:
        raise RuntimeError(f"frozen compat-r1 artifacts changed: {mismatch}")
    return observed


def read_jsonl(path: Path) -> list[dict[str, Any]]:
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]


def write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def write_jsonl(path: Path, rows: list[dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("".join(json.dumps(row, separators=(",", ":")) + "\n" for row in rows),
                    encoding="utf-8")


def selected_row(spec: dict[str, str], protocol: str) -> tuple[dict[str, Any], Path]:
    path = SCHEDULE_ROOT / f"measure-{spec['suite']}-{protocol}-direct-r01.jsonl"
    rows = read_jsonl(path)
    matches = [row for row in rows if row.get("input_id") == spec["input_id"]]
    if len(matches) != 1:
        raise RuntimeError(f"expected one {spec['input_id']} row in {path.name}, found {len(matches)}")
    row = matches[0]
    expected = {
        "suite": spec["suite"], "source_label": spec["label"], "phase": "measure",
        "protocol": protocol, "cache_mode": "direct", "compression_policy": "raw_control",
        "expected_compressed": False, "disable_compression": True,
        "source_sha256": spec["group_sha256"],
        "args_sha256": spec["args_sha256"], "options_sha256": spec["options_sha256"],
        "expected_native_count": 1,
    }
    for key, value in expected.items():
        if row.get(key) != value:
            raise RuntimeError(f"{spec['input_id']} {key} mismatch: {row.get(key)!r} != {value!r}")
    sources = row.get("source_files", [])
    if len(sources) != 1 or sources[0].get("sha256") != spec["source_sha256"]:
        raise RuntimeError(f"{spec['input_id']} does not have the locked single-source digest")
    source_path = Path(sources[0]["original_path"])
    if not source_path.is_file() or sha256_file(source_path) != spec["source_sha256"]:
        raise RuntimeError(f"{spec['input_id']} source file missing or changed: {source_path}")
    calls = row.get("expected_native_calls", [])
    if len(calls) != 1 or Path(calls[0]["compiler_argv"][0]).resolve() != NATIVE_TOOL.resolve():
        raise RuntimeError(f"{spec['input_id']} native executable does not match pinned tool")
    if calls[0].get("wire_format") != protocol or calls[0].get("compressed") is not False:
        raise RuntimeError(f"{spec['input_id']} captured native flags are not raw {protocol}")
    if calls[0].get("cache_enabled") is not False or calls[0].get("ccache_disabled") is not True:
        raise RuntimeError(f"{spec['input_id']} captured native call is not the uncached direct control")
    config = row.get("parser_config", {})
    if (config.get("ast_dump_cache") is not False or config.get("show_exec_info") is not False
            or config.get("syntax_only") is not False):
        raise RuntimeError(f"{spec['input_id']} is not direct, no-SHOW_EXEC_INFO, full-AST parsing")
    return row, path


def select_operations() -> tuple[dict[str, dict[str, dict[str, Any]]], dict[str, Any]]:
    operations: dict[str, dict[str, dict[str, Any]]] = {"text": {}, "protobuf": {}}
    schedule_fingerprints: dict[str, str] = {}
    for spec in GROUPS:
        for protocol in ("text", "protobuf"):
            row, schedule = selected_row(spec, protocol)
            operations[protocol][spec["input_id"]] = row
            schedule_fingerprints[str(schedule)] = sha256_file(schedule)
        text = operations["text"][spec["input_id"]]
        proto = operations["protobuf"][spec["input_id"]]
        for key in ("source_sha256", "args_sha256", "options_sha256", "compiler_options", "source_files",
                    "source_paths", "replay_cwd", "generated_parse_root", "standard"):
            if text.get(key) != proto.get(key):
                raise RuntimeError(f"{spec['input_id']} Text/Protobuf {key} is not identical")
        text_config = dict(text.get("parser_config", {}))
        proto_config = dict(proto.get("parser_config", {}))
        text_config.pop("dumper_folder", None)
        proto_config.pop("dumper_folder", None)
        if text_config != proto_config:
            raise RuntimeError(f"{spec['input_id']} Text/Protobuf parser configuration differs")
    return operations, schedule_fingerprints


def source_sha256() -> str:
    return sha256_file(READER_SOURCE)


def plan(artifacts: dict[str, str], operations: dict[str, dict[str, dict[str, Any]]],
         schedule_hashes: dict[str, str]) -> dict[str, Any]:
    return {
        "schema_version": 1,
        "source_overlay": str(READER_SOURCE),
        "source_overlay_sha256": source_sha256(),
        "reader_properties": {
            "buffer": "clava.astWireBenchmarkBufferedReader",
            "count_profile_only": "clava.astWireBenchmarkCountInputReads",
            "buffer_bytes": 65536,
        },
        "frozen_artifacts": artifacts,
        "captured_schedule_sha256": schedule_hashes,
        "groups": [{"suite": group["suite"], "input_id": group["input_id"],
                    "source_label": group["label"], "group_sha256": group["group_sha256"],
                    "args_sha256": group["args_sha256"], "options_sha256": group["options_sha256"],
                    "input_source_sha256": group["source_sha256"],
                    "captured_source_path": operations["protobuf"][group["input_id"]]["source_files"][0]
                    ["original_path"]} for group in GROUPS],
        "timing_design": {
            "pairs": 4, "jvms": 8, "conditions": ["unbuffered", "buffered"],
            "warmups_per_group_per_jvm": 2, "measured_per_group_per_jvm": 2,
            "rotation": ["unbuffered/buffered", "buffered/unbuffered"] * 2,
            "group_order_reversed_each_pair": True,
            "phase": "measure", "agent": "none", "explicit_gc_flags": [],
            "heap": "JVM default", "native_compile_and_reader_counts_excluded": True,
        },
        "fidelity_design": "separate Text, Protobuf-unbuffered, and Protobuf-buffered full-graph digests",
        "diagnostics_design": "separate native-argv/read-phase and underlying-stream request-count runs",
    }


def compile_reader(classes: Path) -> list[str]:
    javac = shutil.which("javac")
    if not javac:
        raise RuntimeError("javac not found on PATH")
    classes.mkdir(parents=True)
    compile_cp = os.pathsep.join((str(CLASSES), str(RUNTIME / "lib" / "*")))
    command = [javac, "-proc:none", "-sourcepath", "", "-classpath", compile_cp,
               "-d", str(classes), str(READER_SOURCE)]
    completed = subprocess.run(command, capture_output=True, text=True, check=False)
    if completed.returncode:
        raise RuntimeError("reader overlay compilation failed:\n" + completed.stdout + completed.stderr)
    expected_classes = {
        "pt/up/fe/specs/clang/wire/FramedProtobufReader.class",
        "pt/up/fe/specs/clang/wire/FramedProtobufReader$MessageParser.class",
        "pt/up/fe/specs/clang/wire/FramedProtobufReader$CountingInputStream.class",
    }
    found_classes = {path.relative_to(classes).as_posix() for path in classes.rglob("*.class")}
    if found_classes != expected_classes:
        raise RuntimeError(f"javac emitted unexpected classes: {sorted(found_classes)}")
    return command


def command_env(scratch: Path) -> tuple[dict[str, str], list[str]]:
    env = os.environ.copy()
    removed = [name for name in JAVA_OPTION_ENV if name in env]
    for name in JAVA_OPTION_ENV:
        env.pop(name, None)
    env.update({"CLANG_DUMPER_TOOL": str(NATIVE_TOOL), "TMPDIR": str(scratch),
                "TMP": str(scratch), "TEMP": str(scratch), "XDG_CACHE_HOME": str(scratch)})
    scratch.mkdir(parents=True, exist_ok=True)
    return env, removed


def java_command(java: str, classes: Path, schedule: Path, output: Path, scratch: Path,
                 buffered: bool, count_path: Path | None = None) -> list[str]:
    classpath = os.pathsep.join((str(classes), str(CLASSES), str(RUNTIME / "lib" / "*")))
    command = [java, f"-Djava.io.tmpdir={scratch}",
               f"-Dclava.astWireBenchmarkBufferedReader={str(buffered).lower()}"]
    if count_path is not None:
        command += ["-Dclava.astWireBenchmarkCountInputReads=true",
                    f"-Dclava.astWireBenchmarkInputReadCountsPath={count_path}"]
    return command + ["-cp", classpath, RUNNER_CLASS, "--schedule", str(schedule), "--output", str(output)]


def run_batch(name: str, operations: list[dict[str, Any]], roles: list[dict[str, Any]],
              output_root: Path, java: str, classes: Path, buffered: bool,
              count_profile: bool = False) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    batch_dir = output_root / name
    batch_dir.mkdir(parents=True, exist_ok=False)
    schedule = batch_dir / "schedule.jsonl"
    result_path = batch_dir / "results.jsonl"
    log_path = batch_dir / "process.log"
    count_path = batch_dir / "input-read-counts.jsonl" if count_profile else None
    write_jsonl(schedule, operations)
    scratch = batch_dir / "tmp"
    env, removed_options = command_env(scratch)
    command = java_command(java, classes, schedule, result_path, scratch, buffered, count_path)
    start = time.monotonic()
    with log_path.open("w", encoding="utf-8") as log:
        completed = subprocess.run(command, cwd=CLAVA_ROOT, env=env, stdout=log,
                                   stderr=subprocess.STDOUT, check=False)
    wall_seconds = time.monotonic() - start
    if completed.returncode != 0:
        raise RuntimeError(f"{name}: parser runner exited {completed.returncode}; see {log_path}")
    results = read_jsonl(result_path) if result_path.is_file() else []
    if len(results) != len(operations):
        raise RuntimeError(f"{name}: returned {len(results)} rows for {len(operations)} schedule entries")
    for index, (operation, result) in enumerate(zip(operations, results), start=1):
        if result.get("schedule_line") != index:
            raise RuntimeError(f"{name} row {index}: runner output order differs from schedule")
        for key in ("phase", "suite", "input_id", "source_label", "protocol", "cache_mode",
                    "source_sha256", "args_sha256", "options_sha256"):
            if operation.get(key) is not None and result.get(key) != operation[key]:
                raise RuntimeError(f"{name} row {index}: {key} differs from schedule")
        if result.get("valid") is not True:
            raise RuntimeError(f"{name} row {index} failed: {result.get('error')}")
    manifest = {
        "name": name, "buffered": buffered, "count_profile": count_profile,
        "schedule_sha256": sha256_file(schedule), "results_sha256": sha256_file(result_path),
        "log_sha256": sha256_file(log_path), "java_argv": command,
        "cwd": str(CLAVA_ROOT), "java_agent": "none", "explicit_gc_flags": [],
        "heap": "JVM default", "removed_inherited_java_option_variables": removed_options,
        "process_wall_seconds": wall_seconds,
        "rows": [{**role, "result": result} for role, result in zip(roles, results)],
    }
    if count_path is not None:
        manifest["input_read_counts_sha256"] = sha256_file(count_path)
        manifest["input_read_counts"] = read_jsonl(count_path)
    write_json(batch_dir / "manifest.json", manifest)
    return results, manifest


def check_digest_matrix(digests: dict[str, dict[str, str]]) -> None:
    for group in GROUPS:
        item = digests[group["input_id"]]
        if set(item) != {"text", "protobuf-unbuffered", "protobuf-buffered"}:
            raise RuntimeError(f"missing fidelity variants for {group['input_id']}: {sorted(item)}")
        if len(set(item.values())) != 1:
            raise RuntimeError(f"full graph digest differs for {group['input_id']}: {item}")


def execute(output_root: Path, operations: dict[str, dict[str, dict[str, Any]]], artifacts: dict[str, str],
            schedule_hashes: dict[str, str]) -> None:
    if output_root.exists():
        raise RuntimeError(f"output directory already exists; refusing to overwrite: {output_root}")
    output_root.mkdir(parents=True)
    write_json(output_root / "plan.json", plan(artifacts, operations, schedule_hashes))
    overlay_classes = output_root / "reader-overlay-classes"
    compile_command = compile_reader(overlay_classes)
    if sha256_file(NATIVE_TOOL) != FROZEN["native_tool_sha256"]:
        raise RuntimeError("pinned native executable changed before execution")
    java = shutil.which("java")
    if not java:
        raise RuntimeError("java not found on PATH")
    version_env = os.environ.copy()
    for name in JAVA_OPTION_ENV:
        version_env.pop(name, None)
    version = subprocess.run([java, "-version"], env=version_env,
                             capture_output=True, text=True, check=False)
    if version.returncode:
        raise RuntimeError("java -version failed")

    common_manifest = {
        "compile_command": compile_command,
        "reader_source_sha256": source_sha256(),
        "overlay_class_files": {
            path.relative_to(overlay_classes).as_posix(): sha256_file(path)
            for path in sorted(overlay_classes.rglob("*.class"))
        },
        "java_executable": java,
        "java_version_output": version.stdout + version.stderr,
        "frozen_artifacts": artifacts,
    }
    write_json(output_root / "execution.json", common_manifest)

    fidelity_digests: dict[str, dict[str, str]] = {group["input_id"]: {} for group in GROUPS}
    for name, protocol, buffered in (
        ("fidelity-text", "text", False),
        ("fidelity-protobuf-unbuffered", "protobuf", False),
        ("fidelity-protobuf-buffered", "protobuf", True),
    ):
        fidelity_ops = []
        roles = []
        for group in GROUPS:
            op = copy.deepcopy(operations[protocol][group["input_id"]])
            op["phase"] = "fidelity"
            fidelity_ops.append(op)
            roles.append({"suite": group["suite"], "input_id": group["input_id"], "role": name})
        rows, _ = run_batch(name, fidelity_ops, roles, output_root, java, overlay_classes, buffered)
        key = "text" if protocol == "text" else ("protobuf-buffered" if buffered else "protobuf-unbuffered")
        for row in rows:
            digest = row.get("complete_graph_sha256")
            if not digest or row.get("app_returned_null") is not False:
                raise RuntimeError(f"{name}: missing app/full graph digest for {row.get('input_id')}")
            fidelity_digests[row["input_id"]][key] = digest
    check_digest_matrix(fidelity_digests)
    write_json(output_root / "fidelity-gate.json", {
        "valid": True, "full_graph_digests": fidelity_digests,
        "assertion": "Text, Protobuf unbuffered, and Protobuf buffered digests match per selected group",
    })

    measured_rows: list[dict[str, Any]] = []
    pair_order: list[dict[str, Any]] = []
    for pair_index in range(4):
        condition_order = [False, True] if pair_index % 2 == 0 else [True, False]
        group_order = GROUPS if pair_index % 2 == 0 else tuple(reversed(GROUPS))
        for condition_index, buffered in enumerate(condition_order):
            name = f"pair-{pair_index + 1:02d}-{'buffered' if buffered else 'unbuffered'}"
            schedule_ops: list[dict[str, Any]] = []
            roles: list[dict[str, Any]] = []
            for group in group_order:
                base = operations["protobuf"][group["input_id"]]
                for role, count in (("warmup", 2), ("measured", 2)):
                    for repetition in range(1, count + 1):
                        schedule_ops.append(copy.deepcopy(base))
                        roles.append({"suite": group["suite"], "input_id": group["input_id"],
                                      "role": role, "within_role_repeat": repetition,
                                      "pair": pair_index + 1, "condition_order_index": condition_index + 1})
            rows, _ = run_batch(name, schedule_ops, roles, output_root, java, overlay_classes, buffered)
            for role, result in zip(roles, rows):
                if role["role"] == "measured":
                    measured_rows.append({**role, "buffered": buffered,
                                          "elapsed_ms": result["elapsed_ms"]})
            pair_order.append({"pair": pair_index + 1, "condition": name,
                               "buffered": buffered,
                               "group_order": [group["input_id"] for group in group_order]})
    paired_differences = []
    for group in GROUPS:
        for pair_index in range(1, 5):
            pair_values = {
                buffered: [row["elapsed_ms"] for row in measured_rows
                           if row["input_id"] == group["input_id"]
                           and row["pair"] == pair_index and row["buffered"] is buffered]
                for buffered in (False, True)
            }
            unbuffered_median = statistics.median(pair_values[False])
            buffered_median = statistics.median(pair_values[True])
            paired_differences.append({
                "input_id": group["input_id"], "pair": pair_index,
                "unbuffered_median_ms": unbuffered_median,
                "buffered_median_ms": buffered_median,
                "buffered_minus_unbuffered_ms": buffered_median - unbuffered_median,
            })
    write_json(output_root / "timing-summary.json", {
        "narrow_scope": "two selected NAS-LU groups; not a suite-wide result",
        "pairs": pair_order,
        "measured_calls_per_group_per_condition": 8,
        "warmups_per_group_per_jvm": 2,
        "paired_samples": measured_rows,
        "paired_median_differences_ms": paired_differences,
        "median_elapsed_ms": {
            group["input_id"]: {
                str(buffered).lower(): statistics.median(
                    row["elapsed_ms"] for row in measured_rows
                    if row["input_id"] == group["input_id"] and row["buffered"] is buffered)
                for buffered in (False, True)
            } for group in GROUPS
        },
        "timing_boundary": "StandaloneParseRunner phase=measure, outer CodeParser.parse",
        "profile_counters_excluded": True,
    })

    # Diagnostics are isolated from timed JVs: first capture existing reader phases/native argv,
    # then capture calls made to the stream beneath BufferedInputStream.
    diagnostic_group = GROUPS[1]
    diagnostic_results: dict[str, dict[str, Any]] = {}
    count_results: dict[str, dict[str, Any]] = {}
    for buffered in (False, True):
        label = "buffered" if buffered else "unbuffered"
        base = copy.deepcopy(operations["protobuf"][diagnostic_group["input_id"]])
        base["phase"] = "diagnostic"
        base_roles = [{"suite": diagnostic_group["suite"],
                       "input_id": diagnostic_group["input_id"], "role": "reader-phase"}]
        rows, manifest = run_batch(f"diagnostic-{label}", [base], base_roles,
                                   output_root, java, overlay_classes, buffered)
        result = rows[0]
        if result.get("native_calls_match") is not True or len(result.get("metrics", [])) != 1:
            raise RuntimeError(f"diagnostic-{label}: captured native invocation did not match frozen args")
        diagnostic_results[label] = {
            "metrics": result["metrics"][0],
            "parse_phase_metrics": result.get("parse_phase_metrics"),
            "process_manifest": manifest,
        }

        count_base = copy.deepcopy(base)
        count_role = [{"suite": diagnostic_group["suite"],
                       "input_id": diagnostic_group["input_id"], "role": "input-read-counts"}]
        count_rows, count_manifest = run_batch(
            f"read-count-profile-{label}", [count_base], count_role,
            output_root, java, overlay_classes, buffered, count_profile=True)
        if count_rows[0].get("native_calls_match") is not True:
            raise RuntimeError(f"read-count-profile-{label}: native invocation mismatch")
        count_results[label] = {
            "input_read_counts": count_manifest.get("input_read_counts"),
            "profile_only": True,
        }
        count_rows_data = count_manifest.get("input_read_counts", [])
        count_metrics = count_rows[0].get("metrics", [])
        if (len(count_rows_data) != 1 or len(count_metrics) != 1
                or count_rows_data[0].get("frames") != count_metrics[0].get("frames")
                or count_rows_data[0].get("encoded_bytes") != count_metrics[0].get("encoded_bytes")
                or count_rows_data[0].get("bytes_read") != count_metrics[0].get("encoded_bytes")):
            raise RuntimeError(f"read-count-profile-{label}: input byte/frame accounting mismatch")
    off_counts = count_results["unbuffered"]["input_read_counts"][0]
    on_counts = count_results["buffered"]["input_read_counts"][0]
    if not (off_counts["single_byte_calls"] > on_counts["single_byte_calls"]
            and off_counts["single_byte_calls"] + off_counts["bulk_calls"]
            > on_counts["single_byte_calls"] + on_counts["bulk_calls"]):
        raise RuntimeError("separate stream profile did not show fewer underlying read requests when buffered")
    write_json(output_root / "read-diagnostics.json", {
        "timed_results_use_no_counter": True,
        "reader_phase_rows": diagnostic_results,
        "underlying_input_stream_request_counts": count_results,
        "count_definition": "calls to wrapped input stream read() and read(byte[],off,len); not OS syscall counts",
        "interpretation_limit": "separate instrumented diagnostics, not a timing explanation by themselves",
    })

    if class_manifest_hash(CLASSES) != FROZEN["class_manifest_sha256"]:
        raise RuntimeError("frozen baseline class directory changed during run")
    if jar_manifest_hash(RUNTIME) != FROZEN["runtime_jar_manifest_sha256"]:
        raise RuntimeError("frozen runtime jars changed during run")
    if sha256_file(NATIVE_TOOL) != FROZEN["native_tool_sha256"]:
        raise RuntimeError("pinned native executable changed during run")
    write_json(output_root / "completed.json", {"valid": True, "native_tool_sha256": FROZEN["native_tool_sha256"]})


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--execute", action="store_true", help="compile overlay and run the bounded control")
    parser.add_argument("--host-released", action="store_true",
                        help="required acknowledgment that the benchmark host was handed over")
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    args = parser.parse_args()
    artifacts = verify_frozen_artifacts()
    operations, schedule_hashes = select_operations()
    run_plan = plan(artifacts, operations, schedule_hashes)
    if not args.execute:
        print(json.dumps(run_plan, indent=2, sort_keys=True))
        return 0
    if not args.host_released:
        parser.error("--execute requires --host-released; do not compete with the active matrix")
    execute(args.output.resolve(), operations, artifacts, schedule_hashes)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

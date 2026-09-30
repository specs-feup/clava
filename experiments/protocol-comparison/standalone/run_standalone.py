#!/usr/bin/env python3
"""Prepare or run standalone Text/Protobuf per-source parses from a frozen corpus.

`--pilot` materializes five representative inputs and writes a Java schedule only.
`--run` is the full 191/247-event, two-cache-mode, three-repeat experiment. It is
deliberately explicit so preparing a schedule can never start benchmark work.
"""

from __future__ import annotations

import argparse
import collections
import csv
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import re
import shlex
import shutil
import subprocess
import sys
from typing import Any, Iterable


SCRIPT_ROOT = Path(__file__).resolve().parent
CLAVA_ROOT = SCRIPT_ROOT.parents[2]
DEFAULT_CORPUS = CLAVA_ROOT / "experiments/protocol-comparison/results/standalone-corpus-20260930"
NATIVE_EVENTS = {"clava-js": 191, "java": 247}
PROTOCOLS = ("text", "protobuf")
CACHE_MODES = ("warmcache", "directbypass")
JAVA_CACHE_MODE = {"warmcache": "warm", "directbypass": "bypass"}
CCACHE_FOLDER = "clang-dumper-protobuf-ccache-v1"
RUNNER_SOURCE = SCRIPT_ROOT / "StandaloneParseRunner.java"
PARSER_SOURCES = (
    CLAVA_ROOT / "ClangAstParser/src/pt/up/fe/specs/clang/codeparser/ParallelCodeParser.java",
    CLAVA_ROOT / "ClangAstParser/src/pt/up/fe/specs/clang/dumper/AstWireBenchmarkIdentity.java",
    CLAVA_ROOT / "ClangAstParser/src/pt/up/fe/specs/clang/dumper/ClangAstCorpusCapture.java",
    CLAVA_ROOT / "ClangAstParser/src/pt/up/fe/specs/clang/dumper/ClangAstDumper.java",
)
MEASURE_FIELDS = (
    "suite", "input_id", "source_label", "protocol", "cache_mode", "repeat",
    "elapsed_ms", "source_sha256", "args_sha256", "valid", "phase", "event_id",
    "options_sha256", "parse_pass", "test_id", "resource_key",
    "generated_code_sha256", "generated_code_bytes", "ast_root_kind", "ast_node_count",
    "ast_node_kind_counts_sha256", "validity_reason",
)
DIAGNOSTIC_FIELDS = (
    "suite", "input_id", "source_label", "protocol", "cache_mode", "repeat",
    "native_ms", "ccache_invoke_ms", "read_ms", "ast_ms", "records", "nodes", "bytes",
    "valid", "phase", "event_id", "source_sha256", "args_sha256", "options_sha256",
    "cached", "metrics_json", "validity_reason",
)
FIDELITY_FIELDS = (
    "suite", "input_id", "source_label", "protocol", "cache_mode", "event_id",
    "source_sha256", "args_sha256", "options_sha256", "generated_code_sha256",
    "generated_code_bytes", "ast_root_kind", "ast_node_count",
    "ast_node_kind_counts_sha256", "valid", "validity_reason",
)
RUN_FIELDS = (
    "suite", "protocol", "cache_mode", "phase", "repeat", "return_code",
    "input_count", "valid_count", "elapsed_ms_sum", "ccache_cacheable_calls",
    "ccache_hits", "ccache_misses", "ccache_uncacheable_calls", "ccache_adapter_events",
    "ccache_counters_match", "valid", "schedule", "output", "log",
)


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def canonical_sha256(value: Any) -> str:
    encoded = json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode()
    return sha256_bytes(encoded)


def read_jsonl(path: Path) -> list[dict[str, Any]]:
    with path.open(encoding="utf-8") as stream:
        return [json.loads(line) for line in stream if line.strip()]


def write_json(path: Path, value: Any) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    temporary.write_text(json.dumps(value, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    temporary.replace(path)


def write_jsonl(path: Path, rows: Iterable[dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    with temporary.open("w", encoding="utf-8", newline="\n") as stream:
        for row in rows:
            stream.write(json.dumps(row, separators=(",", ":"), ensure_ascii=False) + "\n")
    temporary.replace(path)


def write_csv(path: Path, fields: tuple[str, ...], rows: list[dict[str, Any]]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    with temporary.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=fields, extrasaction="ignore")
        writer.writeheader()
        writer.writerows(rows)
    temporary.replace(path)


def verify_corpus(corpus_root: Path) -> tuple[dict[str, Any], list[dict[str, Any]], str]:
    manifest_path = corpus_root / "corpus.json"
    manifest_bytes = manifest_path.read_bytes()
    manifest = json.loads(manifest_bytes)
    if manifest.get("valid") is not True:
        raise RuntimeError("capture corpus manifest is not marked valid")
    for relative, expected in manifest.get("hashes", {}).items():
        path = corpus_root / relative
        if not path.is_file() or sha256_file(path) != expected:
            raise RuntimeError(f"frozen corpus artifact hash mismatch: {path}")

    events = read_jsonl(corpus_root / "replay_events.jsonl")
    if len(events) != sum(NATIVE_EVENTS.values()):
        raise RuntimeError(f"expected 438 replay events, found {len(events)}")
    suite_counts = collections.Counter(event.get("suite") for event in events)
    if dict(suite_counts) != NATIVE_EVENTS:
        raise RuntimeError(f"unexpected suite event coverage: {dict(suite_counts)}")
    event_ids = [event.get("event_id") for event in events]
    if len(set(event_ids)) != len(events):
        raise RuntimeError("capture corpus dropped or duplicated event IDs")
    all_ordinals = sorted(int(event["ordinal"]) for event in events)
    if all_ordinals != list(range(1, len(events) + 1)):
        raise RuntimeError("capture event ordinals are not a complete 1..438 sequence")

    file_rows = read_jsonl(corpus_root / "files.jsonl")
    blob_by_sha = {entry["sha256"]: entry for entry in file_rows}
    for entry in file_rows:
        blob = corpus_root / entry["blob"]
        if not blob.is_file() or sha256_file(blob) != entry["sha256"]:
            raise RuntimeError(f"content-addressed corpus blob is missing or changed: {blob}")
    for event in events:
        for file_entry in [event["source"], *event.get("dependencies", [])]:
            if file_entry["sha256"] not in blob_by_sha:
                raise RuntimeError(f"unmanifested input blob in event {event['event_id']}")
    runtime = manifest["runtime"]
    runtime_root = Path(runtime["root"])
    jars = {
        str(path.relative_to(runtime_root)): sha256_file(path)
        for path in sorted(runtime_root.rglob("*.jar")) if path.is_file()
    }
    jar_manifest = {"jar_count": len(jars), "sha256": canonical_sha256(jars)}
    if jar_manifest != {"jar_count": runtime["manifest"]["jar_count"],
                        "sha256": runtime["manifest"]["sha256"]}:
        raise RuntimeError("staged Java runtime no longer matches the frozen corpus manifest")
    native_tool = Path(runtime["native_tool"])
    if not native_tool.is_file() or sha256_file(native_tool) != runtime["native_tool_sha256"]:
        raise RuntimeError("pinned clang-dumper executable changed since corpus capture")
    return manifest, events, sha256_bytes(manifest_bytes)


def map_path(original: str | Path, replay_root: Path) -> str:
    path = Path(original)
    if not path.is_absolute():
        raise RuntimeError(f"expected absolute captured path, got {original!r}")
    return str(replay_root / path.relative_to("/"))


def remap_token(token: str, replay_root: Path) -> str:
    """Remap absolute path arguments without re-tokenizing or shell quoting."""
    if token.startswith("/"):
        return map_path(token, replay_root)
    for prefix in ("-I", "-isystem", "-iquote", "-idirafter", "-include", "-imacros", "-isysroot"):
        if token.startswith(prefix + "/"):
            return prefix + map_path(token[len(prefix):], replay_root)
    for separator in ("=", ":"):
        index = token.find(separator + "/")
        if index >= 0:
            return token[:index + 1] + map_path(token[index + 1:], replay_root)
    return token


def remap_options(options: list[str], replay_root: Path) -> list[str]:
    return [remap_token(option, replay_root) for option in options]


def normalized_native_argv(event: dict[str, Any], replay_root: Path) -> list[str]:
    source = map_path(event["source"]["original_path"], replay_root)
    source_original = Path(event["source"]["original_path"])
    original_cwd = Path(event.get("working_directory") or event["original_cwd"])
    argv = event["argv"]
    normalized: list[str] = []
    index = 0
    while index < len(argv):
        item = argv[index]
        if item == "-o" and index + 1 < len(argv):
            index += 2
            continue
        if item == "-ast-dump-format=text":
            index += 1
            continue
        if item.startswith("-id="):
            normalized.append("-id=<id>")
        elif item == event["source"]["original_path"] or is_source_argument(item, source_original, original_cwd):
            normalized.append("<source>")
        elif index == 0:
            # The executable is pinned by hash and deliberately remains a host path.
            normalized.append(item)
        else:
            mapped = remap_token(item, replay_root)
            normalized.append("<source>" if mapped == source else mapped)
        index += 1
    return normalized


def is_source_argument(argument: str, source: Path, working_directory: Path) -> bool:
    try:
        candidate = Path(argument)
        if not candidate.is_absolute():
            candidate = working_directory / candidate
        return os.path.normpath(str(candidate)) == os.path.normpath(str(source))
    except (TypeError, ValueError):
        return False


def semantic_options_hash(event: dict[str, Any]) -> str:
    config = dict(event.get("parser_config") or {})
    for key in ("dumper_folder", "show_exec_info", "ast_dump_cache"):
        config.pop(key, None)
    return canonical_sha256({
        "compiler_options": event["compiler_options"],
        "parser_config": config,
        "effective_libc_mode": event.get("effective_libc_mode"),
        "data_store_options": event.get("data_store_options", {}),
    })


def event_label(event: dict[str, Any]) -> str:
    base = str(event.get("source_label") or Path(event["source"]["original_path"]).name)
    tags: list[str] = []
    if event.get("parse_pass"):
        tags.append(str(event["parse_pass"]))
    if event.get("test_id"):
        tags.append(f"test {event['test_id']}")
    if event.get("resource_key"):
        tags.append(f"resource {event['resource_key']}")
    context = f"{event['suite']} #{int(event['ordinal']):03d}"
    tags.append(context)
    return f"{base} [{', '.join(tags)}]"


def input_id(event: dict[str, Any]) -> str:
    suite = "js" if event["suite"] == "clava-js" else "java"
    return f"{suite}-{int(event['ordinal']):04d}"


def materialize_event(corpus_root: Path, event: dict[str, Any], root_parent: Path) -> dict[str, Any]:
    event_root = root_parent / event["suite"] / input_id(event)
    rootfs = event_root / "rootfs"
    files_by_path: dict[str, dict[str, Any]] = {}
    for item in [event["source"], *event.get("dependencies", [])]:
        previous = files_by_path.get(item["original_path"])
        if previous is not None and previous["sha256"] != item["sha256"]:
            raise RuntimeError(f"one event has conflicting snapshots for {item['original_path']}")
        files_by_path[item["original_path"]] = item
    for original, item in files_by_path.items():
        target = Path(map_path(original, rootfs))
        target.parent.mkdir(parents=True, exist_ok=True)
        blob = corpus_root / item["blob"]
        try:
            os.link(blob, target)
        except OSError:
            shutil.copyfile(blob, target)
    source_path = Path(map_path(event["source"]["original_path"], rootfs))
    replay_cwd = Path(map_path(event["working_directory"], rootfs))
    replay_cwd.mkdir(parents=True, exist_ok=True)
    generated_root_value = event.get("generated_parse_root")
    generated_root = Path(map_path(generated_root_value, rootfs)) if generated_root_value else None
    if generated_root is not None:
        generated_root.mkdir(parents=True, exist_ok=True)

    staged = []
    for original, item in sorted(files_by_path.items()):
        replay_path = Path(map_path(original, rootfs))
        if sha256_file(replay_path) != item["sha256"]:
            raise RuntimeError(f"materialized file hash mismatch for {replay_path}")
        staged.append({
            "event_id": event["event_id"], "input_id": input_id(event),
            "original_path": original, "replay_path": str(replay_path),
            "sha256": item["sha256"], "size_bytes": item["size_bytes"],
        })
    return {
        "event_root": event_root, "rootfs": rootfs, "source_path": source_path,
        "replay_cwd": replay_cwd, "generated_parse_root": generated_root,
        "files": staged,
    }


def select_pilot_events(events: list[dict[str, Any]]) -> list[dict[str, Any]]:
    def choose(predicate) -> dict[str, Any]:
        found = next((event for event in events if predicate(event)), None)
        if found is None:
            raise RuntimeError("corpus lacks a required pilot category")
        return found

    paths = lambda event: Path(event["source"]["original_path"])
    chosen = [
        choose(lambda e: e["suite"] == "clava-js" and paths(e).suffix.lower() == ".c"),
        choose(lambda e: e["suite"] == "clava-js" and paths(e).suffix.lower() == ".cpp"),
        choose(lambda e: e["suite"] == "java" and paths(e).suffix.lower() == ".cl"
               and e.get("parse_pass") == "original"),
        choose(lambda e: e["suite"] == "clava-js" and paths(e).suffix.lower() in (".h", ".hpp")),
        choose(lambda e: e["suite"] == "java" and paths(e).suffix.lower() == ".cpp"
               and e.get("parse_pass") == "roundtrip"),
    ]
    return chosen


def java_runner_class(source: Path) -> str:
    first = source.read_text(encoding="utf-8").splitlines()[:12]
    package = next((line.split()[1].rstrip(";") for line in first
                    if line.strip().startswith("package ")), None)
    return f"{package}.StandaloneParseRunner" if package else "StandaloneParseRunner"


def source_fingerprint() -> dict[str, Any]:
    sources = [*PARSER_SOURCES, RUNNER_SOURCE, Path(__file__).resolve()]
    missing = [str(path) for path in sources if not path.is_file()]
    if missing:
        raise RuntimeError(f"Java overlay source is missing: {missing}")
    result = subprocess.run(
        ["git", "-C", str(CLAVA_ROOT), "rev-parse", "HEAD"],
        capture_output=True, text=True, check=False,
    )
    if result.returncode:
        raise RuntimeError(f"cannot identify Clava source revision: {result.stderr.strip()}")
    status = subprocess.run(
        ["git", "-C", str(CLAVA_ROOT), "status", "--porcelain=v1", "-uall"],
        capture_output=True, text=True, check=False,
    )
    diff = subprocess.run(
        ["git", "-C", str(CLAVA_ROOT), "diff", "--binary", "HEAD"],
        capture_output=True, check=False,
    )
    if status.returncode or diff.returncode:
        raise RuntimeError("cannot fingerprint Clava source state")
    return {
        "clava_head": result.stdout.strip(),
        "clava_status": status.stdout.splitlines(),
        "clava_diff_sha256": sha256_bytes(diff.stdout),
        "benchmark_sources": {str(path.relative_to(CLAVA_ROOT)): sha256_file(path) for path in sources},
    }


def build_parser_config(event: dict[str, Any], condition: dict[str, Any], rootfs: Path,
                        dumper_folder: Path) -> dict[str, Any]:
    config = dict(event.get("parser_config") or {})
    if config.get("generated_parse_root"):
        config["generated_parse_root"] = map_path(config["generated_parse_root"], rootfs)
    config["dumper_folder"] = str(dumper_folder)
    config["show_exec_info"] = False
    config["ast_dump_cache"] = condition["cache_mode"] == "warmcache"
    return config


def parse_operation(event: dict[str, Any], staged: dict[str, Any], output_root: Path,
                    protocol: str, cache_mode: str, phase: str, repeat: int,
                    ) -> dict[str, Any]:
    condition = {"protocol": protocol, "cache_mode": cache_mode}
    state = output_root / "parser-state" / event["suite"] / protocol / cache_mode
    dumper_folder = state / "dumper"
    dumper_folder.mkdir(parents=True, exist_ok=True)
    options = remap_options(event["compiler_options"], staged["rootfs"])
    operation: dict[str, Any] = {
        "phase": phase, "suite": event["suite"], "input_id": input_id(event),
        "source_label": event_label(event), "event_id": event["event_id"],
        "source_sha256": event["source"]["sha256"], "args_sha256": event["args_sha256"],
        "options_sha256": semantic_options_hash(event),
        "protocol": protocol, "cache_mode": JAVA_CACHE_MODE[cache_mode],
        "source_path": str(staged["source_path"]), "replay_cwd": str(staged["replay_cwd"]),
        "generated_parse_root": str(staged["generated_parse_root"])
            if staged["generated_parse_root"] else None,
        "dumper_folder": str(dumper_folder), "compiler_options": options,
        "parser_config": build_parser_config(event, condition, staged["rootfs"], dumper_folder),
        "effective_libc_mode": event["effective_libc_mode"],
        "parse_id": event["parse_id"], "standard": event["standard"],
        "repeat": repeat,
    }
    if phase == "diagnostic":
        operation["expected_native_argv"] = normalized_native_argv(event, staged["rootfs"])
    if phase == "fidelity":
        operation["snapshot_path"] = str(
            output_root / "fidelity-snapshots" / event["suite"] / input_id(event) / f"{protocol}.json"
        )
    return operation


def ccache_operation(operation: str, event: dict[str, Any], output_root: Path,
                     protocol: str, repeat: int) -> dict[str, Any]:
    cache_dir = output_root / "parser-state" / event["suite"] / protocol / "warmcache" \
        / "dumper" / CCACHE_FOLDER
    return {
        "operation": operation, "cache_directory": str(cache_dir),
        "phase": "measure", "suite": event["suite"], "input_id": "__ccache__",
        "source_label": "ccache stats", "protocol": protocol,
        "cache_mode": "warm", "repeat": repeat,
    }


def pilot_schedule(events: list[dict[str, Any]], staged: dict[str, dict[str, Any]],
                   output_root: Path) -> list[dict[str, Any]]:
    selected = select_pilot_events(events)
    rows: list[dict[str, Any]] = []
    for cache_mode in CACHE_MODES:
        for protocol in PROTOCOLS:
            for event in selected:
                rows.append(parse_operation(event, staged[event["event_id"]], output_root,
                                            protocol, cache_mode, "warmup", 0))
    for cache_mode in CACHE_MODES:
        for protocol in PROTOCOLS:
            for event in selected:
                rows.append(parse_operation(event, staged[event["event_id"]], output_root,
                                            protocol, cache_mode, "diagnostic", 1))
            for suite in NATIVE_EVENTS:
                suite_selected = [event for event in selected if event["suite"] == suite]
                if cache_mode == "warmcache":
                    rows.append(ccache_operation("ccache_zero", suite_selected[0], output_root, protocol, 1))
                for event in suite_selected:
                    rows.append(parse_operation(
                        event, staged[event["event_id"]], output_root, protocol, cache_mode, "measure", 1,
                    ))
                if cache_mode == "warmcache":
                    rows.append(ccache_operation("ccache_stats", suite_selected[0], output_root, protocol, 1))
    for protocol in PROTOCOLS:
        for event in selected:
            if event["suite"] == "java" or event["suite"] == "clava-js":
                rows.append(parse_operation(event, staged[event["event_id"]], output_root,
                                            protocol, "directbypass", "fidelity", 1))
    return rows


def environment_for(manifest: dict[str, Any], native_tool: Path, scratch: Path,
                    require_ccache: bool) -> dict[str, str]:
    # The exact captured process variables are passed to each native invocation.
    # The Java launcher gets the same base values, except inherited GC/agent options.
    sample = manifest.get("_environment_template", {})
    environment = {key: str(value) for key, value in sample.items()}
    for key in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "CLAVA_AST_CORPUS_CAPTURE_DIR",
                "CLAVA_AST_CORPUS_SUITE", "CCACHE_DISABLE"):
        environment.pop(key, None)
    environment["CLANG_DUMPER_TOOL"] = str(native_tool)
    environment["TMPDIR"] = str(scratch)
    environment["TMP"] = str(scratch)
    environment["TEMP"] = str(scratch)
    environment["XDG_CACHE_HOME"] = str(scratch)
    path = environment.get("PATH", os.environ.get("PATH", ""))
    if require_ccache and shutil.which("ccache", path=path) is None:
        raise RuntimeError("warmcache mode requires ccache on the captured PATH")
    return environment


def compile_overlay(output_root: Path, runtime_root: Path) -> tuple[Path, str]:
    classes = output_root / "overlay-classes"
    classes.mkdir(parents=True, exist_ok=True)
    sources = [*PARSER_SOURCES, RUNNER_SOURCE]
    command = ["javac", "-cp", str(runtime_root / "lib" / "*"), "-d", str(classes),
               *map(str, sources)]
    result = subprocess.run(command, capture_output=True, text=True, check=False)
    if result.returncode:
        raise RuntimeError("Java overlay compilation failed:\n" + result.stdout + result.stderr)
    return classes, java_runner_class(RUNNER_SOURCE)


def java_command(classes: Path, runtime_root: Path, runner_class: str,
                 schedule: Path, output: Path) -> list[str]:
    classpath = os.pathsep.join((str(classes), str(runtime_root / "lib" / "*")))
    return ["java", "-cp", classpath, runner_class,
            "--schedule", str(schedule), "--output", str(output)]


def parse_ccache_stats(rows: list[dict[str, Any]]) -> dict[str, int]:
    output = "\n".join(str(row.get("output", "")) for row in rows if row.get("record_type") == "ccache")
    labels = {
        "ccache_cacheable_calls": r"Cacheable calls:",
        "ccache_hits": r"Hits:",
        "ccache_misses": r"Misses:",
        "ccache_uncacheable_calls": r"Uncacheable calls:",
    }
    result: dict[str, int] = {}
    for field, label in labels.items():
        match = re.search(rf"^\s*{label}\s*([\d,]+)", output, re.MULTILINE)
        if match is None:
            raise RuntimeError(f"ccache stats output lacks {label!r}: {output[:1200]}")
        result[field] = int(match.group(1).replace(",", ""))
    return result


def row_identity_matches(operation: dict[str, Any], result: dict[str, Any]) -> bool:
    for key in ("phase", "suite", "input_id", "source_label", "event_id", "protocol", "cache_mode", "repeat"):
        if key in operation and result.get(key) != operation[key]:
            return False
    if operation.get("operation"):
        return result.get("record_type") == "ccache" and result.get("valid") is True
    return result.get("event_id") == operation["event_id"]


def run_schedule(output_root: Path, schedule_rows: list[dict[str, Any]], manifest: dict[str, Any],
                 corpus_events: list[dict[str, Any]], classes: Path, runtime_root: Path,
                 runner_class: str, native_tool: Path, name: str) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    schedule_path = output_root / "schedules" / f"{name}.jsonl"
    result_path = output_root / "runner-output" / f"{name}.jsonl"
    log_path = output_root / "logs" / f"{name}.log"
    write_jsonl(schedule_path, schedule_rows)
    env = environment_for(manifest, native_tool, output_root / "scratch" / name,
                          require_ccache=any(row.get("cache_mode") == "warm" for row in schedule_rows))
    Path(env["TMPDIR"]).mkdir(parents=True, exist_ok=True)
    command = java_command(classes, runtime_root, runner_class, schedule_path, result_path)
    with log_path.open("w", encoding="utf-8") as log:
        completed = subprocess.run(command, cwd=CLAVA_ROOT, env=env,
                                   stdout=log, stderr=subprocess.STDOUT, check=False)
    results = read_jsonl(result_path) if result_path.is_file() else []
    if len(results) != len(schedule_rows):
        raise RuntimeError(f"{name}: runner returned {len(results)} rows for {len(schedule_rows)} operations")
    identity_valid = all(row_identity_matches(operation, result)
                         for operation, result in zip(schedule_rows, results))
    summary = {
        "name": name, "command": command, "schedule": str(schedule_path),
        "schedule_sha256": sha256_file(schedule_path), "output": str(result_path),
        "output_sha256": sha256_file(result_path), "log": str(log_path),
        "return_code": completed.returncode, "operation_count": len(schedule_rows),
        "identity_valid": identity_valid,
    }
    if completed.returncode != 0 or not identity_valid:
        raise RuntimeError(f"{name}: Java runner failed or output identity mismatched; see {log_path}")
    return results, summary


def build_full_batch(events: list[dict[str, Any]], staged: dict[str, dict[str, Any]], output_root: Path,
                     protocol: str, cache_mode: str, phase: str, repeat: int) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    if phase in ("diagnostic", "measure"):
        warmup_ids = {event["event_id"] for event in warmup_events_for_suite(events)}
        for event in events:
            if event["event_id"] in warmup_ids:
                rows.append(parse_operation(event, staged[event["event_id"]], output_root,
                                            protocol, cache_mode, "warmup", repeat))
    if phase == "measure" and cache_mode == "warmcache":
        rows.append(ccache_operation("ccache_zero", events[0], output_root, protocol, repeat))
    for event in events:
        rows.append(parse_operation(event, staged[event["event_id"]], output_root,
                                    protocol, cache_mode, phase, repeat))
    if phase == "measure" and cache_mode == "warmcache":
        rows.append(ccache_operation("ccache_stats", events[0], output_root, protocol, repeat))
    return rows


def warmup_events_for_suite(events: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """Pick a small same-JVM warmup that covers the suite's parser paths."""
    selected: list[dict[str, Any]] = []
    predicates = (
        lambda e: Path(e["source"]["original_path"]).suffix.lower() == ".c",
        lambda e: Path(e["source"]["original_path"]).suffix.lower() == ".cpp",
        lambda e: Path(e["source"]["original_path"]).suffix.lower() in (".h", ".hpp"),
        lambda e: Path(e["source"]["original_path"]).suffix.lower() == ".cl",
        lambda e: e.get("parse_pass") == "roundtrip",
    )
    used: set[str] = set()
    for predicate in predicates:
        event = next((candidate for candidate in events
                      if candidate["event_id"] not in used and predicate(candidate)), None)
        if event is not None:
            selected.append(event)
            used.add(event["event_id"])
    return selected


def metric_for(result: dict[str, Any]) -> dict[str, Any] | None:
    metrics = result.get("metrics")
    if not isinstance(metrics, list) or len(metrics) != 1 or not isinstance(metrics[0], dict):
        return None
    return metrics[0]


def validate_batch(schedule_rows: list[dict[str, Any]], results: list[dict[str, Any]],
                   suite_events: list[dict[str, Any]], phase: str) -> tuple[list[dict[str, Any]], list[str]]:
    parse_ops = [row for row in schedule_rows if row.get("operation") is None and row.get("phase") == phase]
    parse_results = [row for row in results if row.get("record_type") != "ccache" and row.get("phase") == phase]
    errors: list[str] = []
    expected = {row["event_id"]: row for row in suite_events}
    got = collections.Counter(row.get("event_id") for row in parse_results)
    if got != collections.Counter(expected):
        errors.append(f"{phase}: parse-event coverage mismatch {sum(got.values())}/{len(expected)}")
    rows_by_id = {row["event_id"]: row for row in parse_results}
    if len(rows_by_id) != len(parse_results):
        errors.append(f"{phase}: duplicate output event IDs")
    for operation in parse_ops:
        result = rows_by_id.get(operation["event_id"])
        if result is None:
            continue
        if result.get("valid") is not True:
            errors.append(f"{phase}: invalid event {operation['input_id']}: {result.get('error') or result.get('validity_reason')}")
        if result.get("source_sha256") != operation["source_sha256"]:
            errors.append(f"{phase}: source identity mismatch for {operation['input_id']}")
        if result.get("args_sha256") != operation["args_sha256"]:
            errors.append(f"{phase}: args identity mismatch for {operation['input_id']}")
    return parse_results, errors


def measured_row(event: dict[str, Any], result: dict[str, Any], protocol: str,
                 cache_mode: str, repeat: int) -> dict[str, Any]:
    fields = (
        "generated_code_sha256", "generated_code_bytes", "ast_root_kind", "ast_node_count",
        "ast_node_kind_counts_sha256",
    )
    return {
        "suite": event["suite"], "input_id": input_id(event), "source_label": event_label(event),
        "protocol": protocol, "cache_mode": cache_mode, "repeat": repeat,
        "elapsed_ms": result.get("elapsed_ms"), "source_sha256": event["source"]["sha256"],
        "args_sha256": event["args_sha256"], "valid": bool(result.get("valid")),
        "phase": "measured", "event_id": event["event_id"],
        "options_sha256": semantic_options_hash(event), "parse_pass": event.get("parse_pass"),
        "test_id": event.get("test_id"), "resource_key": event.get("resource_key"),
        **{field: result.get(field) for field in fields},
        "validity_reason": result.get("validity_reason") or result.get("error") or "valid",
    }


def diagnostic_row(event: dict[str, Any], result: dict[str, Any], protocol: str,
                   cache_mode: str) -> dict[str, Any]:
    metric = metric_for(result) or {}
    byte_count = metric.get("encoded_bytes")
    if byte_count is None:
        byte_count = metric.get("dump_bytes")
    return {
        "suite": event["suite"], "input_id": input_id(event), "source_label": event_label(event),
        "protocol": protocol, "cache_mode": cache_mode, "repeat": 0,
        "native_ms": metric.get("native_ms"), "ccache_invoke_ms": metric.get("ccache_invoke_ms"),
        "read_ms": metric.get("read_ms"), "ast_ms": metric.get("ast_ms"),
        "records": metric.get("records"), "nodes": metric.get("nodes"), "bytes": byte_count,
        "valid": bool(result.get("valid")) and metric_for(result) is not None,
        "phase": "diagnostic", "event_id": event["event_id"],
        "source_sha256": event["source"]["sha256"], "args_sha256": event["args_sha256"],
        "options_sha256": semantic_options_hash(event),
        "cached": metric.get("cached"),
        "metrics_json": json.dumps(metric, sort_keys=True, separators=(",", ":")),
        "validity_reason": result.get("validity_reason") or result.get("error") or "valid",
    }


def validate_source_pairs(events: list[dict[str, Any]]) -> list[str]:
    errors = []
    for field in ("source_sha256", "args_sha256", "options_sha256"):
        for event in events:
            expected = event["source"]["sha256"] if field == "source_sha256" \
                else event["args_sha256"] if field == "args_sha256" else semantic_options_hash(event)
            if not expected:
                errors.append(f"missing {field} for event {event['event_id']}")
    return errors


def compare_fidelity(rows: list[dict[str, Any]]) -> tuple[dict[str, Any], list[str]]:
    fields = (
        "generated_code_sha256", "generated_code_bytes", "ast_root_kind", "ast_node_count",
        "ast_node_kind_counts_sha256",
    )
    grouped: dict[tuple[str, str], dict[str, dict[str, Any]]] = collections.defaultdict(dict)
    for row in rows:
        if row.get("cache_mode") == "directbypass":
            grouped[(row["suite"], row["input_id"])][row["protocol"]] = row
    errors: list[str] = []
    compared = 0
    for key, protocols in grouped.items():
        if set(protocols) != set(PROTOCOLS):
            errors.append(f"fidelity pair missing protocol for {key}")
            continue
        left, right = (protocols[name] for name in PROTOCOLS)
        compared += 1
        for field in fields:
            if left.get(field) is None or right.get(field) is None:
                errors.append(f"fidelity field {field} absent for {key}")
            elif left[field] != right[field]:
                errors.append(f"fidelity mismatch {field} for {key}: {left[field]} != {right[field]}")
    return {"compared_input_pairs": compared, "fields": list(fields), "valid": not errors}, errors


def fidelity_row(event: dict[str, Any], result: dict[str, Any], protocol: str) -> dict[str, Any]:
    fields = (
        "generated_code_sha256", "generated_code_bytes", "ast_root_kind", "ast_node_count",
        "ast_node_kind_counts_sha256",
    )
    return {
        "suite": event["suite"], "input_id": input_id(event), "source_label": event_label(event),
        "protocol": protocol, "cache_mode": "directbypass", "event_id": event["event_id"],
        "source_sha256": event["source"]["sha256"], "args_sha256": event["args_sha256"],
        "options_sha256": semantic_options_hash(event),
        **{field: result.get(field) for field in fields},
        "valid": bool(result.get("valid")),
        "validity_reason": result.get("validity_reason") or result.get("error") or "valid",
    }


def verify_pilot_validation(pilot_root: Path, corpus_sha256: str) -> dict[str, Any]:
    plan_path = pilot_root / "pilot_plan.json"
    schedule_path = pilot_root / "pilot_schedule.jsonl"
    output_path = pilot_root / "pilot_output.jsonl"
    log_path = pilot_root / "pilot-process.log"
    for path in (plan_path, schedule_path, output_path, log_path):
        if not path.is_file():
            raise RuntimeError(f"pilot validation evidence is missing: {path}")
    plan = json.loads(plan_path.read_text(encoding="utf-8"))
    if plan.get("corpus_manifest_sha256") != corpus_sha256:
        raise RuntimeError("pilot used a different corpus manifest")
    if sha256_file(schedule_path) != plan.get("schedule_sha256"):
        raise RuntimeError("pilot schedule hash does not match its plan")
    schedule = read_jsonl(schedule_path)
    output = read_jsonl(output_path)
    if len(schedule) != 78 or len(output) != len(schedule):
        raise RuntimeError("pilot operation count is not the expected 78")
    phases = collections.Counter(row.get("phase") for row in schedule)
    expected_phases = {"warmup": 20, "diagnostic": 20, "measure": 28, "fidelity": 10}
    if dict(phases) != expected_phases:
        raise RuntimeError(f"pilot phase counts differ: {dict(phases)}")
    if any(row.get("valid") is not True for row in output):
        raise RuntimeError("pilot has invalid Java runner rows")
    if any(row.get("phase") == "diagnostic"
           and not isinstance((metric_for(row) or {}).get("native_argv_debug"), list) for row in output):
        raise RuntimeError("pilot diagnostics lack actual native argv parity evidence")
    fidelity_rows = []
    for operation, result in zip(schedule, output):
        if operation.get("phase") == "fidelity":
            fidelity_rows.append({**operation, **result})
    fidelity, fidelity_errors = compare_fidelity(fidelity_rows)
    if fidelity_errors or fidelity["compared_input_pairs"] != 5:
        raise RuntimeError(f"pilot fidelity gate failed: {fidelity_errors}")
    diagnostic_groups: dict[tuple[str, str], list[dict[str, Any]]] = collections.defaultdict(list)
    for row in output:
        if row.get("phase") == "diagnostic":
            diagnostic_groups[(row["suite"], row["protocol"], row["cache_mode"])].append(metric_for(row) or {})
    warm_cache_hits = {}
    for suite in NATIVE_EVENTS:
        for protocol in PROTOCOLS:
            warm = diagnostic_groups[(suite, protocol, "warm")]
            eligible = [metric for metric in warm if metric.get("ccache_cache_dir")]
            hits = sum(metric.get("cached") is True for metric in eligible)
            if not eligible or hits != len(eligible):
                raise RuntimeError(f"pilot warm cache did not hit every eligible sample for {suite}/{protocol}")
            bypass = diagnostic_groups[(suite, protocol, "bypass")]
            if any(metric.get("ccache_cache_dir") or metric.get("cached") is True for metric in bypass):
                raise RuntimeError(f"pilot bypass invoked the AST ccache for {suite}/{protocol}")
            warm_cache_hits[f"{suite}/{protocol}"] = {"eligible": len(eligible), "hits": hits}
    log = log_path.read_text(encoding="utf-8", errors="replace")
    if "System.gc" in log or "System.gc()" in log:
        raise RuntimeError("pilot GC log contains an explicit System.gc cause")
    command = plan.get("java_command_template", [])
    if any("DisableExplicitGC" in str(arg) for arg in command):
        raise RuntimeError("pilot command contains an explicit-GC policy flag")
    return {
        "valid": True, "schedule_sha256": sha256_file(schedule_path),
        "output_sha256": sha256_file(output_path), "process_log_sha256": sha256_file(log_path),
        "operation_count": len(output), "phase_counts": dict(phases),
        "fidelity_pairs": fidelity["compared_input_pairs"],
        "argv_diagnostic_rows": phases["diagnostic"], "warm_cache_hits": warm_cache_hits,
        "explicit_gc_cause_rows": 0,
    }


def validate_ccache_pair(stats: dict[tuple[str, str, int], dict[str, int]],
                         adapter_counts: dict[tuple[str, str, int], int]) -> tuple[dict[str, Any], list[str]]:
    errors: list[str] = []
    pairs = 0
    for suite in NATIVE_EVENTS:
        for repeat in (1, 2, 3):
            text = stats.get((suite, "text", repeat))
            proto = stats.get((suite, "protobuf", repeat))
            if text is None or proto is None:
                errors.append(f"missing warm ccache stats for {suite} repeat {repeat}")
                continue
            pairs += 1
            for field in ("ccache_cacheable_calls", "ccache_hits", "ccache_misses", "ccache_uncacheable_calls"):
                if text[field] != proto[field]:
                    errors.append(f"paired warm ccache {field} mismatch for {suite} repeat {repeat}")
            for protocol, value in (("text", text), ("protobuf", proto)):
                key = (suite, protocol, repeat)
                expected_calls = value["ccache_cacheable_calls"] + value["ccache_uncacheable_calls"]
                if expected_calls != adapter_counts.get(key, -1):
                    errors.append(f"ccache call count mismatch for {key}: {expected_calls} != {adapter_counts.get(key)}")
                if value["ccache_misses"] != 0 or value["ccache_hits"] != value["ccache_cacheable_calls"]:
                    errors.append(f"warm cache miss or non-hit cacheable call for {key}")
    return {"paired_cache_distributions": pairs, "valid": not errors}, errors


def run_full(output_root: Path, corpus_root: Path, manifest: dict[str, Any],
             events: list[dict[str, Any]], manifest_sha: str,
             pilot_root: Path) -> int:
    source_start = source_fingerprint()
    pilot_validation = verify_pilot_validation(pilot_root, manifest_sha)
    runtime_root = Path(manifest["runtime"]["root"])
    native_tool = Path(manifest["runtime"]["native_tool"])
    overlay, runner_class = compile_overlay(output_root, runtime_root)
    write_json(output_root / "source-fingerprint.json", source_start)
    staging_root = output_root / "inputs"
    staged = {event["event_id"]: materialize_event(corpus_root, event, staging_root) for event in events}
    staged_rows = [file_row for event in events for file_row in staged[event["event_id"]]["files"]]
    write_jsonl(output_root / "staged-inputs.jsonl", staged_rows)
    write_json(output_root / "plan.json", {
        "experiment": "standalone per-source Text and Protobuf A/B",
        "created_at": dt.datetime.now(dt.timezone.utc).isoformat(),
        "corpus_root": str(corpus_root), "corpus_manifest_sha256": manifest_sha,
        "corpus_replay_events_sha256": manifest["hashes"]["replay_events.jsonl"],
        "suite_event_counts": NATIVE_EVENTS, "protocols": PROTOCOLS,
        "cache_modes": CACHE_MODES, "repeats": [1, 2, 3],
        "timing_boundary": "outer CodeParser.parse(List.of(source), compiler_options) elapsed time; phase logging is disabled, though intrinsic reader clocks remain unexported",
        "primary_phase": "measure", "diagnostic_phase": "diagnostic",
        "compression": False, "show_exec_info": False, "explicit_gc_policy_flags": [],
        "fidelity_phase": "untimed direct-bypass after all primary measurements",
        "fidelity_rows_expected": 876,
        "pilot_validation": pilot_validation,
        "source_fingerprint": source_start,
        "native_tool_sha256": manifest["runtime"]["native_tool_sha256"],
        "runtime_manifest": manifest["runtime"]["manifest"],
        "staged_input_path_count": len(staged_rows),
        "unique_staged_path_sha256_pairs": len({(row["original_path"], row["sha256"]) for row in staged_rows}),
        "run_directory": str(output_root),
    })

    measured: list[dict[str, Any]] = []
    diagnostics: list[dict[str, Any]] = []
    fidelity_rows: list[dict[str, Any]] = []
    runs: list[dict[str, Any]] = []
    cache_stats: dict[tuple[str, str, int], dict[str, int]] = {}
    adapter_counts: dict[tuple[str, str, int], int] = {}
    all_errors: list[str] = []

    # Seed ccache once for each suite/protocol. Later batches warm the JVM in
    # the same process immediately before diagnostics or measured rows.
    for suite in NATIVE_EVENTS:
        suite_events = [event for event in events if event["suite"] == suite]
        for protocol in PROTOCOLS:
            rows = [parse_operation(event, staged[event["event_id"]], output_root,
                                    protocol, "warmcache", "warmup", 0)
                    for event in suite_events]
            output, meta = run_schedule(output_root, rows, manifest, events, overlay,
                                        runtime_root, runner_class, native_tool,
                                        f"cache-seed-{suite}-{protocol}")
            _, errors = validate_batch(rows, output, suite_events, "warmup")
            all_errors.extend(errors)
            runs.append({"suite": suite, "protocol": protocol, "cache_mode": "warmcache",
                         "phase": "cache-seed", "repeat": 0, "return_code": meta["return_code"],
                         "input_count": len(suite_events), "valid_count": len(suite_events) - len(errors),
                         "elapsed_ms_sum": None, **{k: meta[k] for k in ("schedule", "output", "log")},
                         "valid": not errors})
            print(f"cache seed {suite} {protocol}: valid={not errors}", flush=True)
            if errors:
                break
        if all_errors:
            break

    if not all_errors:
        for suite in NATIVE_EVENTS:
            suite_events = [event for event in events if event["suite"] == suite]
            for cache_mode in CACHE_MODES:
                for protocol in PROTOCOLS:
                    rows = build_full_batch(suite_events, staged, output_root,
                                            protocol, cache_mode, "diagnostic", 0)
                    output, meta = run_schedule(output_root, rows, manifest, events, overlay,
                                                runtime_root, runner_class, native_tool,
                                                f"diagnostic-{suite}-{protocol}-{cache_mode}")
                    parse_rows, errors = validate_batch(rows, output, suite_events, "diagnostic")
                    all_errors.extend(errors)
                    for event, result in zip(suite_events, parse_rows):
                        diagnostics.append(diagnostic_row(event, result, protocol, cache_mode))
                    if any(not row["valid"] for row in diagnostics[-len(suite_events):]):
                        all_errors.append(f"diagnostic metric invalid for {suite} {protocol} {cache_mode}")
                    if any(not (metric_for(result) or {}).get("native_argv_debug")
                           or result.get("argv_match") is not True for result in parse_rows):
                        all_errors.append(f"diagnostic argv parity incomplete for {suite} {protocol} {cache_mode}")
                    if any(result.get("compression") is not False
                           or result.get("show_exec_info") is not False for result in parse_rows):
                        all_errors.append(f"diagnostic runtime flags differ for {suite} {protocol} {cache_mode}")
                    metrics = [metric_for(result) or {} for result in parse_rows]
                    if cache_mode == "warmcache":
                        eligible = [metric for metric in metrics if metric.get("ccache_cache_dir")]
                        if any(metric.get("cached") is not True for metric in eligible):
                            all_errors.append(f"warm diagnostic had an eligible cache miss for {suite} {protocol}")
                    elif any(metric.get("ccache_cache_dir") or metric.get("cached") is True for metric in metrics):
                        all_errors.append(f"bypass diagnostic invoked the AST ccache for {suite} {protocol}")
                    if cache_mode == "warmcache":
                        key = (suite, protocol, 0)
                        adapter_counts[key] = sum(
                            bool((metric_for(result) or {}).get("ccache_cache_dir")) for result in parse_rows
                        )
                    runs.append({"suite": suite, "protocol": protocol, "cache_mode": cache_mode,
                                 "phase": "diagnostic", "repeat": 0, "return_code": meta["return_code"],
                                 "input_count": len(parse_rows), "valid_count": sum(r.get("valid") is True for r in parse_rows),
                                 "elapsed_ms_sum": None, **{k: meta[k] for k in ("schedule", "output", "log")},
                                 "valid": not errors})
                    print(f"diagnostic {suite} {protocol} {cache_mode}: valid={not errors}", flush=True)
                    if errors:
                        break
                if all_errors:
                    break
            if all_errors:
                break

    if not all_errors:
        for repeat in (1, 2, 3):
            protocol_order = PROTOCOLS if repeat % 2 else tuple(reversed(PROTOCOLS))
            cache_order = CACHE_MODES if repeat % 2 else tuple(reversed(CACHE_MODES))
            for suite in NATIVE_EVENTS:
                suite_events = [event for event in events if event["suite"] == suite]
                for cache_mode in cache_order:
                    for protocol in protocol_order:
                        rows = build_full_batch(suite_events, staged, output_root,
                                                protocol, cache_mode, "measure", repeat)
                        output, meta = run_schedule(output_root, rows, manifest, events, overlay,
                                                    runtime_root, runner_class, native_tool,
                                                    f"measure-{suite}-{protocol}-{cache_mode}-r{repeat}")
                        parse_rows, errors = validate_batch(rows, output, suite_events, "measure")
                        all_errors.extend(errors)
                        result_by_id = {row["event_id"]: row for row in parse_rows}
                        condition_rows = [measured_row(event, result_by_id[event["event_id"]],
                                                       protocol, cache_mode, repeat)
                                          for event in suite_events if event["event_id"] in result_by_id]
                        if len(condition_rows) != len(suite_events):
                            errors.append(f"measured coverage incomplete for {suite} {protocol} {cache_mode} r{repeat}")
                        if cache_mode == "warmcache":
                            stats = parse_ccache_stats(output)
                            key = (suite, protocol, repeat)
                            cache_stats[key] = stats
                            adapter_counts[key] = adapter_counts.get((suite, protocol, 0), 0)
                            counts = {
                                **stats,
                                "ccache_adapter_events": adapter_counts.get((suite, protocol, 0), 0),
                            }
                            counts["ccache_counters_match"] = (
                                stats["ccache_cacheable_calls"] + stats["ccache_uncacheable_calls"]
                                == counts["ccache_adapter_events"]
                            )
                            if not counts["ccache_counters_match"]:
                                errors.append(f"ccache adapter/stat count mismatch for {key}")
                            if (stats["ccache_misses"] != 0
                                    or stats["ccache_hits"] != stats["ccache_cacheable_calls"]):
                                errors.append(f"warm cache had measured misses or non-hit cacheable calls for {key}")
                        else:
                            counts = {"ccache_cacheable_calls": 0, "ccache_hits": 0,
                                      "ccache_misses": 0, "ccache_uncacheable_calls": 0,
                                      "ccache_adapter_events": 0, "ccache_counters_match": True}
                        measured.extend(condition_rows)
                        runs.append({"suite": suite, "protocol": protocol, "cache_mode": cache_mode,
                                     "phase": "measured", "repeat": repeat,
                                     "return_code": meta["return_code"], "input_count": len(condition_rows),
                                     "valid_count": sum(row["valid"] for row in condition_rows),
                                     "elapsed_ms_sum": sum(float(row["elapsed_ms"] or 0) for row in condition_rows),
                                     **counts, **{k: meta[k] for k in ("schedule", "output", "log")},
                                     "valid": not errors})
                        write_csv(output_root / "measured.csv", MEASURE_FIELDS, measured)
                        write_csv(output_root / "diagnostics.csv", DIAGNOSTIC_FIELDS, diagnostics)
                        write_csv(output_root / "runs.csv", RUN_FIELDS, runs)
                        print(f"measure {suite} {protocol} {cache_mode} r{repeat}: "
                              f"n={len(condition_rows)} valid={not errors}", flush=True)
                        if errors:
                            all_errors.extend(errors)
                            break
                    if all_errors:
                        break
                if all_errors:
                    break
            if all_errors:
                break

    if not all_errors:
        for suite in NATIVE_EVENTS:
            suite_events = [event for event in events if event["suite"] == suite]
            for protocol in PROTOCOLS:
                rows = [parse_operation(event, staged[event["event_id"]], output_root,
                                        protocol, "directbypass", "fidelity", 0)
                        for event in suite_events]
                output, meta = run_schedule(output_root, rows, manifest, events, overlay,
                                            runtime_root, runner_class, native_tool,
                                            f"fidelity-{suite}-{protocol}")
                result_by_id = {row.get("event_id"): row for row in output}
                condition = [fidelity_row(event, result_by_id[event["event_id"]], protocol)
                             for event in suite_events if event["event_id"] in result_by_id]
                if len(condition) != len(suite_events):
                    all_errors.append(f"fidelity coverage incomplete for {suite} {protocol}")
                if any(not row["valid"] for row in condition):
                    all_errors.append(f"invalid fidelity rows for {suite} {protocol}")
                fidelity_rows.extend(condition)
                runs.append({"suite": suite, "protocol": protocol, "cache_mode": "directbypass",
                             "phase": "fidelity", "repeat": 0, "return_code": meta["return_code"],
                             "input_count": len(condition), "valid_count": sum(row["valid"] for row in condition),
                             "elapsed_ms_sum": None, **{k: meta[k] for k in ("schedule", "output", "log")},
                             "valid": len(condition) == len(suite_events) and all(row["valid"] for row in condition)})
                print(f"fidelity {suite} {protocol}: valid={not any(not row['valid'] for row in condition)}", flush=True)
                if all_errors:
                    break
            if all_errors:
                break

    fidelity, fidelity_errors = compare_fidelity(fidelity_rows)
    all_errors.extend(fidelity_errors)
    if len(measured) != sum(NATIVE_EVENTS.values()) * len(PROTOCOLS) * len(CACHE_MODES) * 3:
        all_errors.append(f"measured row count {len(measured)} != 5256")
    expected_diagnostic = sum(NATIVE_EVENTS.values()) * len(PROTOCOLS) * len(CACHE_MODES)
    if len(diagnostics) != expected_diagnostic:
        all_errors.append(f"diagnostic row count {len(diagnostics)} != {expected_diagnostic}")
    if len(fidelity_rows) != sum(NATIVE_EVENTS.values()) * len(PROTOCOLS):
        all_errors.append(f"fidelity row count {len(fidelity_rows)} != 876")
    if fidelity["compared_input_pairs"] != sum(NATIVE_EVENTS.values()):
        all_errors.append(f"fidelity pair count {fidelity['compared_input_pairs']} != 438")
    ccache_comparison, ccache_errors = validate_ccache_pair(cache_stats, adapter_counts)
    all_errors.extend(ccache_errors)

    # All per-event roots are checked after execution. Hardlinked content must
    # remain byte-identical to the immutable content-addressed corpus.
    materialized_changes = []
    for row in staged_rows:
        path = Path(row["replay_path"])
        if not path.is_file() or sha256_file(path) != row["sha256"]:
            materialized_changes.append(str(path))
    if materialized_changes:
        all_errors.append(f"{len(materialized_changes)} staged input/dependency files changed during replay")
    try:
        end_manifest, end_events, end_manifest_sha = verify_corpus(corpus_root)
        if end_manifest_sha != manifest_sha or len(end_events) != len(events):
            all_errors.append("frozen corpus manifest changed during replay")
    except Exception as error:
        all_errors.append(f"frozen corpus failed post-run verification: {error}")
    if source_fingerprint() != source_start:
        all_errors.append("parser sources or checkout state changed during replay")

    write_csv(output_root / "measured.csv", MEASURE_FIELDS, measured)
    write_csv(output_root / "diagnostics.csv", DIAGNOSTIC_FIELDS, diagnostics)
    write_csv(output_root / "fidelity.csv", FIDELITY_FIELDS, fidelity_rows)
    write_csv(output_root / "runs.csv", RUN_FIELDS, runs)
    results = {
        "complete": not all_errors, "valid": not all_errors,
        "measured_rows": len(measured), "diagnostic_rows": len(diagnostics),
        "fidelity_rows": len(fidelity_rows), "expected_fidelity_rows": 876,
        "expected_measured_rows": 5256, "expected_diagnostic_rows": expected_diagnostic,
        "suite_event_counts": NATIVE_EVENTS, "fidelity": fidelity,
        "ccache_comparison": ccache_comparison, "errors": all_errors,
        "corpus_manifest_sha256": manifest_sha,
        "source_fingerprint_unchanged": source_fingerprint() == source_start,
        "materialized_inputs_unchanged": not materialized_changes,
    }
    write_json(output_root / "results.json", results)
    write_json(output_root / "run-metadata.json", {
        "plan": json.loads((output_root / "plan.json").read_text(encoding="utf-8")),
        "results": results, "runs": runs, "pilot_validation": pilot_validation,
    })
    return 0 if not all_errors else 1


def prepare_pilot(output_root: Path, corpus_root: Path, manifest: dict[str, Any],
                  events: list[dict[str, Any]], manifest_sha: str) -> int:
    staged: dict[str, dict[str, Any]] = {}
    selected = select_pilot_events(events)
    for event in selected:
        staged[event["event_id"]] = materialize_event(corpus_root, event, output_root / "inputs")
    rows = pilot_schedule(events, staged, output_root)
    schedule = output_root / "pilot_schedule.jsonl"
    write_jsonl(schedule, rows)
    runtime_root = Path(manifest["runtime"]["root"])
    native_tool = Path(manifest["runtime"]["native_tool"])
    class_name = java_runner_class(RUNNER_SOURCE)
    command = java_command(Path("<overlay-classes>"), runtime_root, class_name,
                           schedule, output_root / "pilot_output.jsonl")
    command[0:1] = ["java", "-Xlog:gc"]
    plan = {
        "kind": "pilot schedule only; this script does not start Java",
        "created_at": dt.datetime.now(dt.timezone.utc).isoformat(),
        "corpus_manifest_sha256": manifest_sha,
        "selected_categories": ["c", "c++", "opencl", "header", "generated-roundtrip"],
        "selected_events": [{"input_id": input_id(event), "event_id": event["event_id"],
                             "source_label": event_label(event), "source_sha256": event["source"]["sha256"],
                             "parse_pass": event.get("parse_pass")} for event in selected],
        "schedule_rows": len(rows), "schedule_sha256": sha256_file(schedule),
        "java_command_template": command,
        "native_tool": str(native_tool), "native_tool_sha256": manifest["runtime"]["native_tool_sha256"],
        "gc_policy": "no explicit GC flags; -Xlog:gc is observational only",
    }
    write_json(output_root / "pilot_plan.json", plan)
    write_jsonl(output_root / "staged-inputs.jsonl",
                (row for event in selected for row in staged[event["event_id"]]["files"]))
    write_json(output_root / "pilot_command.json", {"argv": command, "cwd": str(CLAVA_ROOT),
                "environment": {"CLANG_DUMPER_TOOL": str(native_tool),
                                "JAVA_TOOL_OPTIONS": "unset", "explicit_gc_policy_flags": []}})
    print(json.dumps({"pilot_schedule": str(schedule), "rows": len(rows),
                      "pilot_plan": str(output_root / "pilot_plan.json"),
                      "command": command}, indent=2))
    return 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    mode = parser.add_mutually_exclusive_group(required=True)
    mode.add_argument("--pilot", action="store_true", help="materialize five inputs and write Java schedule only")
    mode.add_argument("--run", action="store_true", help="run full standalone benchmark, after pilot approval")
    parser.add_argument("--corpus-root", type=Path, default=DEFAULT_CORPUS)
    parser.add_argument("--output-root", type=Path)
    parser.add_argument("--pilot-validation-dir", type=Path,
                        default=SCRIPT_ROOT / "results" / "pilot-20260930-03")
    return parser.parse_args()


def default_output(pilot: bool) -> Path:
    timestamp = dt.datetime.now().strftime("%Y%m%d-%H%M%S")
    return SCRIPT_ROOT / "results" / f"{'pilot' if pilot else 'ab'}-{timestamp}"


def main() -> int:
    args = parse_args()
    corpus_root = args.corpus_root.resolve()
    output_root = (args.output_root or default_output(args.pilot)).resolve()
    if output_root.exists():
        raise SystemExit(f"output already exists; refusing overwrite: {output_root}")
    if not (corpus_root / "corpus.json").is_file():
        raise SystemExit(f"missing frozen corpus manifest: {corpus_root / 'corpus.json'}")
    manifest, events, manifest_sha = verify_corpus(corpus_root)
    manifest["_environment_template"] = events[0].get("effective_environment", {})
    output_root.mkdir(parents=True)
    if args.pilot:
        return prepare_pilot(output_root, corpus_root, manifest, events, manifest_sha)
    return run_full(output_root, corpus_root, manifest, events, manifest_sha,
                    args.pilot_validation_dir.resolve())


if __name__ == "__main__":
    raise SystemExit(main())

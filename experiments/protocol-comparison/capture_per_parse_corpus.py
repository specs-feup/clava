#!/usr/bin/env python3
"""Capture the Clava-JS and Java parser inputs for standalone replay.

This runs each existing suite once only to discover its source/configuration
events. It does not collect or publish performance measurements.
"""

from __future__ import annotations

import argparse
from collections import Counter, defaultdict
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import shlex
import shutil
import subprocess
import sys
from typing import Any

sys.dont_write_bytecode = True

import run_comparison as base
import run_dual_ab as ab


SCRIPT_ROOT = Path(__file__).resolve().parent
CLAVA_ROOT = SCRIPT_ROOT.parents[1]
DEFAULT_REFERENCE = CLAVA_ROOT / "experiments/protocol-comparison/results/per-parse-ab-20260929-final8"
DEFAULT_RUNTIME_TEMPLATE = CLAVA_ROOT / "experiments/protocol-comparison/results/per-parse-preflight-20260929-final8/runtime/java-binaries"
EXPECTED_NATIVE_EVENTS = {"clava-js": 191, "java": 247}
EXPECTED_TEST_COUNTS = ab.EXPECTED_SUITE_COUNTS
CAPTURE_ENV = "CLAVA_AST_CORPUS_CAPTURE_DIR"
SUITE_ENV = "CLAVA_AST_CORPUS_SUITE"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--clava-root", type=Path, default=CLAVA_ROOT)
    parser.add_argument("--native-tool", type=Path, default=ab.DEFAULT_NATIVE_TOOL)
    parser.add_argument("--js-workspace", type=Path, default=ab.DEFAULT_JS_WORKSPACE)
    parser.add_argument("--runtime-template", type=Path, default=DEFAULT_RUNTIME_TEMPLATE,
                        help="existing Java runtime directory whose parser JAR will be replaced")
    parser.add_argument("--reference-results", type=Path, default=DEFAULT_REFERENCE,
                        help="final8 parse rows used to verify captured source multiplicities")
    parser.add_argument("--output-root", type=Path,
                        help="new corpus directory; defaults under protocol-comparison/results")
    parser.add_argument("--suite", choices=("all", "clava-js", "java"), default="all")
    return parser.parse_args()


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def sha256_json(value: Any) -> str:
    encoded = json.dumps(value, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
    return hashlib.sha256(encoded).hexdigest()


def new_output_root(requested: Path | None) -> Path:
    if requested is None:
        stamp = dt.datetime.now(dt.timezone.utc).strftime("%Y%m%dT%H%M%S%fZ")
        requested = SCRIPT_ROOT / "results" / f"standalone-corpus-{stamp}"
    root = requested.resolve()
    if root.exists():
        raise SystemExit(f"refusing to reuse corpus directory: {root}")
    root.mkdir(parents=True)
    return root


def sanitized_java_options(environment: dict[str, str], temp_root: Path) -> str:
    retained = []
    for option in shlex.split(environment.get("JAVA_TOOL_OPTIONS", "")):
        if option.startswith((
                "-Djava.io.tmpdir=", "-Dclava.astAbWire=", "-Dclava.astWireMetrics=",
                "-Dclava.astTestGcPolicy=", "-XX:+DisableExplicitGC", "-XX:-DisableExplicitGC")):
            continue
        retained.append(option)
    retained.extend((f"-Djava.io.tmpdir={temp_root}", "-Dclava.astAbWire=protobuf"))
    return shlex.join(retained)


def collection_environment(base_environment: dict[str, str], suite: str, root: Path,
                           native_tool: Path, runtime: Path) -> dict[str, str]:
    environment = base_environment.copy()
    temporary = root / "temp" / suite
    temporary.mkdir(parents=True, exist_ok=True)
    environment.update({
        "TMPDIR": str(temporary), "TMP": str(temporary), "TEMP": str(temporary),
        "XDG_CACHE_HOME": str(temporary), "CLANG_DUMPER_TOOL": str(native_tool),
        CAPTURE_ENV: str(root), SUITE_ENV: suite,
    })
    environment["JAVA_TOOL_OPTIONS"] = sanitized_java_options(environment, temporary)
    if suite == "clava-js":
        environment["CLAVA_SUITE_JAR_PATH"] = str(runtime)
    return environment


def run_suite_capture(suite: str, clava_root: Path, native_tool: Path, js_workspace: Path,
                      runtime: Path, root: Path, dependencies: dict[str, Path]) -> dict[str, Any]:
    run_dir = root / "capture_runs" / suite
    run_dir.mkdir(parents=True)
    environment = collection_environment(os.environ, suite, root, native_tool, runtime)
    log_path = run_dir / "capture-only.log"

    if suite == "java":
        command = [
            "gradle", "--no-daemon", "--offline",
            f"-PclangDumperRoot={native_tool.parent.parent}",
            "-p", str(clava_root / "ClangAstParser"),
            "--init-script", str(SCRIPT_ROOT / "java-suite.init.gradle"),
            "--rerun-tasks", "test",
        ]
        with log_path.open("w") as log:
            process = subprocess.run(command, cwd=clava_root, env=environment,
                                     stdout=log, stderr=subprocess.STDOUT, check=False)
        counts = base.java_counts(clava_root / "ClangAstParser/build/test-results/test")
    else:
        config_path = run_dir / "vitest.config.ts"
        ab.write_js_config(config_path, js_workspace, runtime)
        report_path = run_dir / "vitest.json"
        command = [
            "npm", "exec", "--workspace", "@specs-feup/clava", "--", "vitest", "run",
            "--config", str(config_path), "--reporter=json", "--outputFile", str(report_path),
            "-t", base.JS_TEST_FILTER,
        ]
        with log_path.open("w") as log:
            process = subprocess.run(command, cwd=js_workspace, env=environment,
                                     stdout=log, stderr=subprocess.STDOUT, check=False)
        report = json.loads(report_path.read_text()) if report_path.is_file() else {}
        counts = base.js_counts(report)

    return {
        "suite": suite,
        "command": command,
        "return_code": process.returncode,
        "test_counts": counts,
        "expected_test_counts": EXPECTED_TEST_COUNTS[suite],
        "test_counts_match": all(counts.get(key) == value
                                 for key, value in EXPECTED_TEST_COUNTS[suite].items()),
        "log": log_path.relative_to(root).as_posix(),
        "capture_root": str(root),
        "cache_used_for_capture": False,
        "explicit_gc_policy_flags": [
            token for token in shlex.split(environment.get("JAVA_TOOL_OPTIONS", ""))
            if token in ("-XX:+DisableExplicitGC", "-XX:-DisableExplicitGC")
        ],
    }


def load_json_records(directory: Path) -> list[dict[str, Any]]:
    records = []
    if not directory.is_dir():
        return records
    for path in sorted(directory.glob("*.json")):
        records.append(json.loads(path.read_text()))
    return records


def reference_source_counts(results_root: Path, suites: tuple[str, ...]) -> dict[str, Counter[str]]:
    csv_path = results_root / "parses.csv"
    if not csv_path.is_file():
        return {}
    import csv

    counts = {suite: Counter() for suite in suites}
    with csv_path.open(newline="") as source:
        for row in csv.DictReader(source):
            if row.get("suite") not in counts:
                continue
            if (row.get("phase") == "seed" and row.get("protocol") == "text"
                    and row.get("gc_policy") == "normal" and row.get("event_valid") == "True"):
                counts[row["suite"]][row.get("source_content_sha256", "")] += 1
    return counts


def verify_blobs(records: list[dict[str, Any]], root: Path) -> tuple[dict[tuple[str, str], dict[str, Any]], list[str]]:
    files: dict[tuple[str, str], dict[str, Any]] = {}
    errors: list[str] = []

    def add(file_record: dict[str, Any]) -> None:
        original_path = file_record.get("original_path")
        digest = file_record.get("sha256")
        blob = file_record.get("blob")
        if not original_path or not digest or not blob:
            errors.append("captured file record is missing original_path, sha256, or blob")
            return
        blob_path = root / blob
        if not blob_path.is_file():
            errors.append(f"missing content blob {blob}")
            return
        actual = sha256_file(blob_path)
        if actual != digest:
            errors.append(f"blob digest mismatch for {original_path}: expected {digest}, got {actual}")
            return
        files[(original_path, digest)] = file_record

    for call in records:
        for source in call.get("sources", []):
            add(source)
        for native in call.get("native_invocations", []):
            add(native.get("source", {}))
            for dependency in native.get("dependencies", []):
                add(dependency)
    return files, errors


def manifest_capture(root: Path, args: argparse.Namespace, source_metadata: dict[str, Any],
                     runtime: Path, runtime_template: Path, suite_results: list[dict[str, Any]],
                     suites: tuple[str, ...]) -> dict[str, Any]:
    calls = load_json_records(root / "raw/calls")
    native = load_json_records(root / "raw/native")
    calls_by_id = {call["event_id"]: call for call in calls}
    calls_by_suite: dict[str, list[dict[str, Any]]] = defaultdict(list)
    native_by_call: dict[str, list[dict[str, Any]]] = defaultdict(list)
    orphans = []
    for call in calls:
        calls_by_suite[call.get("suite", "unknown")].append(call)
    for invocation in native:
        call_id = invocation.get("code_parser_call_id")
        if call_id in calls_by_id:
            native_by_call[call_id].append(invocation)
        else:
            orphans.append(invocation)

    call_events = []
    for suite in suites:
        for call in sorted(calls_by_suite.get(suite, []),
                           key=lambda row: (row.get("captured_at_epoch_ms", 0), row["event_id"])):
            nested = sorted(native_by_call.get(call["event_id"], []),
                            key=lambda row: (row.get("captured_at_epoch_ms", 0), row["event_id"]))
            call_events.append({**call, "native_invocations": nested})

    replay_events = []
    ordered_native = []
    for call in call_events:
        ordered_native.extend((call, invocation) for invocation in call["native_invocations"])
    ordered_native.extend((None, invocation) for invocation in orphans)
    ordered_native.sort(key=lambda item: (
        item[1].get("suite", "unknown"), item[1].get("captured_at_epoch_ms", 0), item[1]["event_id"]
    ))

    for ordinal, (call, invocation) in enumerate(ordered_native, start=1):
        source = invocation["source"]
        call_options = call or {}
        replay_event = {
            "event_id": invocation["event_id"],
            "ordinal": ordinal,
            "suite": invocation.get("suite", "unknown"),
            "code_parser_call_id": invocation.get("code_parser_call_id"),
            "test_id": invocation.get("test_id"),
            "tester_invocation": invocation.get("tester_invocation"),
            "parse_pass": invocation.get("parse_pass"),
            "resource_key": invocation.get("resource_key"),
            "source_label": invocation.get("resource_key") or Path(source["original_path"]).name,
            "source": source,
            "sources": [source],
            "input_sources": call_options.get("input_sources", []),
            "code_parser_sources": call_options.get("sources", []),
            "compiler_options": call_options.get("compiler_options", []),
            "parser_config": call_options.get("code_parser_options", {}),
            "data_store_options": invocation.get("data_store_options", {}),
            "effective_system_includes": invocation.get("effective_system_includes", []),
            "generated_parse_root": invocation.get("generated_parse_root"),
            "effective_libc_mode": call_options.get("effective_libc_mode"),
            "runtime_resources": call_options.get("runtime_resources", {}),
            "path_map": invocation.get("path_map", call_options.get("path_map", [])),
            "replay_cwd": invocation.get("replay_cwd"),
            "original_cwd": invocation.get("original_cwd"),
            "working_directory": invocation.get("working_directory"),
            "argv": invocation.get("argv", []),
            "process_argv": invocation.get("process_argv", []),
            "args_sha256": invocation.get("args_sha256"),
            "standard": invocation.get("standard"),
            "parse_id": invocation.get("parse_id"),
            "effective_environment": invocation.get("effective_environment", {}),
            "environment_delta": invocation.get("environment_delta", {}),
            "dependencies": invocation.get("dependencies", []),
            "dependency_file": invocation.get("dependency_file"),
            "captured_at_epoch_ms": invocation.get("captured_at_epoch_ms"),
        }
        replay_events.append(replay_event)

    files, blob_errors = verify_blobs(call_events, root)
    file_records = sorted(files.values(), key=lambda row: (row["original_path"], row["sha256"]))
    write_jsonl(root / "events.jsonl", call_events)
    write_jsonl(root / "replay_events.jsonl", replay_events)
    write_jsonl(root / "files.jsonl", file_records)

    event_counts = Counter(row["suite"] for row in replay_events)
    pass_counts: dict[str, Counter[str]] = defaultdict(Counter)
    source_configs: dict[str, Counter[tuple[str, str]]] = defaultdict(Counter)
    for row in replay_events:
        pass_counts[row["suite"]][row.get("parse_pass") or "unlabeled"] += 1
        source_configs[row["suite"]][(row["source"].get("sha256", ""), row.get("args_sha256", ""))] += 1

    references = reference_source_counts(args.reference_results.resolve(), suites)
    reference_matches = {}
    for suite in suites:
        actual = Counter(row["source"].get("sha256", "") for row in replay_events if row["suite"] == suite)
        expected = references.get(suite)
        reference_matches[suite] = None if expected is None else actual == expected

    suite_result_by_name = {row["suite"]: row for row in suite_results}
    suite_coverage = {}
    for suite in suites:
        suite_rows = [row for row in replay_events if row["suite"] == suite]
        duplicate_extras = sum(count - 1 for count in source_configs[suite].values() if count > 1)
        suite_coverage[suite] = {
            "expected_native_events": EXPECTED_NATIVE_EVENTS[suite],
            "captured_native_events": event_counts[suite],
            "event_count_match": event_counts[suite] == EXPECTED_NATIVE_EVENTS[suite],
            "code_parser_calls": len(calls_by_suite.get(suite, [])),
            "unlinked_native_events": sum(1 for row in suite_rows if row.get("code_parser_call_id") is None),
            "multi_source_calls": sum(1 for call in calls_by_suite.get(suite, [])
                                       if len(call.get("sources", [])) > 1),
            "duplicate_source_and_config_extras": duplicate_extras,
            "parse_pass_counts": dict(sorted(pass_counts[suite].items())),
            "reference_source_sha256_multiset_match": reference_matches[suite],
            "suite_run": suite_result_by_name.get(suite, {}),
        }

    native_tool = args.native_tool.resolve()
    version = subprocess.run([str(native_tool), "--version"], text=True,
                             stdout=subprocess.PIPE, stderr=subprocess.STDOUT, check=False)
    runtime_manifest = base.runtime_manifest(runtime)
    expected_suite_ok = all(
        row.get("return_code") == 0 and row.get("test_counts_match") is True
        for row in suite_results
    )
    event_counts_ok = all(suite_coverage[suite]["event_count_match"] for suite in suites)
    source_matches_ok = all(reference_matches[suite] is True for suite in suites)
    valid = expected_suite_ok and event_counts_ok and source_matches_ok and not blob_errors

    corpus = {
        "schema_version": 1,
        "created_at": dt.datetime.now(dt.timezone.utc).isoformat(),
        "experiment": "capture-only source/config corpus for standalone per-parse replay",
        "capture_is_timing_dataset": False,
        "valid": valid,
        "suites": list(suites),
        "coverage": suite_coverage,
        "expected_native_event_counts": {suite: EXPECTED_NATIVE_EVENTS[suite] for suite in suites},
        "code_parser_call_count": len(call_events),
        "native_invocation_count": len(replay_events),
        "unlinked_native_invocation_count": len(orphans),
        "files_count": len(file_records),
        "content_blobs_count": len({row["sha256"] for row in file_records}),
        "file_byte_total_deduplicated": sum(row["size_bytes"] for row in file_records),
        "path_mapping": {"original_root": "/", "replay_root": "rootfs"},
        "capture_behavior": {
            "AST_dump_cache": "bypassed only while collecting dependency files",
            "dependency_capture": "clang-dumper -MD/-MF output captured before temporary cleanup",
            "heap_logging": "SHOW_EXEC_INFO memory sampling and timing log branches skipped",
            "explicit_GC": "no GC policy flags set; the only parser-side explicit-GC path is skipped",
            "benchmark_timings": "none collected or used",
        },
        "artifacts": {
            "calls": "events.jsonl",
            "single_source_replay_rows": "replay_events.jsonl",
            "file_manifest": "files.jsonl",
            "content_blobs": "blobs/sha256/<sha256>",
            "replay_path_map": "original absolute paths map under rootfs/<absolute path without leading slash>",
        },
        "runtime": {
            "root": str(runtime),
            "template": str(runtime_template),
            "manifest": runtime_manifest,
            "native_tool": str(native_tool),
            "native_tool_sha256": sha256_file(native_tool),
            "native_tool_version_output": version.stdout.strip(),
            "native_tool_version_return_code": version.returncode,
        },
        "source_metadata": source_metadata,
        "suite_runs": suite_results,
        "blob_errors": blob_errors,
        "hashes": {
            "events.jsonl": sha256_file(root / "events.jsonl"),
            "replay_events.jsonl": sha256_file(root / "replay_events.jsonl"),
            "files.jsonl": sha256_file(root / "files.jsonl"),
        },
    }
    (root / "corpus.json").write_text(json.dumps(corpus, indent=2, sort_keys=True) + "\n")
    (root / "README.md").write_text(corpus_readme(corpus), encoding="utf-8")
    return corpus


def write_jsonl(path: Path, rows: list[dict[str, Any]]) -> None:
    with path.open("w", encoding="utf-8", newline="\n") as output:
        for row in rows:
            output.write(json.dumps(row, ensure_ascii=False, sort_keys=True, separators=(",", ":")))
            output.write("\n")


def corpus_readme(corpus: dict[str, Any]) -> str:
    lines = [
        "# Standalone per-parse replay corpus",
        "",
        "This directory contains source and configuration snapshots collected by running each selected suite once.",
        "The suite pass discovered workloads only. Its logs and wall times are not benchmark observations.",
        "",
        "`replay_events.jsonl` has one row per native source parse. Duplicate source/config events remain separate rows.",
        "Each row includes the parent `CodeParser.parse` options, parser datakeys, exact native argv, source SHA-256,",
        "working directory, path map, and transitive compiler dependency references. `events.jsonl` records parent",
        "CodeParser calls with all child native parses. `files.jsonl` maps each captured file path/hash to a deduplicated",
        "content blob under `blobs/sha256/`.",
        "",
        "Materializers should map original absolute paths through the `/` to `rootfs` entry and verify each blob SHA-256.",
        "The original parser test runner uses `SHOW_EXEC_INFO=true`; standalone replay should set it to false, as recorded",
        "in the comparison plan, to avoid heap sampling. The corpus capture pass itself skipped that logging branch.",
        "",
        "No explicit GC flag or benchmark timing was used for capture. AST dump caching was bypassed only while the",
        "compiler wrote dependency files. The standalone benchmark controls cache behavior per measurement cell.",
        "",
        f"Captured native invocations: {corpus['native_invocation_count']}",
        f"Captured CodeParser calls: {corpus['code_parser_call_count']}",
        f"Unique path/hash files: {corpus['files_count']}",
        f"Deduplicated content bytes: {corpus['file_byte_total_deduplicated']}",
        "",
        "Manifest hashes:",
        "",
    ]
    for name, digest in corpus["hashes"].items():
        lines.append(f"- `{name}`: `{digest}`")
    lines.append("")
    return "\n".join(lines)


def main() -> int:
    args = parse_args()
    clava_root = args.clava_root.resolve()
    native_tool = args.native_tool.resolve()
    js_workspace = args.js_workspace.resolve()
    runtime_template = args.runtime_template.resolve()
    suites = ("clava-js", "java") if args.suite == "all" else (args.suite,)

    if not runtime_template.is_dir():
        raise SystemExit(f"runtime template is missing: {runtime_template}")
    dependency_roots = ab.configure_gradle_dependency_roots(js_workspace)
    ab.validate_inputs(clava_root, native_tool, js_workspace, dependency_roots, require_native=True)
    root = new_output_root(args.output_root)
    runtime = root / "runtime" / "java-binaries"
    shutil.copytree(runtime_template, runtime)

    env = os.environ.copy()
    env.update({key: str(value) for key, value in dependency_roots.items()})
    build_command = [
        "gradle", "--no-daemon", "--offline", "-p", str(clava_root / "ClangAstParser"), "jar",
    ]
    build_log = root / "capture_runs" / "build-parser-jar.log"
    build_log.parent.mkdir(parents=True, exist_ok=True)
    with build_log.open("w") as log:
        build = subprocess.run(build_command, cwd=clava_root, env=env,
                               stdout=log, stderr=subprocess.STDOUT, check=False)
    if build.returncode != 0:
        raise SystemExit(f"parser JAR build failed; see {build_log}")
    parser_jar = clava_root / "ClangAstParser/build/libs/ClangAstParser.jar"
    staged_parser_jar = runtime / "lib/ClangAstParser.jar"
    if not parser_jar.is_file() or not staged_parser_jar.is_file():
        raise SystemExit(f"parser JAR missing: built={parser_jar}, staged={staged_parser_jar}")
    shutil.copy2(parser_jar, staged_parser_jar)

    source_state = ab.source_metadata(clava_root, native_tool, js_workspace)
    plan = {
        "created_at": dt.datetime.now(dt.timezone.utc).isoformat(),
        "experiment": "capture-only replay corpus",
        "output_root": str(root),
        "suites": list(suites),
        "source_metadata_before_capture": source_state,
        "runtime_template": str(runtime_template),
        "runtime_parser_jar_sha256": sha256_file(staged_parser_jar),
        "native_tool_sha256": sha256_file(native_tool),
        "commands": {"build_parser_jar": build_command},
        "explicit_gc": "not requested; JAVA_TOOL_OPTIONS GC-policy flags stripped",
    }
    (root / "capture-plan.json").write_text(json.dumps(plan, indent=2, sort_keys=True) + "\n")

    suite_results = []
    for suite in suites:
        result = run_suite_capture(suite, clava_root, native_tool, js_workspace,
                                  runtime, root, dependency_roots)
        suite_results.append(result)
        print(json.dumps(result, sort_keys=True), flush=True)

    metadata_after = ab.source_metadata(clava_root, native_tool, js_workspace)
    corpus = manifest_capture(root, args, metadata_after, runtime, runtime_template,
                              suite_results, suites)
    corpus["source_metadata_changed_during_capture"] = metadata_after != source_state
    corpus["capture_plan_sha256"] = sha256_file(root / "capture-plan.json")
    corpus["hashes"]["capture-plan.json"] = corpus["capture_plan_sha256"]
    corpus["hashes"]["capture_runs/java/capture-only.log"] = (
        sha256_file(root / "capture_runs/java/capture-only.log")
        if (root / "capture_runs/java/capture-only.log").is_file() else None
    )
    corpus["hashes"]["capture_runs/clava-js/capture-only.log"] = (
        sha256_file(root / "capture_runs/clava-js/capture-only.log")
        if (root / "capture_runs/clava-js/capture-only.log").is_file() else None
    )
    corpus["valid"] = corpus["valid"] and not corpus["source_metadata_changed_during_capture"]
    (root / "corpus.json").write_text(json.dumps(corpus, indent=2, sort_keys=True) + "\n")
    (root / "README.md").write_text(corpus_readme(corpus), encoding="utf-8")
    print(json.dumps({
        "corpus": str(root / "corpus.json"),
        "valid": corpus["valid"],
        "native_invocation_count": corpus["native_invocation_count"],
        "code_parser_call_count": corpus["code_parser_call_count"],
        "coverage": corpus["coverage"],
        "hashes": corpus["hashes"],
    }, sort_keys=True, indent=2), flush=True)
    return 0 if corpus["valid"] else 1


if __name__ == "__main__":
    raise SystemExit(main())

#!/usr/bin/env python3
"""Measure Clava App construction inside the unchanged parser test suites.

The Java overlay supplies the narrow in-parser timing hook. This runner only
selects frozen stages/cache states, injects that overlay, and validates the
captured parser-call workload; Gradle/Vitest/test setup is outside the reported
App intervals.
"""

from __future__ import annotations

import argparse
import collections
import csv
import getpass
import hashlib
import json
import math
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from typing import Any
from urllib.parse import unquote

SCRIPT_ROOT = Path(__file__).resolve().parents[1]
APP_BUILD_ROOT = SCRIPT_ROOT / "app-build"
sys.path.insert(0, str(SCRIPT_ROOT))
import run_comparison as comparison


DEFAULT_FROZEN_ROOT = SCRIPT_ROOT / "results/matched-fast-20261002-r1"
DEFAULT_OUTPUT_ROOT = SCRIPT_ROOT / "results/app-build-20261002-r1"
DEFAULT_INCLUDE_AUDIT_OVERLAY_ROOT = (
    SCRIPT_ROOT / "results/app-build-20261002-r1/include-audit-overlay-java17-r2"
)
STAGE_KEYS = ("before-cache", "ccache-text", "protobuf", "flatbuffers")
SUITES = ("clava-js", "java")
EXPECTED_TESTS = {
    "clava-js": {"total_tests": 164, "passed_tests": 158, "failed_tests": 0, "skipped_tests": 6},
    "java": {"total_tests": 116, "passed_tests": 116, "failed_tests": 0, "skipped_tests": 0},
}
STAGE_LABELS = {
    "before-cache": "Before cache",
    "ccache-text": "Text + ccache",
    "protobuf": "Protobuf",
    "flatbuffers": "eager FlatBuffers",
}
INCLUDE_AUDIT_TEST_FILTER = r"^(?:CxxTest (?:Wrap|FileRebuild|CloneOnFile))$"
INCLUDE_AUDIT_TEST_FILE = "api/LegacyIntegrationTests - CXX.test.ts"
FILE_REBUILD_HEADER_SHA256 = "9e666f658b9163e814379456e35b350b82ae53086e668e0d48ec5d01dff0e3ff"
NO_INCLUDE_ADDED_FILE_SHA256 = "e3bd207f4221442494484d2c2e6963ce641b77e529d9244bf5e23796b8375aaf"


def sha256_file(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def digest(value: Any) -> str:
    raw = json.dumps(value, sort_keys=True, separators=(",", ":"), ensure_ascii=False).encode()
    return hashlib.sha256(raw).hexdigest()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--frozen-root", type=Path, default=DEFAULT_FROZEN_ROOT)
    parser.add_argument("--output-root", type=Path)
    parser.add_argument("--phase", choices=("preflight", "include-audit", "measure"), required=True)
    parser.add_argument("--repeat-count", type=int, default=4)
    parser.add_argument("--host-lock-confirmed", action="store_true")
    parser.add_argument("--lock-note", default="")
    parser.add_argument("--preflight-manifest", type=Path)
    parser.add_argument(
        "--resume-verified-round1", action="store_true",
        help="resume an existing measurement output after validating its complete round-1 cells",
    )
    parser.add_argument("--include-audit-overlay-root", type=Path,
                        default=DEFAULT_INCLUDE_AUDIT_OVERLAY_ROOT,
                        help="prebuilt, parser-byte-identical overlays for focused include audit")
    parser.add_argument("--report-payload", type=Path)
    parser.add_argument("--stage-keys", default=",".join(STAGE_KEYS),
                        help="preflight subset only; measurement requires all four stages")
    parser.add_argument("--suite-keys", default=",".join(SUITES),
                        help="preflight subset only; measurement requires both suites")
    args = parser.parse_args()
    requested = tuple(key.strip() for key in args.stage_keys.split(",") if key.strip())
    if not requested or len(set(requested)) != len(requested) or not set(requested).issubset(STAGE_KEYS):
        parser.error(f"--stage-keys must be a unique subset of {','.join(STAGE_KEYS)}")
    if args.phase == "measure" and args.repeat_count != 4:
        parser.error("the reportable App-build matrix requires exactly four rotated rounds")
    if args.resume_verified_round1 and args.phase != "measure":
        parser.error("--resume-verified-round1 is only valid with --phase measure")
    if args.resume_verified_round1 and args.preflight_manifest is None:
        parser.error("--resume-verified-round1 requires --preflight-manifest")
    if args.phase == "measure" and set(requested) != set(STAGE_KEYS):
        parser.error("measurement requires all four stages")
    args.selected_stage_keys = requested
    requested_suites = tuple(key.strip() for key in args.suite_keys.split(",") if key.strip())
    if not requested_suites or len(set(requested_suites)) != len(requested_suites) or not set(requested_suites).issubset(SUITES):
        parser.error(f"--suite-keys must be a unique subset of {','.join(SUITES)}")
    if args.phase == "measure" and set(requested_suites) != set(SUITES):
        parser.error("measurement requires both suites")
    if args.phase == "include-audit":
        if set(requested) != set(STAGE_KEYS):
            parser.error("include audit requires all four frozen stages")
        if requested_suites != ("clava-js",):
            parser.error("include audit runs only the Clava-JS suite")
        if args.preflight_manifest is None:
            parser.error("include audit requires --preflight-manifest for original suite identities")
    args.selected_suites = requested_suites
    return args


def read_plan(frozen_root: Path) -> dict[str, Any]:
    plan_path = frozen_root / "enriched-final-verified/js-results.json"
    if not plan_path.is_file():
        raise FileNotFoundError(f"missing accepted frozen stage manifest: {plan_path}")
    plan = json.loads(plan_path.read_text())
    stages = plan.get("plan", {}).get("stages", {})
    if set(stages) != set(STAGE_KEYS):
        raise RuntimeError(f"frozen manifest has unexpected stages: {sorted(stages)}")
    return plan


def stage_records(plan: dict[str, Any]) -> dict[str, dict[str, Any]]:
    stages = plan["plan"]["stages"]
    records: dict[str, dict[str, Any]] = {}
    for key in STAGE_KEYS:
        record = dict(stages[key])
        record["root"] = Path(record["root"]).resolve()
        record["clava"] = record["root"] / "clava"
        record["runtime"] = record["clava"] / "Clava-JS/java-binaries"
        record["runtime_lib"] = record["runtime"] / "lib"
        record["dumper"] = Path(record["dumper"]).resolve()
        record["native_root"] = Path(record["native_root"]).resolve()
        record["cache_enabled"] = key != "before-cache"
        record["label"] = STAGE_LABELS[key]
        for needed in (record["runtime"], record["dumper"], record["native_root"]):
            if not needed.exists():
                raise RuntimeError(f"frozen stage input is missing for {key}: {needed}")
        parser_jar = record["runtime_lib"] / "ClangAstParser.jar"
        if not parser_jar.is_file():
            raise RuntimeError(f"missing frozen parser JAR for {key}: {parser_jar}")
        import zipfile

        with zipfile.ZipFile(parser_jar) as jar:
            tag = jar.read("clang-dumper-release.tag").decode().strip()
        expected_native_dir = str(record["dumper"].parent)
        if Path(tag).resolve() != Path(expected_native_dir).resolve():
            raise RuntimeError(f"frozen native tag mismatch for {key}: {tag} != {expected_native_dir}")
        record["parser_jar_sha256"] = sha256_file(parser_jar)
        record["overlay"] = None
        records[key] = record
    return records


def schedule(repeat: int) -> list[tuple[str, str, dict[str, Any]]]:
    """Four-way stage rotation and alternating suite order, 20 cells/round."""
    rotation = (repeat - 1) % len(STAGE_KEYS)
    ordered = list(STAGE_KEYS[rotation:] + STAGE_KEYS[:rotation])
    result: list[tuple[str, str, dict[str, Any]]] = []
    for position, key in enumerate(ordered):
        modes = ["direct"]
        if key != "before-cache":
            modes += ["cold", "warm"]
            offset = (repeat - 1 + position) % len(modes)
            modes = modes[offset:] + modes[:offset]
        for mode in modes:
            suites = SUITES if (repeat + position + modes.index(mode)) % 2 == 0 else tuple(reversed(SUITES))
            result.extend((suite, mode, {"key": key}) for suite in suites)
    return result


def normalize_string(value: str, stage: dict[str, Any], temp_root: Path) -> str:
    replacements = [
        (str(stage["clava"]), "$CLAVA"),
        (str(stage["root"]), "$STAGE"),
        (str(temp_root), "$TMP"),
    ]
    result = value
    for prefix, replacement in sorted(replacements, key=lambda item: len(item[0]), reverse=True):
        result = result.replace(prefix, replacement)
    # These are the two parser-test harness generated roots, not arbitrary
    # numeric or UUID path components. Keep every suffix after the root so
    # file names and relative include structure remain part of the fingerprint.
    result = re.sub(
        r"(?:\$CLAVA/ClangAstParser/|\$TMP/)?temp-clang-ast-(?:[0-9]+-)*[0-9]+(?=/|$)",
        "$JAVA_TEST_REPARSE",
        result,
    )
    result = re.sub(
        r"__clava_woven_[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}_[^/\\]+",
        "$JS_REBUILD",
        result,
    )
    result = re.sub(r"(?:\$TMP/)?junit[0-9]+(?=/|$)", "$JUNIT_TEMP", result)
    return result


def normalize(value: Any, stage: dict[str, Any], temp_root: Path) -> Any:
    if isinstance(value, str):
        return normalize_string(value, stage, temp_root)
    if isinstance(value, list):
        return [normalize(item, stage, temp_root) for item in value]
    if isinstance(value, dict):
        controls = {
            "ast_dump_cache", "generated_parse_root", "dumper_folder",
            "show_exec_info", "direct_mode", "cache_mode",
        }
        return {
            str(key): normalize(item, stage, temp_root)
            for key, item in value.items()
            if str(key).lower().replace("-", "_") not in controls
        }
    return value


def fingerprint_payload(row: dict[str, Any], stage: dict[str, Any], temp_root: Path) -> dict[str, Any]:
    normalized_inputs = normalize(row.get("input_sources", row.get("inputs", [])), stage, temp_root)
    if isinstance(normalized_inputs, list):
        # ClangAstDumper sorts user source paths before native parsing, and
        # getInputSourceFolders also sorts. Keep raw input_sources untouched in
        # each capture row, but fingerprint membership in that actual order.
        normalized_inputs = sorted(normalized_inputs, key=lambda item: json.dumps(item, sort_keys=True))
    normalized_options = normalize(row.get("compiler_options", row.get("options", [])), stage, temp_root)
    sources = row.get("resolved_sources", [])
    source_hashes = [source.get("sha256") for source in sources if isinstance(source, dict)]
    if row.get("suite") == "clava-js" and not (bool(row.get("syntax_only")) or row.get("excluded_reason") == "syntax_only"):
        role = None
        if source_hashes == [FILE_REBUILD_HEADER_SHA256]:
            role = "$JS_REBUILD_CURRENT_CODE"
        elif source_hashes == [NO_INCLUDE_ADDED_FILE_SHA256]:
            role = "$JS_REBUILD_NO_INCLUDE_I_ROOT"
        if role is not None:
            raw_options = row.get("compiler_options", row.get("options", []))
            first_i = next((index for index, option in enumerate(raw_options)
                            if isinstance(option, str) and option.startswith("-I")), None)
            if first_i is None:
                raise RuntimeError(f"approved generated-root case lost its first -I option: {row}")
            raw_path = raw_options[first_i][2:]
            static_root = raw_path.endswith("/Clava-JS/__clava_woven_for_file_rebuild")
            current_code_root = re.search(
                r"/__clava_woven_[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}_[^/]+/current-code$",
                raw_path,
            ) is not None
            if not (static_root or current_code_root):
                raise RuntimeError(f"approved generated-root case has an unrecognized first -I path: {raw_path}")
            normalized_options[first_i] = f"-I{role}"
    return {
        "input_sources": normalized_inputs,
        "resolved_sources": normalize(row.get("resolved_sources", []), stage, temp_root),
        "compiler_options": normalized_options,
        "parser_config": normalize(row.get("parser_config", row.get("config", {})), stage, temp_root),
        "working_directory": normalize(row.get("working_directory"), stage, temp_root),
    }


def enrich_calls(metrics_path: Path, suite: str, stage_key: str, mode: str, repeat: int,
                 stage: dict[str, Any], temp_root: Path) -> list[dict[str, Any]]:
    rows: list[dict[str, Any]] = []
    for line_number, line in enumerate(metrics_path.read_text().splitlines(), start=1):
        if not line.strip():
            continue
        try:
            row = json.loads(line)
        except json.JSONDecodeError as exc:
            raise RuntimeError(f"invalid overlay JSONL at {metrics_path}:{line_number}: {exc}") from exc
        row.update({"suite": suite, "stage": stage_key, "mode": mode, "repeat": repeat})
        fingerprint_data = fingerprint_payload(row, stage, temp_root)
        fingerprint = digest(fingerprint_data)
        row["group_fingerprint"] = fingerprint
        row["group_id"] = fingerprint
        row["normalized_workload"] = fingerprint_data
        rows.append(row)
    occurrences: collections.Counter[str] = collections.Counter()
    for row in rows:
        fingerprint = row["group_fingerprint"]
        row["group_occurrence"] = occurrences[fingerprint]
        row["group_id"] = f"{fingerprint}:{occurrences[fingerprint]:04d}"
        occurrences[fingerprint] += 1
    return rows


def validate_capture_rows(rows: list[dict[str, Any]], overlay: Path, suite: str, stage_key: str) -> None:
    for row in rows:
        sources = row.get("resolved_sources", [])
        if not isinstance(sources, list):
            raise RuntimeError(f"resolved source manifest is malformed for {suite}/{stage_key}: {row}")
        for source in sources:
            if not isinstance(source, dict) or source.get("resolution_error"):
                raise RuntimeError(f"could not resolve parser input sources for {suite}/{stage_key}: {source}")
            if source.get("sha256_error") or not isinstance(source.get("sha256"), str) or not re.fullmatch(r"[0-9a-f]{64}", source["sha256"]):
                raise RuntimeError(f"missing source content hash for {suite}/{stage_key}: {source}")
        parser_config = row.get("parser_config", {})
        if not isinstance(parser_config, dict):
            raise RuntimeError(f"parser configuration manifest is malformed for {suite}/{stage_key}: {row}")
        if row.get("metadata_complete") is not True:
            raise RuntimeError(f"overlay metadata is incomplete for {suite}/{stage_key}: {row}")
        if row.get("metadata_errors"):
            raise RuntimeError(f"overlay metadata errors for {suite}/{stage_key}: {row.get('metadata_errors')}")
        config_errors = [key for key in parser_config if str(key).endswith("_error")]
        if config_errors:
            raise RuntimeError(f"could not inspect parser configuration for {suite}/{stage_key}: {config_errors}")
        origin = str(row.get("parser_class_origin", ""))
        if not origin:
            raise RuntimeError(f"parser overlay origin was not captured for {suite}/{stage_key}")
        if str(overlay.resolve()) not in unquote(origin):
            raise RuntimeError(f"parser overlay did not win classpath for {suite}/{stage_key}: {origin}")
        helper_origin = str(row.get("helper_class_origin", ""))
        if not helper_origin or str(overlay.resolve()) not in unquote(helper_origin):
            raise RuntimeError(f"App-build helper origin mismatch for {suite}/{stage_key}: {helper_origin}")
        if row.get("overlay_sha256") != sha256_file(overlay):
            raise RuntimeError(f"loaded overlay digest mismatch for {suite}/{stage_key}: {row.get('overlay_sha256')}")
        if row.get("failure_reason"):
            raise RuntimeError(f"parser call captured an exception for {suite}/{stage_key}: {row['failure_reason']}")
        actual_jvm_args = row.get("jvm_input_arguments")
        if not isinstance(actual_jvm_args, list):
            raise RuntimeError(f"actual JVM input arguments were not captured for {suite}/{stage_key}")
        forbidden_args = [
            argument for argument in actual_jvm_args
            if (argument.startswith(("-javaagent:", "-agentlib:", "-agentpath:", "-Xrun"))
                or re.search(r"(?i)DisableExplicitGC|ExplicitGCInvokesConcurrent|Use[A-Za-z]*GC|MaxGCPauseMillis|ParallelGCThreads|ConcGCThreads", argument))
        ]
        if forbidden_args:
            raise RuntimeError(f"coverage agent or explicit GC override found in JVM input args: {forbidden_args}")
        if suite == "java":
            maximum_memory = row.get("jvm_max_memory_bytes")
            expected_memory = 512 * 1024 * 1024
            if not isinstance(maximum_memory, (int, float)) or not 0.98 * expected_memory <= maximum_memory <= 1.05 * expected_memory:
                raise RuntimeError(f"Java worker max heap differs from the 512 MiB matrix contract: {maximum_memory}")
        if not row.get("valid", True):
            raise RuntimeError(f"parser call was invalid for {suite}/{stage_key}: {row}")
        syntax_only = bool(row.get("syntax_only")) or row.get("excluded_reason") == "syntax_only"
        if syntax_only:
            if row.get("elapsed_ms") is not None:
                raise RuntimeError(f"syntax-only parser call was timed: {row}")
            continue
        if row.get("elapsed_ms") is None:
            raise RuntimeError(f"non-syntax App construction lacks elapsed_ms: {row}")
        if row.get("app_returned_null"):
            raise RuntimeError(f"App construction returned null or invalid result for {suite}/{stage_key}: {row}")
        if not isinstance(row["elapsed_ms"], (int, float)) or isinstance(row["elapsed_ms"], bool):
            raise RuntimeError(f"invalid App-construction interval for {suite}/{stage_key}: {row}")
        elapsed = float(row["elapsed_ms"])
        if not math.isfinite(elapsed) or elapsed < 0:
            raise RuntimeError(f"App-construction interval is not finite and nonnegative for {suite}/{stage_key}: {row}")


def context_pattern(rows: list[dict[str, Any]]) -> tuple[tuple[tuple[str, int], ...], ...]:
    """Return the ClavaContext sharing partition, independent of call order and IDs.

    Each context is represented by the multiset of App-call workload fingerprints
    it parsed. Sorting those signatures preserves true sharing (including
    multiplicity for identical calls) while ignoring nondeterministic sequential
    test-file order and opaque first-seen context labels.
    """
    members: dict[str, collections.Counter[str]] = {}
    for row in rows:
        if row.get("elapsed_ms") is None or row.get("syntax_only") or row.get("excluded_reason") == "syntax_only":
            continue
        context = row.get("context_id")
        if context is None:
            continue
        members.setdefault(str(context), collections.Counter())[row["group_fingerprint"]] += 1
    return tuple(sorted(
        tuple(sorted((fingerprint, count) for fingerprint, count in counter.items()))
        for counter in members.values()
    ))


def canonical_context_reference(serialized: list[Any]) -> tuple[tuple[tuple[str, int], ...], ...]:
    """Read either the old ordered first-seen list or the canonical partition."""
    if not serialized:
        return tuple()
    first = serialized[0]
    if isinstance(first, (list, tuple)) and len(first) == 2 and isinstance(first[0], str):
        # Existing verified preflight manifests store [fingerprint, first-seen-label]
        # per App call. Reconstruct the exact sharing partition without depending
        # on their call sequence or opaque labels.
        members: dict[str, collections.Counter[str]] = {}
        for fingerprint, label in serialized:
            members.setdefault(str(label), collections.Counter())[str(fingerprint)] += 1
        return tuple(sorted(
            tuple(sorted((fingerprint, count) for fingerprint, count in counter.items()))
            for counter in members.values()
        ))
    # New manifests store a list of context signatures, each a list of
    # [fingerprint, multiplicity] pairs.
    return tuple(sorted(
        tuple(sorted((str(pair[0]), int(pair[1])) for pair in context))
        for context in serialized
    ))


def cache_dir_for(stage: dict[str, Any], suite: str, output_root: Path) -> Path:
    if suite == "java":
        return (output_root / "temp" / suite / stage["key"]
                / f"clang_ast_exe_{getpass.getuser()}" / comparison.cache_namespace(stage))
    return (output_root / "cache" / suite / stage["key"]
            / "@specs-feup/clava" / comparison.cache_namespace(stage))


def base_environment(stage: dict[str, Any], suite: str, mode: str, repeat: int,
                     run_dir: Path, metrics: Path, temp_root: Path, overlay: Path,
                     output_root: Path, include_audit: bool = False) -> dict[str, str]:
    env = os.environ.copy()
    env.update({
        "TMPDIR": str(temp_root), "TMP": str(temp_root), "TEMP": str(temp_root),
        "XDG_CACHE_HOME": str(output_root / "cache" / suite / stage["key"]),
        "APP_BUILD_METRICS_PATH": str(metrics),
        "APP_BUILD_SUITE": suite,
        "APP_BUILD_STAGE": stage["key"],
        "APP_BUILD_MODE": mode,
        "APP_BUILD_REPEAT": str(repeat),
        "APP_BUILD_STAGE_ROOT": str(stage["root"]),
        "APP_BUILD_OVERLAY_JAR": str(overlay.resolve()),
        "APP_BUILD_ENVIRONMENT_MARKER": str(run_dir / "environment-startup.jsonl"),
        "SPECS_JAVA_LIBS_HOME": str((stage["root"] / "specs-java-libs").resolve()),
        "LARA_FRAMEWORK_HOME": str((stage["root"] / "lara-framework").resolve()),
        "JAVA_TOOL_OPTIONS": f"-Djava.io.tmpdir={temp_root} -Dclava.astWire={stage['wire']}",
    })
    for name in ("JDK_JAVA_OPTIONS", "_JAVA_OPTIONS", "DEADLINE_JFR_PATH", "DEADLINE_JFR_SETTINGS"):
        env.pop(name, None)
    if stage["key"] == "flatbuffers":
        env["FLAT_NATIVE"] = str(stage["native_root"])
    else:
        env.pop("FLAT_NATIVE", None)
    if include_audit:
        env["APP_BUILD_AUDIT_INCLUDES"] = "true"
    else:
        env.pop("APP_BUILD_AUDIT_INCLUDES", None)
    return env


def write_js_config(run_dir: Path, stage: dict[str, Any], runtime: Path) -> Path:
    """Create a Vitest config with an environment shim that prepends the overlay."""
    clava = stage["clava"]
    helper = clava.parent / "node_modules/@specs-feup/lara/vitest/weaverVitestConfig.ts"
    environment_module = clava.parent / "node_modules/@specs-feup/lara/vitest/weaverEnvironment.ts"
    # Resolve from Lara's real source file, not from the stage root: workspace
    # symlinks can make `java` resolve to a shared outer node_modules package.
    resolve_java = (
        'import { createRequire } from "node:module"; import fs from "node:fs"; '
        f'const environment = fs.realpathSync({json.dumps(str(environment_module))}); '
        'process.stdout.write(createRequire(environment).resolve("java"));'
    )
    resolved_java = subprocess.run(
        ["node", "--input-type=module", "-e", resolve_java],
        check=True, capture_output=True, text=True,
    ).stdout
    java_module = Path(resolved_java).resolve()
    shim = run_dir / "appBuildWeaverEnvironment.ts"
    shim.write_text(
        'import fs from "node:fs";\n'
        f'import java from "{java_module.as_uri()}";\n'
        f'import base from "{environment_module.as_uri()}";\n'
        'const environment = { ...base, async setup(global, options) {\n'
        '  const overlay = process.env.APP_BUILD_OVERLAY_JAR;\n'
        '  if (!overlay) throw new Error("APP_BUILD_OVERLAY_JAR is required");\n'
        '  const marker = process.env.APP_BUILD_ENVIRONMENT_MARKER;\n'
        '  fs.appendFileSync(marker, JSON.stringify({event: "setup-entry", jvmCreated: java.isJvmCreated(), overlay}) + "\\n");\n'
        '  if (java.isJvmCreated()) throw new Error("Java VM started before app-build overlay registration");\n'
        '  java.registerClient(() => {\n'
        '    if (!java.classpath.includes(overlay)) java.classpath.unshift(overlay);\n'
        '    fs.appendFileSync(marker, JSON.stringify({event: "before-jvm", classpath: java.classpath.slice(0, 8)}) + "\\n");\n'
        '    console.error("APP_BUILD_OVERLAY_PREPENDED " + overlay);\n'
        '  }, null);\n'
        '  const result = await base.setup(global, options);\n'
        '  fs.appendFileSync(marker, JSON.stringify({event: "setup-complete", jvmCreated: java.isJvmCreated(), classpath: java.classpath.slice(0, 8)}) + "\\n");\n'
        '  return result;\n'
        '} };\n'
        'export default environment;\n'
    )
    side_effect_imports = [
        (clava / "Clava-JS/api/Joinpoints.ts").as_uri(),
        (clava / "Clava-JS/code/sideEffects.ts").as_uri(),
    ]
    config = run_dir / "vitest.app-build.config.ts"
    config.write_text(
        f'import {{ createWeaverVitestConfig }} from "{helper.as_uri()}";\n'
        f'import {{ weaverConfig }} from "{(clava / "Clava-JS/code/WeaverConfiguration.ts").as_uri()}";\n'
        f'const baseConfig = createWeaverVitestConfig({{ ...weaverConfig, '
        f'jarPath: {json.dumps(str(runtime))}, '
        f'importForSideEffects: {json.dumps(side_effect_imports)} }});\n'
        f'export default {{ ...baseConfig, '
        f'test: {{ ...baseConfig.test, environment: {json.dumps(str(shim))} }}, '
        f'root: {json.dumps(str(clava / "Clava-JS"))} }};\n'
    )
    return config


def write_gradle_overlay_init(run_dir: Path) -> Path:
    init = run_dir / "app-build-overlay.init.gradle"
    init.write_text(
        "import groovy.json.JsonOutput\n"
        "import org.gradle.api.tasks.testing.Test\n"
        "import org.gradle.testing.jacoco.plugins.JacocoTaskExtension\n"
        "gradle.afterProject { project ->\n"
        "  if (project.name == 'ClangAstParser') {\n"
        "    project.tasks.named('test', Test) { testTask ->\n"
        "      def jacoco = testTask.extensions.findByType(JacocoTaskExtension)\n"
        "      if (jacoco == null) throw new GradleException('Missing JaCoCo extension')\n"
        "      jacoco.enabled = false\n"
        "      testTask.maxHeapSize = '512m'\n"
        "      classpath = files(System.getenv('APP_BUILD_OVERLAY_JAR')) + classpath\n"
        "      testTask.doFirst {\n"
        "        def workerArgs = testTask.allJvmArgs\n"
        "        def record = [jacoco_enabled: jacoco.enabled, max_heap_size: testTask.maxHeapSize,\n"
        "          jvm_args: workerArgs, javaagent_args: workerArgs.findAll { it.startsWith('-javaagent:') }]\n"
        "        logger.lifecycle('APP_BUILD_TEST_WORKER ' + JsonOutput.toJson(record))\n"
        "      }\n"
        "    }\n"
        "  }\n"
        "}\n"
    )
    return init


def check_test_counts(suite: str, run_dir: Path, process_code: int) -> dict[str, Any]:
    if suite == "clava-js":
        report_path = run_dir / "vitest.json"
        report = json.loads(report_path.read_text()) if report_path.is_file() else {}
        counts = comparison.js_counts(report)
    else:
        result_root = run_dir / "junit-results"
        counts = comparison.java_counts(result_root)
    expected = EXPECTED_TESTS[suite]
    counts_only = {key: counts.get(key) for key in expected}
    if process_code != 0 or counts_only != expected:
        raise RuntimeError(f"suite failed fixed test contract for {suite}: rc={process_code}, counts={counts_only}, expected={expected}, run={run_dir}")
    return counts


def js_test_identities(report: dict[str, Any]) -> list[dict[str, str]]:
    identities: list[dict[str, str]] = []
    for test_file in report.get("testResults", []):
        raw_path = str(test_file.get("name", ""))
        anchor = "/Clava-JS/"
        relative_path = raw_path.split(anchor, 1)[1] if anchor in raw_path else Path(raw_path).name
        for assertion in test_file.get("assertionResults", []):
            identities.append({
                "file": relative_path,
                "test": str(assertion.get("fullName") or assertion.get("title") or ""),
                "status": str(assertion.get("status", "unknown")).lower(),
            })
    return sorted(identities, key=lambda item: (item["file"], item["test"], item["status"]))


def reference_include_audit_identities(plan_path: Path) -> list[dict[str, str]]:
    plan = json.loads(plan_path.read_text())
    baseline = next((item for item in plan.get("results", [])
                     if item.get("suite") == "clava-js" and item.get("stage") == "before-cache"), None)
    if baseline is None:
        raise RuntimeError(f"reference plan has no baseline Clava-JS full-suite result: {plan_path}")
    report_path = Path(baseline["run_dir"]) / "vitest.json"
    report = json.loads(report_path.read_text())
    selected = [row for row in js_test_identities(report)
                if row["file"] == INCLUDE_AUDIT_TEST_FILE
                and row["test"] in {"CxxTest Wrap", "CxxTest FileRebuild", "CxxTest CloneOnFile"}]
    expected = {
        (INCLUDE_AUDIT_TEST_FILE, "CxxTest Wrap", "passed"),
        (INCLUDE_AUDIT_TEST_FILE, "CxxTest FileRebuild", "passed"),
        (INCLUDE_AUDIT_TEST_FILE, "CxxTest CloneOnFile", "passed"),
    }
    actual = {(row["file"], row["test"], row["status"]) for row in selected}
    if actual != expected or len(selected) != len(expected):
        raise RuntimeError(f"reference suite identity filter differs from the exact focused cases: {selected}")
    return selected


def check_include_audit_identities(run_dir: Path, expected: list[dict[str, str]], process_code: int) -> list[dict[str, str]]:
    report = json.loads((run_dir / "vitest.json").read_text())
    all_rows = js_test_identities(report)
    names = {row["test"] for row in expected}
    selected = [row for row in all_rows if row["file"] == INCLUDE_AUDIT_TEST_FILE and row["test"] in names]
    if process_code != 0 or selected != expected:
        raise RuntimeError(
            f"focused include-audit test identities differ: rc={process_code}, actual={selected}, expected={expected}"
        )
    return selected


def execute_cell(stage: dict[str, Any], suite: str, mode: str, repeat: int,
                 output_root: Path, ordinal: int, measured: bool,
                 include_audit_expected: list[dict[str, str]] | None = None) -> dict[str, Any]:
    include_audit = include_audit_expected is not None
    phase_dir = "include-audit" if include_audit else "measured" if measured else "preflight"
    run_dir = output_root / phase_dir / suite / f"{ordinal:03d}-{stage['key']}-{mode}-r{repeat}"
    run_dir.mkdir(parents=True, exist_ok=False)
    metrics = run_dir / "app-calls.jsonl"
    temp_root = output_root / "temp" / suite / stage["key"]
    temp_root.mkdir(parents=True, exist_ok=True)
    overlay = stage["overlay"]["overlay_jar"]
    env = base_environment(stage, suite, mode, repeat, run_dir, metrics, temp_root, overlay, output_root,
                           include_audit=include_audit)
    cache_dir = cache_dir_for(stage, suite, output_root)
    direct_probe = None
    if mode == "direct" and stage["cache_enabled"]:
        direct_probe = comparison.install_direct_ccache_probe(run_dir, env)
    comparison.configure_cache_mode(stage, mode, measured, env, cache_dir)

    if suite == "clava-js":
        clava = stage["clava"]
        config = write_js_config(run_dir, stage, stage["runtime"])
        command = [
            "npm", "exec", "--workspace", "@specs-feup/clava", "--", "vitest", "run",
            "--config", str(config), "--reporter=json", "--outputFile", str(run_dir / "vitest.json"),
            "-t", INCLUDE_AUDIT_TEST_FILTER if include_audit else comparison.JS_TEST_FILTER,
        ]
        cwd = clava / "Clava-JS"
    else:
        clava = stage["clava"]
        init = SCRIPT_ROOT / "java-suite.init.gradle"
        overlay_init = write_gradle_overlay_init(run_dir)
        command = ["gradle", "--no-daemon", "--offline", "--init-script", str(init),
                   "--init-script", str(overlay_init)]
        if stage["key"] == "protobuf":
            command += [f"-PclangDumperRoot={stage['native_root']}"]
        command += ["-p", "ClangAstParser", "test"]
        cwd = clava
        old_test_results = clava / "ClangAstParser/build/test-results/test"
        if old_test_results.exists():
            shutil.rmtree(old_test_results)

    (run_dir / "command.json").write_text(json.dumps({"argv": command, "cwd": str(cwd)}, indent=2) + "\n")
    log_path = run_dir / "run.log"
    started = time.perf_counter()
    with log_path.open("w") as log:
        process = subprocess.run(command, cwd=cwd, env=env, stdout=log,
                                 stderr=subprocess.STDOUT, check=False)
    wall_s = time.perf_counter() - started
    if suite == "java":
        result_root = clava / "ClangAstParser/build/test-results/test"
        archived_results = run_dir / "junit-results"
        if result_root.is_dir():
            shutil.copytree(result_root, archived_results)
    selected_test_identity = None
    if include_audit:
        selected_test_identity = check_include_audit_identities(
            run_dir, include_audit_expected, process.returncode)
        counts = {
            "total_tests": len(selected_test_identity),
            "passed_tests": sum(row["status"] == "passed" for row in selected_test_identity),
            "failed_tests": 0,
            "skipped_tests": sum(row["status"] in {"skipped", "pending", "todo"} for row in selected_test_identity),
            "failure_names": [],
            "identity_scope": "three named tests selected from the original 164-test suite identity",
        }
    else:
        counts = check_test_counts(suite, run_dir, process.returncode)
    cache = comparison.cache_validation(stage, mode, measured, cache_dir, direct_probe)
    if not cache["passed"]:
        raise RuntimeError(f"cache-state gate failed for {stage['key']}/{suite}/{mode}: {cache}")
    rows = enrich_calls(metrics, suite, stage["key"], mode, repeat, stage, temp_root)
    validate_capture_rows(rows, overlay, suite, stage["key"])
    if include_audit:
        missing_audit = [index for index, row in enumerate(rows)
                         if row.get("elapsed_ms") is not None
                         and ("include_directory_audit" not in row or "source_parent_audit" not in row)]
        if missing_audit:
            raise RuntimeError(f"include audit metadata missing from captured rows {missing_audit[:8]} in {run_dir}")
    for row in rows:
        row["source_count"] = len(row.get("resolved_sources", []))
    if not rows:
        raise RuntimeError(f"overlay emitted no parser-call records for {suite}/{stage['key']} in {run_dir}")
    worker_record = None
    if suite == "java":
        marker = "APP_BUILD_TEST_WORKER "
        worker_lines = [line.partition(marker)[2] for line in log_path.read_text(errors="replace").splitlines() if marker in line]
        if len(worker_lines) != 1:
            raise RuntimeError(f"expected one actual Java Test-worker argv record, saw {len(worker_lines)} in {log_path}")
        worker_record = json.loads(worker_lines[0])
        if worker_record.get("jacoco_enabled") or worker_record.get("javaagent_args"):
            raise RuntimeError(f"Java worker is not agent-off: {worker_record}")
    (run_dir / "app-calls.enriched.jsonl").write_text("".join(json.dumps(row, sort_keys=True) + "\n" for row in rows))
    result = {
        "suite": suite, "stage": stage["key"], "mode": mode, "repeat": repeat,
        "measured": measured, "valid": True, "return_code": process.returncode,
        "wall_s": wall_s, "test_counts": counts, "cache_validation": cache,
        "call_count": len(rows),
        "syntax_only_calls": sum(bool(row.get("syntax_only")) or row.get("excluded_reason") == "syntax_only" for row in rows),
        "app_calls": sum(row.get("elapsed_ms") is not None for row in rows),
        "app_returned_null": sum(bool(row.get("app_returned_null")) for row in rows if row.get("elapsed_ms") is not None),
        "app_elapsed_ms": sum(float(row["elapsed_ms"]) for row in rows if row.get("elapsed_ms") is not None),
        "source_count": sum(int(row.get("source_count", 0)) for row in rows if row.get("elapsed_ms") is not None),
        "context_pattern": context_pattern(rows),
        "test_identity": selected_test_identity,
        "test_identity_sha256": digest(selected_test_identity) if selected_test_identity is not None else None,
        "java_worker_configuration": worker_record,
        "metrics": str((run_dir / "app-calls.enriched.jsonl").resolve()),
        "run_dir": str(run_dir.resolve()),
    }
    (run_dir / "summary.json").write_text(json.dumps(result, indent=2) + "\n")
    return result


def build_overlays(stages: dict[str, dict[str, Any]], output_root: Path,
                   selected_keys: tuple[str, ...]) -> None:
    sys.path.insert(0, str(APP_BUILD_ROOT))
    from build_overlay import build_overlay

    for key in selected_keys:
        stage = stages[key]
        result = build_overlay(key, stage["clava"], stage["runtime_lib"], output_root / "overlays" / key)
        overlay_path = Path(result["overlay_jar"]).resolve()
        provenance_path = Path(result["provenance"]).resolve()
        if not overlay_path.is_file() or not provenance_path.is_file():
            raise RuntimeError(f"overlay builder returned missing artifacts for {key}: {result}")
        stage["overlay"] = {
            "overlay_jar": overlay_path,
            "overlay_sha256": sha256_file(overlay_path),
            "provenance": str(provenance_path),
            "provenance_sha256": sha256_file(provenance_path),
        }


def attach_include_audit_overlays(stages: dict[str, dict[str, Any]], overlay_root: Path,
                                  selected_keys: tuple[str, ...]) -> None:
    """Use the separately built, metadata-only include-audit helper overlays."""
    for key in selected_keys:
        stage_root = overlay_root / key
        overlay_path = (stage_root / "overlay.jar").resolve()
        provenance_path = (stage_root / "provenance.json").resolve()
        if not overlay_path.is_file() or not provenance_path.is_file():
            raise FileNotFoundError(f"missing include-audit overlay/provenance for {key}: {stage_root}")
        stage = stages[key]
        stage["overlay"] = {
            "overlay_jar": overlay_path,
            "overlay_sha256": sha256_file(overlay_path),
            "provenance": str(provenance_path),
            "provenance_sha256": sha256_file(provenance_path),
        }


def attach_existing_measurement_overlays(stages: dict[str, dict[str, Any]],
                                         existing_manifest: dict[str, Any]) -> None:
    """Reuse the already-built isolated overlays when resuming a partial matrix."""
    for key in STAGE_KEYS:
        recorded = existing_manifest.get("stages", {}).get(key)
        if not isinstance(recorded, dict):
            raise RuntimeError(f"partial measurement lacks frozen stage metadata for {key}")
        stage = stages[key]
        for field in ("root", "dumper", "wire", "cache_enabled", "parser_jar_sha256"):
            value = stage[field]
            actual = str(value) if isinstance(value, Path) else value
            expected = str(recorded.get(field)) if isinstance(value, Path) else recorded.get(field)
            if actual != expected:
                raise RuntimeError(f"frozen stage identity changed for {key}/{field}: {actual} != {expected}")
        if recorded.get("native_tool_sha256") != sha256_file(stage["dumper"]):
            raise RuntimeError(f"frozen native tool changed for {key} since partial measurement")
        overlay = recorded.get("overlay", {})
        overlay_path = Path(str(overlay.get("overlay_jar", ""))).resolve()
        provenance_path = Path(str(overlay.get("provenance", ""))).resolve()
        if not overlay_path.is_file() or sha256_file(overlay_path) != overlay.get("overlay_sha256"):
            raise RuntimeError(f"existing measurement overlay missing or changed for {key}: {overlay_path}")
        if not provenance_path.is_file() or sha256_file(provenance_path) != overlay.get("provenance_sha256"):
            raise RuntimeError(f"existing overlay provenance missing or changed for {key}: {provenance_path}")
        stage["overlay"] = {
            "overlay_jar": overlay_path,
            "overlay_sha256": overlay["overlay_sha256"],
            "provenance": str(provenance_path),
            "provenance_sha256": overlay["provenance_sha256"],
        }


def java_test_identities(run_dir: Path) -> list[dict[str, str]]:
    identities: list[dict[str, str]] = []
    for xml_path in sorted((run_dir / "junit-results").glob("*.xml")):
        root = ET.parse(xml_path).getroot()
        for test in root.findall("testcase"):
            if test.find("failure") is not None or test.find("error") is not None:
                status = "failed"
            elif test.find("skipped") is not None:
                status = "skipped"
            else:
                status = "passed"
            identities.append({
                "class": str(test.attrib.get("classname", "")),
                "test": str(test.attrib.get("name", "")),
                "status": status,
            })
    return sorted(identities, key=lambda item: (item["class"], item["test"], item["status"]))


def completed_result_from_summary(summary_path: Path, stages: dict[str, dict[str, Any]],
                                  expected_identity_sha: str,
                                  expected_identity_count: int) -> dict[str, Any]:
    """Revalidate one completed measured cell without rerunning its suite."""
    summary = json.loads(summary_path.read_text())
    suite = summary.get("suite")
    stage_key = summary.get("stage")
    run_dir = summary_path.parent.resolve()
    if (summary.get("measured") is not True or summary.get("valid") is not True
            or summary.get("return_code") != 0 or summary.get("repeat") != 1
            or summary.get("run_dir") != str(run_dir)):
        raise RuntimeError(f"partial round cell is not a successful measured run: {summary_path}")
    if suite not in SUITES or stage_key not in STAGE_KEYS:
        raise RuntimeError(f"partial round cell has an unknown suite/stage: {summary_path}")
    metrics_path = Path(str(summary.get("metrics", ""))).resolve()
    if metrics_path != (run_dir / "app-calls.enriched.jsonl").resolve() or not metrics_path.is_file():
        raise RuntimeError(f"partial round metrics path is missing or escaped its run directory: {summary_path}")
    raw_metrics = run_dir / "app-calls.jsonl"
    if not raw_metrics.is_file():
        raise RuntimeError(f"partial round raw metrics are missing: {raw_metrics}")
    stage = stages[stage_key]
    rows = load_calls({"metrics": str(metrics_path)})
    temp_root = run_dir.parents[2] / "temp" / suite / stage_key
    raw_rows = enrich_calls(raw_metrics, suite, stage_key, summary["mode"], 1, stage, temp_root)
    for row in raw_rows:
        row["source_count"] = len(row.get("resolved_sources", []))
    if rows != raw_rows:
        raise RuntimeError(f"enriched capture differs from raw parser rows: {summary_path}")
    validate_capture_rows(rows, stage["overlay"]["overlay_jar"], suite, stage_key)
    if not rows or summary.get("cache_validation", {}).get("passed") is not True:
        raise RuntimeError(f"partial round cell failed parser/cache metadata validation: {summary_path}")
    if summary.get("call_count") != len(rows):
        raise RuntimeError(f"partial round call count changed: {summary_path}")
    app_rows = [row for row in rows if row.get("elapsed_ms") is not None]
    if (summary.get("app_calls") != len(app_rows)
            or summary.get("app_returned_null") != sum(bool(row.get("app_returned_null")) for row in app_rows)
            or not math.isclose(float(summary.get("app_elapsed_ms", -1)),
                                sum(float(row["elapsed_ms"]) for row in app_rows),
                                rel_tol=1e-12, abs_tol=1e-9)):
        raise RuntimeError(f"partial round App timing summary disagrees with its rows: {summary_path}")
    counts = check_test_counts(suite, run_dir, 0)
    if {key: counts.get(key) for key in EXPECTED_TESTS[suite]} != EXPECTED_TESTS[suite]:
        raise RuntimeError(f"partial round test counts changed: {summary_path}")
    if suite == "clava-js":
        identities = js_test_identities(json.loads((run_dir / "vitest.json").read_text()))
    else:
        identities = java_test_identities(run_dir)
    if len(identities) != expected_identity_count or digest(identities) != expected_identity_sha:
        raise RuntimeError(f"partial round test identity differs from frozen suite contract: {summary_path}")
    summary.update({
        "test_identity": identities,
        "test_identity_sha256": digest(identities),
        "context_pattern": context_pattern(rows),
        "recovered_without_rerun": True,
    })
    return summary


def js_serial_file_order_evidence(preflight_path: Path, round1_results: list[dict[str, Any]],
                                  stages: dict[str, dict[str, Any]]) -> dict[str, Any]:
    """Record why ordered call labels drifted while serial sharing stayed fixed."""
    preflight = json.loads(preflight_path.read_text())
    baseline = next((row for row in preflight["results"]
                     if row.get("suite") == "clava-js" and row.get("stage") == "before-cache"), None)
    measured = next((row for row in round1_results
                     if row.get("suite") == "clava-js" and row.get("stage") == "before-cache"), None)
    if baseline is None or measured is None:
        raise RuntimeError("cannot establish the sequential JS file-order audit")
    config_source = stages["before-cache"]["root"] / "node_modules/@specs-feup/lara/vitest/weaverVitestConfig.ts"
    if not config_source.is_file() or not re.search(r"fileParallelism\s*:\s*false", config_source.read_text()):
        raise RuntimeError(f"frozen Vitest config does not prove serial file execution: {config_source}")
    test_sources = [
        stages["before-cache"]["clava"] / "Clava-JS/api/LegacyIntegrationTests - C.test.ts",
        stages["before-cache"]["clava"] / "Clava-JS/api/LegacyIntegrationTests - CXX.test.ts",
    ]
    concurrent_test_pattern = re.compile(r"\b(?:it|test|describe|suite)\s*\.\s*concurrent\b")
    if any(not path.is_file() or concurrent_test_pattern.search(path.read_text()) for path in test_sources):
        raise RuntimeError("frozen Clava JS C/CXX test sources are missing or declare concurrent tests")

    def sequence(run_dir: Path) -> list[dict[str, Any]]:
        report = json.loads((run_dir / "vitest.json").read_text())
        entries = []
        for row in report.get("testResults", []):
            name = str(row.get("name", ""))
            for suffix in ("LegacyIntegrationTests - C.test.ts", "LegacyIntegrationTests - CXX.test.ts"):
                if name.endswith(suffix):
                    entries.append({"file": suffix, "start_ms": row.get("startTime"), "end_ms": row.get("endTime")})
        entries.sort(key=lambda row: row["start_ms"])
        if len(entries) != 2 or any(not isinstance(row["start_ms"], (int, float))
                                    or not isinstance(row["end_ms"], (int, float)) for row in entries):
            raise RuntimeError(f"Vitest report lacks C/CXX file timing evidence in {run_dir}")
        if entries[0]["end_ms"] > entries[1]["start_ms"]:
            raise RuntimeError(f"C/CXX test files overlapped despite serial-file config: {entries}")
        return entries

    preflight_order = sequence(Path(str(baseline["run_dir"])))
    measured_order = sequence(Path(str(measured["run_dir"])))
    if [row["file"] for row in preflight_order] == [row["file"] for row in measured_order]:
        raise RuntimeError("round-1 JS C/CXX execution order did not reproduce the observed order drift")
    preflight_rows = load_calls({"metrics": str(baseline["metrics"])})
    measured_rows = load_calls(measured)
    return {
        "file_parallelism": False,
        "ordinary_awaited_tests_no_concurrent_declarations": True,
        "test_config_source": str(config_source),
        "test_config_source_sha256": sha256_file(config_source),
        "test_source_sha256_by_file": {path.name: sha256_file(path) for path in test_sources},
        "preflight_serial_file_order": preflight_order,
        "measurement_serial_file_order": measured_order,
        "file_intervals_overlap": False,
        "ordered_context_labels_differ": True,
        "canonical_context_partition_sha256_preflight": digest(context_pattern(preflight_rows)),
        "canonical_context_partition_sha256_measurement": digest(context_pattern(measured_rows)),
        "conclusion": "C/CXX files ran sequentially in a different order; the App-context sharing partition is unchanged, so only ordered first-seen labels differed.",
    }


def parser_class_digest(jar_path: Path) -> str:
    import zipfile

    entry = "pt/up/fe/specs/clang/codeparser/ParallelCodeParser.class"
    with zipfile.ZipFile(jar_path) as archive:
        return hashlib.sha256(archive.read(entry)).hexdigest()


def verify_include_audit_parser_classes(stages: dict[str, dict[str, Any]],
                                        reference_manifest: Path) -> dict[str, str]:
    reference_root = reference_manifest.resolve().parent
    records: dict[str, str] = {}
    for key in STAGE_KEYS:
        previous = reference_root / "overlays" / key / "overlay.jar"
        current = Path(stages[key]["overlay"]["overlay_jar"])
        previous_digest = parser_class_digest(previous)
        current_digest = parser_class_digest(current)
        if current_digest != previous_digest:
            raise RuntimeError(
                f"include-audit parser bytecode changed for {key}: {previous_digest} != {current_digest}"
            )
        records[key] = current_digest
    return records


def workload_counter(rows: list[dict[str, Any]]) -> collections.Counter[str]:
    return collections.Counter(
        row["group_id"] for row in rows
        if row.get("elapsed_ms") is not None
        and not row.get("syntax_only")
        and row.get("excluded_reason") != "syntax_only"
    )


def load_calls(result: dict[str, Any]) -> list[dict[str, Any]]:
    return [json.loads(line) for line in Path(result["metrics"]).read_text().splitlines() if line.strip()]


def compare_workloads(results: list[dict[str, Any]], reference: dict[str, collections.Counter[str]],
                      context_refs: dict[str, Any]) -> None:
    for result in results:
        key = f"{result['suite']}:{result['stage']}:{result['mode']}:{result['repeat']}"
        rows = load_calls(result)
        actual = workload_counter(rows)
        suite = result["suite"]
        if suite not in reference:
            reference[suite] = actual
            context_refs[suite] = context_pattern(rows)
            continue
        if actual != reference[suite]:
            missing = list((reference[suite] - actual).elements())[:4]
            extra = list((actual - reference[suite]).elements())[:4]
            raise RuntimeError(f"parser-call workload changed in {key}; missing={missing}, extra={extra}")
        if context_pattern(rows) != context_refs[suite]:
            raise RuntimeError(f"ClavaContext sharing pattern changed in {key}")


def write_rows(results: list[dict[str, Any]], output_root: Path) -> None:
    rows: list[dict[str, Any]] = []
    fields = ["suite", "stage", "mode", "repeat", "group_id", "elapsed_ms", "valid", "app_returned_null", "source_count"]
    for result in results:
        for call in load_calls(result):
            if call.get("elapsed_ms") is None:
                continue
            rows.append({
                "suite": result["suite"], "stage": result["stage"], "mode": result["mode"],
                "repeat": result["repeat"], "group_id": call["group_id"],
                "elapsed_ms": call["elapsed_ms"], "valid": result["valid"],
                "app_returned_null": call.get("app_returned_null"),
                "source_count": call.get("source_count", 0),
            })
    path = output_root / "app-build-calls.csv"
    with path.open("w", newline="") as output:
        writer = csv.DictWriter(output, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)


def write_report_payload(results: list[dict[str, Any]], output_path: Path) -> None:
    rows = []
    groups: dict[tuple[str, str], dict[str, Any]] = {}
    for result in results:
        for call in load_calls(result):
            if call.get("elapsed_ms") is None:
                continue
            rows.append({
                "suite": result["suite"], "stage": result["stage"], "mode": result["mode"],
                "repeat": result["repeat"], "group_id": call["group_id"],
                "group_fingerprint": call["group_fingerprint"],
                "elapsed_ms": call["elapsed_ms"], "valid": result["valid"],
                "app_returned_null": call.get("app_returned_null"),
                "source_count": call.get("source_count", 0),
                "context_id": call.get("context_id"),
                "context_first_ordinal": call.get("context_first_ordinal"),
                "jvm_input_arguments": call.get("jvm_input_arguments"),
                "jvm_max_memory_bytes": call.get("jvm_max_memory_bytes"),
            })
            key = (result["suite"], call["group_id"])
            groups.setdefault(key, {
                "suite": result["suite"], "group_id": call["group_id"],
                "group_fingerprint": call["group_fingerprint"],
                "normalized_workload": call["normalized_workload"],
                "source_count": call.get("source_count", 0),
            })
    payload = {
        "schema_version": 1,
        "task": "Clava App construction matrix",
        "timing_boundary": "ParallelCodeParser.parse (3-arg) entry to completed App",
        "repeat_count": 4,
        "syntax_only_timed": False,
        "forced_gc": False,
        "natural_gc_included": True,
        "startup_included": False,
        "rows": rows,
        "groups": list(groups.values()),
    }
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_text(json.dumps(payload, indent=2) + "\n")


def recover_verified_round1(output_root: Path, stages: dict[str, dict[str, Any]],
                            preflight_path: Path, preflight: dict[str, Any]) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    expected = [(suite, item["key"], mode, 1) for suite, mode, item in schedule(1)]
    found: dict[tuple[str, str, str, int], Path] = {}
    for summary_path in (output_root / "measured").glob("**/summary.json"):
        summary = json.loads(summary_path.read_text())
        if summary.get("measured") is not True or summary.get("repeat") != 1:
            continue
        key = (str(summary.get("suite")), str(summary.get("stage")),
               str(summary.get("mode")), int(summary.get("repeat")))
        if key in found:
            raise RuntimeError(f"duplicate existing measured cell for round 1: {key}")
        found[key] = summary_path
    if set(found) != set(expected):
        raise RuntimeError(
            f"existing output does not contain exactly the 20 round-1 cells: "
            f"missing={sorted(set(expected) - set(found))}, extra={sorted(set(found) - set(expected))}"
        )
    results = []
    hashes: dict[str, dict[str, str]] = {}
    for suite, stage, mode, repeat in expected:
        result = completed_result_from_summary(
            found[(suite, stage, mode, repeat)], stages,
            str(preflight["test_identity_sha256_by_suite"][suite]),
            int(preflight["test_identity_count_by_suite"][suite]),
        )
        if workload_counter(load_calls(result)) != collections.Counter(preflight["workload_counters"][suite]):
            raise RuntimeError(f"existing round-1 workload does not match preflight: {suite}/{stage}/{mode}")
        results.append(result)
        run_dir = Path(result["run_dir"])
        hashes[str(run_dir.relative_to(output_root))] = {
            name: sha256_file(run_dir / name)
            for name in ("app-calls.jsonl", "app-calls.enriched.jsonl", "summary.json")
        }
    workload_refs = {
        suite: collections.Counter(preflight["workload_counters"][suite]) for suite in SUITES
    }
    context_refs = {
        suite: canonical_context_reference(preflight["context_patterns"][suite]) for suite in SUITES
    }
    compare_workloads(results, workload_refs, context_refs)
    warm_seed_records = {}
    for stage in STAGE_KEYS[1:]:
        for suite in SUITES:
            seed_dir = output_root / "cache-seeds" / suite / stage
            if not seed_dir.is_dir() or not any(seed_dir.iterdir()):
                raise RuntimeError(f"existing warm-cache snapshot missing or empty: {seed_dir}")
            warm_seed_records[f"{suite}/{stage}"] = {
                "path": str(seed_dir.resolve()),
                "entry_count": sum(1 for item in seed_dir.rglob("*") if item.is_file()),
            }
    return results, {
        "algorithm": "verified-round1-recovery-r1",
        "reused_measured_cells": len(results),
        "suite_invocations_during_recovery": 0,
        "raw_cell_sha256_by_run": hashes,
        "warm_cache_snapshots_reused_without_reseed": warm_seed_records,
        "js_serial_file_order_audit": js_serial_file_order_evidence(preflight_path, results, stages),
        "context_gate": "canonical per-context multiset partition; raw capture rows and their order are unchanged",
    }


def main() -> int:
    args = parse_args()
    frozen_root = args.frozen_root.resolve()
    output_root = args.output_root.resolve() if args.output_root else DEFAULT_OUTPUT_ROOT / args.phase
    if args.phase == "measure" and not args.host_lock_confirmed:
        raise SystemExit("measure phase requires --host-lock-confirmed after parent approval")
    plan = read_plan(frozen_root)
    stages = stage_records(plan)
    existing_manifest = None
    if args.resume_verified_round1:
        plan_path = output_root / "plan.json"
        if not output_root.is_dir() or not plan_path.is_file():
            raise RuntimeError(f"resume requires the existing partial measurement output: {output_root}")
        existing_manifest = json.loads(plan_path.read_text())
        expected_frozen_sha = sha256_file(frozen_root / "enriched-final-verified/js-results.json")
        if (existing_manifest.get("phase") != "measure"
                or existing_manifest.get("frozen_root") != str(frozen_root)
                or existing_manifest.get("frozen_plan_sha256") != expected_frozen_sha
                or existing_manifest.get("repeat_count") != args.repeat_count
                or existing_manifest.get("completed_rounds") not in (None, 0)
                or existing_manifest.get("results") not in (None, [])):
            raise RuntimeError("existing output is not the expected unaccepted round-1 partial matrix")
        if not existing_manifest.get("host_lock_confirmed"):
            raise RuntimeError("existing measurement partial does not record host-lock authorization")
    else:
        output_root.mkdir(parents=True, exist_ok=False)
    selected_keys = args.selected_stage_keys
    if args.phase == "include-audit":
        attach_include_audit_overlays(stages, args.include_audit_overlay_root.resolve(), selected_keys)
    elif args.resume_verified_round1:
        attach_existing_measurement_overlays(stages, existing_manifest)
    else:
        build_overlays(stages, output_root, selected_keys)
    include_audit_expected = None
    parser_bytecode_digests = None
    if args.phase == "include-audit":
        include_audit_expected = reference_include_audit_identities(args.preflight_manifest.resolve())
        parser_bytecode_digests = verify_include_audit_parser_classes(stages, args.preflight_manifest)
    manifest: dict[str, Any] = existing_manifest if args.resume_verified_round1 else {
        "schema_version": 1,
        "task": ("focused Clava-JS include-directory provenance audit" if args.phase == "include-audit"
                 else "in-suite Clava App construction timing"),
        "phase": args.phase,
        "frozen_root": str(frozen_root),
        "frozen_plan_sha256": sha256_file(frozen_root / "enriched-final-verified/js-results.json"),
        "repeat_count": None if args.phase == "include-audit" else args.repeat_count,
        "host_lock_confirmed": bool(args.host_lock_confirmed),
        "lock_note": args.lock_note,
        "timing_boundary": "ParallelCodeParser.parse (3-arg) entry to completed App",
        "forced_gc": False,
        "natural_gc_included": True,
        "startup_included": False,
        "excluded_from_timing": ["Gradle/Vitest startup", "JVM startup", "test adapters and assertions", "code generation", "syntax-only calls"],
        "suites": (None if args.phase == "include-audit" else EXPECTED_TESTS),
        "stages": {},
        "results": [],
    }
    if args.phase == "include-audit":
        manifest["reference_identity_manifest"] = str(args.preflight_manifest.resolve())
        manifest["reference_identity_manifest_sha256"] = sha256_file(args.preflight_manifest.resolve())
        manifest["selected_test_filter"] = INCLUDE_AUDIT_TEST_FILTER
        manifest["selected_test_identities"] = include_audit_expected
        manifest["selected_test_identity_sha256"] = digest(include_audit_expected)
        manifest["parser_class_sha256_by_stage"] = parser_bytecode_digests
        manifest["input_audit_policy"] = (
            "raw input_sources order and resolved_sources order retained; normalized input_sources membership sorted "
            "only because ParallelCodeParser sorts user files and source folders before native processing"
        )
    for key in selected_keys:
        stage = stages[key]
        manifest["stages"][key] = {
            "clava_revision": stage.get("clava_revision"),
            "native_revision": stage.get("dumper_revision"),
            "root": str(stage["root"]), "dumper": str(stage["dumper"]),
            "wire": stage["wire"], "cache_enabled": stage["cache_enabled"],
            "parser_jar_sha256": stage["parser_jar_sha256"],
            "native_tool_sha256": sha256_file(stage["dumper"]),
            "overlay": {key: value for key, value in stage["overlay"].items() if key != "overlay_jar"}
            | {"overlay_jar": str(stage["overlay"]["overlay_jar"])},
        }
    if not args.resume_verified_round1:
        (output_root / "plan.json").write_text(json.dumps(manifest, indent=2) + "\n")

    results: list[dict[str, Any]] = []
    ordinal = 0
    reference_workloads: dict[str, collections.Counter[str]] = {}
    reference_context: dict[str, Any] = {}
    if args.phase == "include-audit":
        for key in selected_keys:
            ordinal += 1
            run_dir = output_root / "include-audit" / "clava-js" / f"{ordinal:03d}-{key}-direct-r-1"
            try:
                result = execute_cell(
                    stages[key], "clava-js", "direct", -1, output_root, ordinal, False,
                    include_audit_expected=include_audit_expected,
                )
            except Exception as error:
                manifest.setdefault("attempts", []).append({
                    "suite": "clava-js", "stage": key, "mode": "direct", "repeat": -1,
                    "valid": False, "error": f"{type(error).__name__}: {error}",
                    "run_dir": str(run_dir.resolve()),
                })
                (output_root / "plan.json").write_text(json.dumps(manifest, indent=2) + "\n")
                raise
            result["include_audit"] = True
            manifest["results"].append(result)
            manifest["completed"] = len(manifest["results"])
            (output_root / "plan.json").write_text(json.dumps(manifest, indent=2) + "\n")
            print(f"completed include-audit cell {len(manifest['results'])}/4: clava-js/{key}", flush=True)
        manifest["valid"] = len(manifest["results"]) == 4 and all(row["valid"] for row in manifest["results"])
        manifest["completed"] = len(manifest["results"])
        output = output_root / "include-audit.json"
        output.write_text(json.dumps(manifest, indent=2) + "\n")
        (output_root / "plan.json").write_text(json.dumps(manifest, indent=2) + "\n")
        return 0 if manifest["valid"] else 1

    if args.phase == "preflight":
        for key in selected_keys:
            for suite in ("java", "clava-js"):
                if suite not in args.selected_suites:
                    continue
                ordinal += 1
                run_dir = output_root / "preflight" / suite / f"{ordinal:03d}-{key}-direct-r-1"
                try:
                    result = execute_cell(stages[key], suite, "direct", -1, output_root, ordinal, False)
                except Exception as error:
                    manifest.setdefault("attempts", []).append({
                        "suite": suite, "stage": key, "mode": "direct", "repeat": -1,
                        "valid": False, "error": f"{type(error).__name__}: {error}",
                        "run_dir": str(run_dir.resolve()),
                    })
                    (output_root / "plan.json").write_text(json.dumps(manifest, indent=2) + "\n")
                    raise
                result["preflight"] = True
                results.append(result)
                manifest.setdefault("attempts", []).append(result)
                calls = load_calls(result)
                reference_workloads.setdefault(suite, workload_counter(calls))
                reference_context.setdefault(suite, context_pattern(calls))
                manifest["results"] = results
                manifest["completed"] = len(results)
                (output_root / "plan.json").write_text(json.dumps(manifest, indent=2) + "\n")
                print(f"completed preflight cell {len(results)}/{len(selected_keys) * len(args.selected_suites)}: {suite}/{key}", flush=True)
                if result["app_calls"] == 0:
                    raise RuntimeError(f"no App timings captured in {suite}/{key} preflight")
        compare_workloads(results, {}, {})
        manifest["workload_counters"] = {
            suite: dict(reference_workloads[suite]) for suite in args.selected_suites
        }
        manifest["context_patterns"] = {
            suite: reference_context[suite] for suite in args.selected_suites
        }
        manifest["preflight_complete"] = (
            set(selected_keys) == set(STAGE_KEYS) and set(args.selected_suites) == set(SUITES)
        )
        manifest["valid"] = manifest["preflight_complete"]
        manifest["completed"] = len(results)
        preflight_manifest = output_root / (
            "app-build-preflight.json" if manifest["preflight_complete"] else "partial-preflight.json"
        )
        preflight_manifest.write_text(json.dumps(manifest, indent=2) + "\n")
    else:
        preflight_path = args.preflight_manifest.resolve() if args.preflight_manifest else (
            DEFAULT_OUTPUT_ROOT / "preflight/app-build-preflight.json"
        )
        if not preflight_path.is_file():
            raise RuntimeError(f"measure requires validated preflight manifest: {preflight_path}")
        preflight = json.loads(preflight_path.read_text())
        if not preflight.get("valid"):
            raise RuntimeError(f"preflight manifest is not valid: {preflight_path}")
        reference_workloads = {
            suite: collections.Counter(preflight["workload_counters"][suite]) for suite in SUITES
        }
        reference_context = {
            suite: canonical_context_reference(preflight["context_patterns"][suite])
            for suite in SUITES
        }
        warm_seeds: dict[tuple[str, str], Path] = {}
        manifest["preflight_manifest"] = str(preflight_path)
        manifest["preflight_manifest_sha256"] = sha256_file(preflight_path)
        # Seed one complete representative suite per cached stage/suite and
        # snapshot it. Restore this cache before each measured warm command;
        # do not add a full-suite unmeasured seed before every warm cell.
        if args.resume_verified_round1:
            results, recovery_audit = recover_verified_round1(output_root, stages, preflight_path, preflight)
            for key in STAGE_KEYS[1:]:
                for suite in SUITES:
                    warm_seeds[(suite, key)] = output_root / "cache-seeds" / suite / key
            ordinals = [
                int(match.group(1))
                for phase_dir in (output_root / "preflight", output_root / "measured")
                for path in phase_dir.rglob("*")
                if path.is_dir() and (match := re.match(r"([0-9]{3})-", path.name))
            ]
            ordinal = max(ordinals, default=0)
            manifest["results"] = results
            manifest["completed_rounds"] = 1
            manifest["completed"] = len(results)
            manifest["valid"] = False
            manifest["partial_round_valid"] = True
            manifest["measurement_recovery"] = recovery_audit
            (output_root / "plan.json").write_text(json.dumps(manifest, indent=2) + "\n")
            print("revalidated existing round 1 without reruns: 20/20 cells", flush=True)
            first_repeat = 2
        else:
            for key in STAGE_KEYS[1:]:
                for suite in SUITES:
                    ordinal += 1
                    seed = execute_cell(stages[key], suite, "warm", 0, output_root, ordinal, False)
                    if not seed["valid"]:
                        raise RuntimeError(f"warm cache seed suite invalid: {seed}")
                    cache_dir = cache_dir_for(stages[key], suite, output_root)
                    seed_dir = output_root / "cache-seeds" / suite / key
                    seed_dir.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copytree(cache_dir, seed_dir)
                    warm_seeds[(suite, key)] = seed_dir
            first_repeat = 1
        for repeat in range(first_repeat, args.repeat_count + 1):
            round_results: list[dict[str, Any]] = []
            for suite, mode, item in schedule(repeat):
                key = item["key"]
                stage = stages[key]
                if mode == "warm":
                    cache_dir = cache_dir_for(stage, suite, output_root)
                    if cache_dir.exists():
                        shutil.rmtree(cache_dir)
                    cache_dir.parent.mkdir(parents=True, exist_ok=True)
                    shutil.copytree(warm_seeds[(suite, key)], cache_dir)
                ordinal += 1
                result = execute_cell(stage, suite, mode, repeat, output_root, ordinal, True)
                round_results.append(result)
            compare_workloads(round_results, reference_workloads, reference_context)
            results.extend(round_results)
            manifest["results"] = results
            manifest["completed_rounds"] = repeat
            (output_root / "plan.json").write_text(json.dumps(manifest, indent=2) + "\n")
            print(f"completed App-build measurement round {repeat}/{args.repeat_count}: {len(results)} cells", flush=True)
    manifest["results"] = results
    manifest["completed"] = len(results)
    manifest["valid"] = all(row["valid"] for row in results)
    if args.phase == "measure":
        all_calls = [call for result in results for call in load_calls(result) if call.get("elapsed_ms") is not None]
        manifest["app_call_count"] = len(all_calls)
        manifest["unique_group_count_by_suite"] = {
            suite: len({call["group_fingerprint"] for call in all_calls if call["suite"] == suite})
            for suite in SUITES
        }
        manifest["workload_counters"] = {
            suite: dict(reference_workloads[suite]) for suite in SUITES
        }
    (output_root / "plan.json").write_text(json.dumps(manifest, indent=2) + "\n")
    write_rows(results, output_root)
    report_payload = args.report_payload.resolve() if args.report_payload else output_root / "app-build-matrix.json"
    if args.phase == "measure":
        write_report_payload(results, report_payload)
    return 0 if all(row["valid"] for row in results) else 1


if __name__ == "__main__":
    raise SystemExit(main())

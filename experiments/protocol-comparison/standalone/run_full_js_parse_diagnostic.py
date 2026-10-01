#!/usr/bin/env python3
"""Run the original Clava-JS Vitest suite with outer CodeParser.parse spans.

This is a diagnostic-only overlay. It compiles the replacement ParallelCodeParser
class family against each frozen stage runtime, updates only the per-run copied parser
JAR, then invokes the existing full-suite direct runner serially in a counterbalanced
two-round order. A host-release note is mandatory before compilation or execution.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import zipfile
from typing import Any


STAGE_SOURCE_SHA256 = "3caf0c47c80bbfb41cad5bf0d9dbfc11d61eda10f3a4d55b9b5f5ae7187b9779"
OVERLAY_SOURCE_SHA256 = "edc05b0109a4866e0388e9e14858d94ff9231fb20f57e0fe71c7932b01dbfe72"
OVERLAY_CLASS_ENTRY = "pt/up/fe/specs/clang/codeparser/ParallelCodeParser.class"
RELEASE_TAG_ENTRY = "clang-dumper-release.tag"


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def jar_files(root: Path) -> dict[str, str]:
    return {
        path.relative_to(root).as_posix(): sha256_file(path)
        for path in sorted(root.rglob("*.jar"))
        if path.is_file()
    }


def parse_args() -> argparse.Namespace:
    default_diag = Path(__file__).resolve().parents[1] / "results" / "grouped-live-corpus-20260930-r2" / "standalone-overlay" / "full-js-outer-parse-r1"
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--matrix-root",
        type=Path,
        default=Path("/home/lmsousa/Documents/Projects/SPeCS/ast-protobuf/clava/experiments/protocol-comparison/results/deadline-20260930"),
        help="frozen deadline matrix root containing stages/ and shared/",
    )
    parser.add_argument("--diagnostic-root", type=Path, default=default_diag)
    parser.add_argument("--output-root", type=Path)
    parser.add_argument(
        "--host-release-note",
        help="parent's explicit host handoff reference; without it the script does not compile or run",
    )
    return parser.parse_args()


def validate_jar_overlay(
    source_runtime: Path,
    run_runtime: Path,
    stage: dict[str, Any],
    expected_class_hashes: dict[str, str],
) -> dict[str, Any]:
    source_jar = source_runtime / "lib" / "ClangAstParser.jar"
    run_jar = run_runtime / "lib" / "ClangAstParser.jar"
    source_jars = jar_files(source_runtime)
    run_jars = jar_files(run_runtime)
    source_manifest_sha256 = hashlib.sha256(
        json.dumps(source_jars, sort_keys=True, separators=(",", ":")).encode()
    ).hexdigest()
    if len(source_jars) != stage["runtime_manifest"]["jar_count"] \
            or source_manifest_sha256 != stage["runtime_manifest"]["sha256"]:
        raise RuntimeError(f"frozen source runtime manifest mismatch for {stage['key']}")
    if set(source_jars) != set(run_jars):
        raise RuntimeError(f"runtime JAR file set changed for {stage['key']}")
    for relative, source_sha in source_jars.items():
        if relative == "lib/ClangAstParser.jar":
            continue
        if run_jars[relative] != source_sha:
            raise RuntimeError(f"non-parser runtime JAR changed for {stage['key']}: {relative}")

    if OVERLAY_CLASS_ENTRY not in expected_class_hashes:
        raise RuntimeError("compiled overlay is missing its ParallelCodeParser class")

    with zipfile.ZipFile(source_jar) as source_archive, zipfile.ZipFile(run_jar) as run_archive:
        source_names = set(source_archive.namelist())
        run_names = set(run_archive.namelist())
        expected_names = source_names | set(expected_class_hashes)
        if expected_names != run_names:
            raise RuntimeError(f"parser JAR entry set changed beyond the compiled class family for {stage['key']}")
        for entry in source_names - set(expected_class_hashes) - {RELEASE_TAG_ENTRY}:
            if hashlib.sha256(source_archive.read(entry)).digest() != hashlib.sha256(run_archive.read(entry)).digest():
                raise RuntimeError(f"unexpected parser JAR entry change for {stage['key']}: {entry}")
        observed_class_hashes = {
            entry: hashlib.sha256(run_archive.read(entry)).hexdigest()
            for entry in expected_class_hashes
        }
        if observed_class_hashes != expected_class_hashes:
            raise RuntimeError(f"compiled class family hash mismatch for {stage['key']}: {observed_class_hashes}")
        tag_directory = run_archive.read(RELEASE_TAG_ENTRY).decode("utf-8").strip()

    expected_native = Path(stage["dumper"]).resolve()
    configured_native = (Path(tag_directory) / expected_native.name).resolve()
    if configured_native != expected_native:
        raise RuntimeError(f"runtime tag points at {configured_native}, expected {expected_native}")
    native_sha = sha256_file(expected_native)
    if native_sha != stage["dumper_sha256"]:
        raise RuntimeError(f"actual native binary SHA changed for {stage['key']}")

    return {
        "stage": stage["key"],
        "source_runtime_jar_count": len(source_jars),
        "source_runtime_manifest_sha256": stage["runtime_manifest"]["sha256"],
        "non_parser_jars_match": True,
        "parser_jar_original_sha256": source_jars["lib/ClangAstParser.jar"],
        "parser_jar_diagnostic_sha256": run_jars["lib/ClangAstParser.jar"],
        "overlay_class_entries": [
            {"entry": entry, "sha256": expected_class_hashes[entry]}
            for entry in sorted(expected_class_hashes)
        ],
        "configured_native_path": str(configured_native),
        "native_sha256": native_sha,
    }


def parse_metric_rows(log_path: Path) -> list[dict[str, Any]]:
    rows = []
    for line_number, line in enumerate(log_path.read_text(errors="replace").splitlines(), 1):
        marker = "CLAVA_FULL_PARSE "
        if not line.startswith(marker):
            continue
        try:
            row = json.loads(line[len(marker):])
        except json.JSONDecodeError as error:
            raise RuntimeError(f"malformed full-parse metric at {log_path}:{line_number}: {error}") from error
        rows.append(row)
    return rows


def main() -> int:
    args = parse_args()
    matrix_root = args.matrix_root.resolve()
    diagnostic_root = args.diagnostic_root.resolve()
    output_root = (args.output_root or diagnostic_root / "execution-r1").resolve()
    if not args.host_release_note or not args.host_release_note.strip():
        print(json.dumps({
            "mode": "plan-only",
            "compiled": False,
            "executed": False,
            "matrix_root": str(matrix_root),
            "diagnostic_root": str(diagnostic_root),
            "output_root": str(output_root),
            "required_order": ["ccache-text-r01", "protobuf-r01", "protobuf-r02", "ccache-text-r02"],
            "required_parent_action": "pass --host-release-note only after the exclusive benchmark-host handoff",
        }, indent=2))
        return 0

    if output_root.exists():
        raise SystemExit(f"refusing to overwrite existing diagnostic output: {output_root}")
    if not matrix_root.is_dir() or not diagnostic_root.is_dir():
        raise SystemExit("matrix root or prepared diagnostic source root is missing")

    scripts_root = matrix_root / "orchestration" / "experiments" / "protocol-comparison"
    sys.path.insert(0, str(scripts_root))
    import run_deadline_matrix as matrix

    if matrix.DEADLINE_ROOT.resolve() != matrix_root:
        raise SystemExit(f"matrix script resolves a different matrix root: {matrix.DEADLINE_ROOT}")

    os.environ["SPECS_JAVA_LIBS_HOME"] = str(matrix.SPECS_ROOT.resolve())
    os.environ["LARA_FRAMEWORK_HOME"] = str(matrix.LARA_ROOT.resolve())
    comparison = matrix.comparison
    comparison.FIXED_SPECSUTILS_REVISION = matrix.FIXED_SPECSUTILS_REVISION
    selected = {"ccache-text", "protobuf"}
    comparison.STAGES = tuple(dict(stage) for stage in matrix.STAGE_CONFIG)
    stages = comparison.validate_stages(selected)
    stages_by_key = {stage["key"]: stage for stage in stages}

    overlay_sources = {
        "ccache-text": diagnostic_root / "source" / "text" / "ParallelCodeParser.java",
        "protobuf": diagnostic_root / "source" / "protobuf" / "ParallelCodeParser.java",
    }
    class_directories: dict[str, Path] = {}
    class_hashes: dict[str, str] = {}
    class_family_hashes: dict[str, dict[str, str]] = {}
    for key, stage in stages_by_key.items():
        source_runtime = Path(stage["root"]) / "clava" / "Clava-JS" / "java-binaries"
        source_file = overlay_sources[key]
        stage_source_file = Path(stage["root"]) / "clava" / "ClangAstParser" / "src" / OVERLAY_CLASS_ENTRY.replace(".class", ".java")
        if sha256_file(stage_source_file) != STAGE_SOURCE_SHA256:
            raise SystemExit(f"unexpected frozen stage source for {key}: {stage_source_file}")
        if sha256_file(source_file) != OVERLAY_SOURCE_SHA256:
            raise SystemExit(f"unexpected prepared diagnostic source for {key}: {source_file}")
        class_directory = output_root.parent / f"{output_root.name}-classes" / key
        if class_directory.exists():
            raise SystemExit(f"refusing to reuse existing compiled overlay directory: {class_directory}")
        class_directory.mkdir(parents=True)
        command = [
            "javac",
            "-classpath",
            str(source_runtime / "lib" / "*"),
            "-d",
            str(class_directory),
            str(source_file),
        ]
        subprocess.run(command, check=True)
        class_files = sorted(class_directory.rglob("ParallelCodeParser*.class"))
        expected_package = Path(OVERLAY_CLASS_ENTRY).parent
        if not class_files or not (class_directory / OVERLAY_CLASS_ENTRY).is_file():
            raise RuntimeError(f"javac did not produce the expected overlay class for {key}")
        family_hashes: dict[str, str] = {}
        for class_file in class_files:
            relative = class_file.relative_to(class_directory).as_posix()
            if Path(relative).parent != expected_package or not class_file.name.startswith("ParallelCodeParser"):
                raise RuntimeError(f"unexpected compiled class outside ParallelCodeParser family: {relative}")
            family_hashes[relative] = sha256_file(class_file)
        class_directories[key] = class_directory
        class_family_hashes[key] = family_hashes
        class_hashes[key] = family_hashes[OVERLAY_CLASS_ENTRY]

    output_root.mkdir(parents=True)
    source_to_key = {
        (Path(stage["root"]) / "clava" / "Clava-JS" / "java-binaries").resolve(): key
        for key, stage in stages_by_key.items()
    }
    original_stage_runtime = comparison.stage_runtime
    runtime_gates: list[dict[str, Any]] = []

    def stage_runtime_with_overlay(source: Path, destination: Path, dumper: str | None) -> None:
        original_stage_runtime(source, destination, dumper)
        stage_key = source_to_key.get(Path(source).resolve())
        if stage_key is None:
            raise RuntimeError(f"unexpected runtime source passed to runner: {source}")
        class_directory = class_directories[stage_key]
        class_entries = sorted(class_family_hashes[stage_key])
        jar_command = ["jar", "uf", str(destination / "lib" / "ClangAstParser.jar")]
        for class_entry in class_entries:
            jar_command.extend(["-C", str(class_directory), class_entry])
        subprocess.run(jar_command, check=True)
        gate = validate_jar_overlay(
            source,
            destination,
            stages_by_key[stage_key],
            class_family_hashes[stage_key],
        )
        (destination.parent / "outer-parse-runtime-gate.json").write_text(json.dumps(gate, indent=2) + "\n")
        runtime_gates.append(gate)

    comparison.stage_runtime = stage_runtime_with_overlay
    original_write_vitest_config = comparison.write_vitest_config

    def write_vitest_config_as_esm(run_dir: Path, clava: Path, runtime: Path) -> Path:
        # The diagnostic output root is outside the ast-protobuf workspace's
        # package.json (type=module) scope. Keep identical config contents, but
        # use .mts so Vite's native config loader treats its imports as ESM.
        config = original_write_vitest_config(run_dir, clava, runtime)
        esm_config = config.with_suffix(".mts")
        config.replace(esm_config)
        return esm_config

    comparison.write_vitest_config = write_vitest_config_as_esm
    base_java_tool_options = os.environ.get("JAVA_TOOL_OPTIONS", "").strip()
    metric_flags = [item for item in base_java_tool_options.split() if item.startswith("-Dclava.fullParseMetrics=")]
    if metric_flags and metric_flags != ["-Dclava.fullParseMetrics=true"]:
        raise SystemExit(f"conflicting inherited full-parse metric flag: {metric_flags}")
    if not metric_flags:
        os.environ["JAVA_TOOL_OPTIONS"] = f"{base_java_tool_options} -Dclava.fullParseMetrics=true".strip()

    order = [
        (1, "ccache-text"),
        (1, "protobuf"),
        (2, "protobuf"),
        (2, "ccache-text"),
    ]
    results: list[dict[str, Any]] = []
    all_metric_rows: list[dict[str, Any]] = []
    try:
        for ordinal, (repeat, key) in enumerate(order, 1):
            stage = stages_by_key[key]
            native_before = sha256_file(Path(stage["dumper"]))
            if native_before != stage["dumper_sha256"]:
                raise RuntimeError(f"native binary hash mismatch before run for {key}")
            result = comparison.run_clava_js(
                stage, output_root, ordinal, measured=True, repeat=repeat, mode="direct"
            )
            if not result.get("valid") or not result.get("cache_validation", {}).get("passed"):
                raise RuntimeError(f"full-suite test/cache gate failed: {key}, round {repeat}: {result}")
            if result.get("cacheable_calls") != 0 or result.get("cache_hits") != 0 \
                    or result.get("cache_misses") != 0:
                raise RuntimeError(f"direct mode unexpectedly used ccache: {key}, round {repeat}")
            native_after = sha256_file(Path(stage["dumper"]))
            if native_after != native_before:
                raise RuntimeError(f"native binary changed during the {key} full-suite run")

            run_dir = Path(result["run_dir"])
            metric_rows = parse_metric_rows(run_dir / "run.log")
            if not metric_rows:
                raise RuntimeError(f"no instrumented CodeParser.parse records found in {run_dir / 'run.log'}")
            required_fields = {
                "record_type", "call_ordinal", "identity_sha256", "options_sha256",
                "source_content_sha256", "input_count", "option_count", "input_basenames",
                "input_content_sha256", "elapsed_ms", "outcome",
            }
            for row in metric_rows:
                if not required_fields.issubset(row) or row.get("record_type") != "full_parse":
                    raise RuntimeError(f"incomplete full-parse metric row in {run_dir}: {row}")
                row.update({"suite": "clava-js", "stage": key, "repeat": repeat, "mode": "direct"})
            metric_path = run_dir / "outer-parse.jsonl"
            metric_path.write_text("".join(json.dumps(row, sort_keys=True) + "\n" for row in metric_rows))
            result["outer_parse_records"] = len(metric_rows)
            result["outer_parse_path"] = str(metric_path)
            result["outer_parse_sha256"] = sha256_file(metric_path)
            (run_dir / "summary.json").write_text(json.dumps(result, indent=2) + "\n")
            results.append(result)
            all_metric_rows.extend(metric_rows)
            print(json.dumps({
                "completed": ordinal,
                "expected_total": len(order),
                "stage": key,
                "repeat": repeat,
                "elapsed_s": result.get("elapsed_s"),
                "outer_parse_records": len(metric_rows),
                "valid": result.get("valid"),
            }, sort_keys=True), flush=True)
    finally:
        comparison.stage_runtime = original_stage_runtime
        comparison.write_vitest_config = original_write_vitest_config

    if len(runtime_gates) != len(order):
        raise RuntimeError(f"expected {len(order)} runtime gates, got {len(runtime_gates)}")
    metrics_path = output_root / "outer-parse.jsonl"
    metrics_path.write_text("".join(json.dumps(row, sort_keys=True) + "\n" for row in all_metric_rows))
    (output_root / "results.json").write_text(json.dumps(results, indent=2) + "\n")
    (output_root / "execution-manifest.json").write_text(json.dumps({
        "schema_version": 1,
        "host_release_note": args.host_release_note,
        "matrix_root": str(matrix_root),
        "order": [{"repeat": repeat, "stage": key} for repeat, key in order],
        "protocol_source_sha256": OVERLAY_SOURCE_SHA256,
        "compiled_class_sha256": class_hashes,
        "compiled_class_family_sha256": class_family_hashes,
        "runtime_gates": runtime_gates,
        "results_sha256": sha256_file(output_root / "results.json"),
        "outer_parse_sha256": sha256_file(metrics_path),
    }, indent=2) + "\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

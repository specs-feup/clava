#!/usr/bin/env python3
"""Compare Clava consumer outcomes and generated code across fixed runtimes."""

from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import time
import zipfile
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
CLAVA_ROOT = ROOT.parents[1]
PROBE = Path(__file__).with_name("java") / "ValidationProbe.java"
RESULTS_ROOT = ROOT / "results" / "validation"


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
    jar_hashes = {
        str(path.relative_to(runtime)): sha256_file(path)
        for path in sorted(runtime.rglob("*.jar")) if path.is_file()
    }
    canonical = json.dumps(jar_hashes, sort_keys=True, separators=(",", ":")).encode()
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
        "jar_count": len(jar_hashes),
        "jar_manifest_sha256": hashlib.sha256(canonical).hexdigest(),
        "source_revisions_file": str(source_revisions_path) if source_revisions_path else None,
        "source_revisions_sha256": sha256_file(source_revisions_path) if source_revisions_path else None,
        "source_revisions": source_revisions,
    }


def parse_runtime(raw: str) -> tuple[str, Path]:
    label, separator, value = raw.partition("=")
    if not separator or not label or not value:
        raise argparse.ArgumentTypeError("runtime must be LABEL=/path/to/ClavaWeaver")
    return label, Path(value).resolve()


def git_state() -> dict[str, Any]:
    revision = subprocess.run(["git", "rev-parse", "HEAD"], cwd=CLAVA_ROOT,
                              capture_output=True, text=True, check=False)
    status = subprocess.run(["git", "status", "--porcelain=v1", "--untracked-files=all"], cwd=CLAVA_ROOT,
                            capture_output=True, text=True, check=False)
    return {"revision": revision.stdout.strip() if revision.returncode == 0 else "unknown",
            "dirty": status.returncode != 0 or bool(status.stdout.strip()),
            "porcelain": status.stdout.splitlines()}


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--runtime", action="append", type=parse_runtime, required=True,
                        help="Prebuilt runtime, e.g. --runtime eager=/path; repeat for controls.")
    parser.add_argument("--manifest", type=Path, required=True,
                        help="consumer-inputs.json emitted by run_binary_corpus.py")
    parser.add_argument("--output-root", type=Path, required=True)
    parser.add_argument("--heap", default="8g")
    parser.add_argument("--reparse-generated", action="store_true",
                        help="Reparse each emitted translation unit and require stable generation.")
    return parser.parse_args()


def parse_rows(text: str) -> list[dict[str, Any]]:
    rows = []
    for line in text.splitlines():
        match = re.match(r"CLAVA_CORPUS (\{.*\})$", line)
        if match:
            rows.append(json.loads(match.group(1)))
    return rows


def run_runtime(label: str, runtime: Path, manifest: dict[str, Any], output: Path,
                heap: str, reparse: bool = False) -> dict[str, Any]:
    metadata = runtime_metadata(runtime)
    classes = output / "classes" / label
    classes.mkdir(parents=True)
    lib = runtime / "lib"
    classpath = f"{classes}:{lib}/*"
    compile_command = ["javac", "-cp", f"{lib}/*", "-d", str(classes), str(PROBE)]
    subprocess.run(compile_command, cwd=CLAVA_ROOT, check=True)
    manifest_path = output / "consumer-inputs.json"
    manifest_path.write_text(json.dumps(manifest, indent=2) + "\n")
    work = output / "consumer-work" / label
    work.mkdir(parents=True)
    tmp = output / "tmp" / label
    tmp.mkdir(parents=True)
    xdg = output / "xdg-cache" / label
    xdg.mkdir(parents=True)
    command = [
        "java", "-Xms256m", f"-Xmx{heap}", f"-Djava.io.tmpdir={tmp}",
        "-cp", classpath, "ValidationProbe",
        "corpus-roundtrip" if reparse else "corpus", str(manifest_path), str(work),
    ]
    env = os.environ.copy()
    options = env.get("JAVA_TOOL_OPTIONS", "")
    if re.search(r"(?<!\S)-Dclava\.astWire=\S+", options) or any(
        key in env for key in ("AST_WIRE_FLAT", "AST_WIRE_DENSE_TEXT")
    ):
        raise SystemExit("remove obsolete AST wire-selection overrides before corpus validation")
    env["CCACHE_DISABLE"] = "true"
    env["XDG_CACHE_HOME"] = str(xdg)
    env.update({"TMPDIR": str(tmp), "TMP": str(tmp), "TEMP": str(tmp)})
    env["JAVA_TOOL_OPTIONS"] = (options + " " if options else "") + f"-Djava.io.tmpdir={tmp}"
    started = time.perf_counter()
    completed = subprocess.run(command, cwd=CLAVA_ROOT, env=env, capture_output=True,
                               text=True, check=False)
    elapsed = time.perf_counter() - started
    log = output / f"{label}.log"
    log.write_text(completed.stdout + completed.stderr)
    rows = parse_rows(completed.stdout + completed.stderr)
    expected = len(manifest["files"])
    seen = {row.get("relative") for row in rows}
    missing = sorted(set(item["relative"] for item in manifest["files"]) - seen)
    clean = sum(row.get("bucket") == "CLEAN" for row in rows)
    failed = sum(row.get("bucket") != "CLEAN" for row in rows)
    result = {
        "label": label,
        "runtime": metadata,
        "cases_expected": expected,
        "cases_reported": len(rows),
        "clean": clean,
        "consumer_failures": failed,
        "missing_rows": missing,
        "elapsed_s": elapsed,
        "timing_boundary": "one JVM; measured command includes compilation? No: javac runs before the timer; timer covers JVM startup and all per-source native parse/import/generate work.",
        "cache_policy": "CCACHE_DISABLE=true; AST_DUMP_CACHE=false in ValidationProbe",
        "return_code": completed.returncode,
        "compile_command": compile_command,
        "command": command,
        "log": str(log),
        "results": str(output / f"{label}-results.json"),
        "rows": rows,
        "passed": completed.returncode == 0 and len(rows) == expected and not missing
                  and (not reparse or failed == 0),
    }
    (output / f"{label}-results.json").write_text(json.dumps(rows, indent=2) + "\n")
    return result


def main() -> int:
    args = parse_args()
    if len(args.runtime) < 2 and not args.reparse_generated:
        raise SystemExit("supply eager plus at least one isolated comparison runtime, or --reparse-generated")
    if len({label for label, _ in args.runtime}) != len(args.runtime):
        raise SystemExit("runtime labels must be unique")
    for _, runtime in args.runtime:
        if not runtime.is_dir():
            raise SystemExit(f"runtime directory does not exist: {runtime}")
    manifest_path = args.manifest.resolve()
    if not manifest_path.is_file():
        raise SystemExit(f"corpus consumer manifest does not exist: {manifest_path}")
    manifest_bytes = manifest_path.read_bytes()
    manifest = json.loads(manifest_bytes)
    files = manifest.get("files")
    if not isinstance(files, list) or not files:
        raise SystemExit("corpus consumer manifest has no files")
    output = args.output_root.resolve()
    if output.exists() and any(output.iterdir()):
        raise SystemExit(f"refusing to reuse non-empty output directory: {output}")
    output.mkdir(parents=True, exist_ok=True)
    results = [run_runtime(label, runtime, manifest, output, args.heap, args.reparse_generated)
               for label, runtime in args.runtime]
    by_label = {item["label"]: {row["relative"]: row for row in item["rows"]} for item in results}
    eager_label = next((label for label, _ in args.runtime if label == "eager"), args.runtime[0][0])
    eager = by_label[eager_label]
    comparisons = []
    for other_label, _runtime in args.runtime:
        if other_label == eager_label:
            continue
        other = by_label[other_label]
        for relative in sorted(set(eager) | set(other)):
            eager_row = eager.get(relative)
            other_row = other.get(relative)
            eager_clean = bool(eager_row and eager_row.get("bucket") == "CLEAN")
            other_clean = bool(other_row and other_row.get("bucket") == "CLEAN")
            if eager_clean and other_clean:
                status = "MATCH" if eager_row.get("generated_code_sha256") == other_row.get("generated_code_sha256") else "GENERATED_CODE_MISMATCH"
            elif eager_clean and not other_clean:
                status = "EAGER_ONLY"
            elif not eager_clean and other_clean:
                status = "EAGER_REGRESSION"
            else:
                status = "BOTH_UNSUPPORTED"
            comparisons.append({
                "control": other_label,
                "relative": relative,
                "status": status,
                "eager_bucket": eager_row.get("bucket") if eager_row else "MISSING",
                "control_bucket": other_row.get("bucket") if other_row else "MISSING",
                "eager_code_sha256": eager_row.get("generated_code_sha256") if eager_row else None,
                "control_code_sha256": other_row.get("generated_code_sha256") if other_row else None,
            })
    counts: dict[str, dict[str, int]] = {}
    for row in comparisons:
        control = row["control"]
        counts.setdefault(control, {})
        counts[control][row["status"]] = counts[control].get(row["status"], 0) + 1
    regression_or_mismatch = any(row["status"] in {"EAGER_REGRESSION", "GENERATED_CODE_MISMATCH"}
                                 for row in comparisons)
    summary = {
        "created_utc": dt.datetime.now(dt.timezone.utc).isoformat(),
        "reparse_generated": args.reparse_generated,
        "consumer_manifest": str(manifest_path),
        "consumer_manifest_sha256": hashlib.sha256(manifest_bytes).hexdigest(),
        "corpus": manifest.get("corpus"),
        "corpus_revision": manifest.get("corpus_revision"),
        "clava_git": git_state(),
        "baseline_results_sha256": manifest.get("baseline_results_sha256"),
        "results": results,
        "comparisons": str(output / "comparisons.json"),
        "comparison_counts": counts,
        "pass": all(result["passed"] for result in results) and not regression_or_mismatch,
        "comparison_contract": ("Require every selected input to parse, generate, reparse and generate identical bytes."
                                if args.reparse_generated else
                                "Require no eager-only consumer failures and exact generated-code hashes for cases both runtimes consume."),
    }
    (output / "comparisons.json").write_text(json.dumps(comparisons, indent=2) + "\n")
    (output / "summary.json").write_text(json.dumps(summary, indent=2) + "\n")
    print(json.dumps({"output": str(output), **summary}, indent=2))
    return 0 if summary["pass"] else 1


if __name__ == "__main__":
    raise SystemExit(main())

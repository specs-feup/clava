#!/usr/bin/env python3
"""Build a measured-App shadow of a frozen ClangAstParser runtime stage."""

from __future__ import annotations

import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import zipfile
from typing import Any


PARSER_SUFFIX = Path(
    "pt/up/fe/specs/clang/codeparser/ParallelCodeParser.java"
)
PARSER_SIGNATURE = re.compile(
    r"public\s+App\s+parse\s*\(\s*List<File>\s+inputSources\s*,\s*"
    r"List<String>\s+compilerOptions\s*,\s*ClavaContext\s+context\s*\)"
)
TEXT_TRANSFORM = "new TreeTransformer(ClangAstParser.getTextParsingRules()).transform(app);"
CLASS_DECLARATION = "public class ParallelCodeParser extends CodeParser {"
HELPER_SOURCE = Path(__file__).resolve().parent / "src" / PARSER_SUFFIX.parent / "AppBuildMetrics.java"


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for chunk in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def _find_parser_source(source_root: Path) -> Path:
    root = source_root.resolve()
    if root.is_file():
        if root.name != "ParallelCodeParser.java":
            raise ValueError(f"expected ParallelCodeParser.java, got {root}")
        return root

    candidates = (
        root / "clava" / "ClangAstParser" / "src" / PARSER_SUFFIX,
        root / "ClangAstParser" / "src" / PARSER_SUFFIX,
        root / "src" / PARSER_SUFFIX,
        root / PARSER_SUFFIX,
    )
    for candidate in candidates:
        if candidate.is_file():
            return candidate

    matches = [
        path for path in root.rglob("ParallelCodeParser.java")
        if path.as_posix().endswith(PARSER_SUFFIX.as_posix())
    ]
    if len(matches) != 1:
        raise ValueError(
            f"expected exactly one frozen ParallelCodeParser source under {root}, found {len(matches)}"
        )
    return matches[0]


def _method_bounds(source: str) -> tuple[int, int, int]:
    match = PARSER_SIGNATURE.search(source)
    if match is None:
        raise ValueError("could not find the expected three-argument parse signature")
    opening = source.find("{", match.end())
    if opening < 0:
        raise ValueError("parse method has no body")
    closing = _matching_brace(source, opening)
    return opening, closing, match.start()


def _matching_brace(source: str, opening: int) -> int:
    """Find a Java block's closing brace while ignoring comments and literals."""
    depth = 0
    state = "code"
    index = opening
    while index < len(source):
        char = source[index]
        next_char = source[index + 1] if index + 1 < len(source) else ""
        if state == "code":
            if char == "/" and next_char == "/":
                state = "line-comment"
                index += 2
                continue
            if char == "/" and next_char == "*":
                state = "block-comment"
                index += 2
                continue
            if source.startswith('"""', index):
                state = "text-block"
                index += 3
                continue
            if char == '"':
                state = "string"
            elif char == "'":
                state = "char"
            elif char == "{":
                depth += 1
            elif char == "}":
                depth -= 1
                if depth == 0:
                    return index
        elif state == "line-comment":
            if char == "\n":
                state = "code"
        elif state == "block-comment":
            if char == "*" and next_char == "/":
                state = "code"
                index += 2
                continue
        elif state == "text-block":
            if source.startswith('"""', index):
                state = "code"
                index += 3
                continue
        elif state in ("string", "char"):
            if char == "\\":
                index += 2
                continue
            if (state == "string" and char == '"') or (state == "char" and char == "'"):
                state = "code"
        index += 1
    raise ValueError("parse method has an unterminated body")


def instrument_source(source: str) -> tuple[str, dict[str, int]]:
    """Add timing and row emission around the unchanged parser body."""
    if source.count(CLASS_DECLARATION) != 1:
        raise ValueError("expected one ParallelCodeParser class declaration")
    opening, closing, _ = _method_bounds(source)
    body = source[opening + 1:closing]

    text_count = body.count(TEXT_TRANSFORM)
    app_return = re.compile(r"(?m)^(?P<indent>[ \t]*)return app;[ \t]*$")
    null_return = re.compile(r"(?m)^(?P<indent>[ \t]*)return null;[ \t]*$")
    app_returns = list(app_return.finditer(body))
    null_returns = list(null_return.finditer(body))
    if text_count != 1 or len(app_returns) != 1 or len(null_returns) != 1:
        raise ValueError(
            "unexpected parse body anchors: "
            f"text_transform={text_count}, app_returns={len(app_returns)}, null_returns={len(null_returns)}"
        )
    if not HELPER_SOURCE.is_file():
        raise ValueError(f"missing Java helper source: {HELPER_SOURCE}")

    body = body.replace(
        TEXT_TRANSFORM,
        TEXT_TRANSFORM
        + "\n        __appBuildStopNanos = System.nanoTime();",
        1,
    )

    null_return = re.compile(r"(?m)^(?P<indent>[ \t]*)return null;[ \t]*$")
    null_match = null_return.search(body)
    assert null_match is not None
    indent = null_match.group("indent")
    syntax_record = (
        f"{indent}__appBuildRowWritten = true;\n"
        f"{indent}AppBuildMetrics.recordSyntaxOnly(this, inputSources, compilerOptions, context, "
        f"__appBuildCallOrdinal, __appBuildPreviousShowExecInfo);\n"
        f"{indent}return null;"
    )
    body = null_return.sub(lambda _: syntax_record, body, count=1)

    app_return = re.compile(r"(?m)^(?P<indent>[ \t]*)return app;[ \t]*$")
    app_match = app_return.search(body)
    assert app_match is not None
    indent = app_match.group("indent")
    success_record = (
        f"{indent}__appBuildRowWritten = true;\n"
        f"{indent}AppBuildMetrics.recordSuccess(this, inputSources, compilerOptions, context, "
        f"__appBuildCallOrdinal, __appBuildStartNanos, __appBuildStopNanos, "
        f"__appBuildPreviousShowExecInfo, app == null);\n"
        f"{indent}return app;"
    )
    body = app_return.sub(lambda _: success_record, body, count=1)

    prologue = """
        final long __appBuildStartNanos = System.nanoTime();
        boolean __appBuildPreviousShowExecInfo = false;
        boolean __appBuildShowExecInfoCaptured = false;
        long __appBuildCallOrdinal = 0L;
        boolean __appBuildSyntaxOnly = false;
        long __appBuildStopNanos = -1L;
        boolean __appBuildRowWritten = false;
        try {
            __appBuildPreviousShowExecInfo = get(SHOW_EXEC_INFO);
            __appBuildShowExecInfoCaptured = true;
            set(SHOW_EXEC_INFO, false);
            __appBuildCallOrdinal = AppBuildMetrics.nextCallOrdinal();
            __appBuildSyntaxOnly = get(SYNTAX_ONLY);
"""
    epilogue = """
        } catch (Throwable __appBuildFailure) {
            if (!__appBuildRowWritten) {
                __appBuildRowWritten = true;
                long __appBuildFailureStopNanos = __appBuildStopNanos >= 0L
                        ? __appBuildStopNanos : System.nanoTime();
                AppBuildMetrics.recordFailure(this, inputSources, compilerOptions, context,
                        __appBuildCallOrdinal, __appBuildStartNanos, __appBuildFailureStopNanos,
                        __appBuildPreviousShowExecInfo, __appBuildSyntaxOnly, __appBuildFailure);
            }
            throw AppBuildMetrics.rethrow(__appBuildFailure);
        } finally {
            if (__appBuildShowExecInfoCaptured) {
                set(SHOW_EXEC_INFO, __appBuildPreviousShowExecInfo);
            }
        }
"""
    transformed = (
        source[:opening + 1]
        + prologue
        + body
        + epilogue
        + source[closing:]
    )
    transformed = transformed.replace(
        CLASS_DECLARATION,
        CLASS_DECLARATION
        + "\n\n    static {\n        AppBuildMetrics.initialize();\n    }",
        1,
    )
    return transformed, {
        "parse_methods": 1,
        "text_transform_anchors": text_count,
        "syntax_only_returns": len(null_returns),
        "app_returns": len(app_returns),
    }


def _runtime_layout(runtime_lib: os.PathLike[str] | str) -> tuple[Path, Path]:
    path = Path(runtime_lib).resolve()
    if not path.is_dir():
        raise ValueError(f"runtime_lib must be a directory: {path}")
    if (path / "lib").is_dir():
        runtime_root, lib_dir = path, path / "lib"
    elif path.name == "lib":
        runtime_root, lib_dir = path.parent, path
    else:
        raise ValueError(f"expected a runtime root or lib directory, got {path}")
    required = ("ClangAstParser.jar", "gson-2.12.1.jar")
    missing = [name for name in required if not (lib_dir / name).is_file()]
    if missing:
        raise ValueError(f"runtime library directory is missing required JARs: {missing}")
    return runtime_root, lib_dir


def _runtime_jar_manifest(lib_dir: Path) -> dict[str, str]:
    return {
        path.relative_to(lib_dir).as_posix(): sha256_file(path)
        for path in sorted(lib_dir.rglob("*.jar"))
        if path.is_file()
    }


def _class_entries(class_dir: Path) -> list[str]:
    entries = sorted(path.relative_to(class_dir).as_posix()
                     for path in class_dir.rglob("*.class"))
    expected_prefixes = (
        "pt/up/fe/specs/clang/codeparser/ParallelCodeParser",
        "pt/up/fe/specs/clang/codeparser/AppBuildMetrics",
    )
    unexpected = [
        entry for entry in entries
        if not entry.endswith(".class") or not entry.startswith(expected_prefixes)
    ]
    required = {
        "pt/up/fe/specs/clang/codeparser/ParallelCodeParser.class",
        "pt/up/fe/specs/clang/codeparser/AppBuildMetrics.class",
    }
    if unexpected or not required.issubset(entries):
        raise RuntimeError(
            f"unexpected overlay class output; missing={sorted(required - set(entries))}, "
            f"unexpected={unexpected}"
        )
    return entries


def _write_overlay_jar(class_dir: Path, entries: list[str], destination: Path) -> None:
    with zipfile.ZipFile(destination, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=6) as jar:
        for entry in entries:
            jar.write(class_dir / entry, entry)


def build_overlay(
    stage_key: str,
    source_root: os.PathLike[str] | str,
    runtime_lib: os.PathLike[str] | str,
    output_dir: os.PathLike[str] | str,
) -> dict[str, Path]:
    """Compile one frozen-stage shadow and return its JAR and provenance paths."""
    source_path = _find_parser_source(Path(source_root))
    runtime_root, lib_dir = _runtime_layout(runtime_lib)
    output_root = Path(output_dir).resolve()
    output_root.mkdir(parents=True, exist_ok=True)
    overlay_jar = output_root / "overlay.jar"
    provenance_path = output_root / "provenance.json"
    shadow_source = output_root / "src" / PARSER_SUFFIX
    if overlay_jar.exists() or provenance_path.exists() or shadow_source.exists():
        raise FileExistsError(f"refusing to overwrite app-build overlay output in {output_root}")

    original_source = source_path.read_text(encoding="utf-8")
    transformed_source, anchors = instrument_source(original_source)
    source_sha = sha256_file(source_path)
    helper_sha = sha256_file(HELPER_SOURCE)
    jars_before = _runtime_jar_manifest(lib_dir)
    javac = shutil.which("javac")
    if javac is None:
        raise RuntimeError("javac is required to compile the app-build overlay")

    with tempfile.TemporaryDirectory(prefix=".app-build-overlay-", dir=output_root) as temporary:
        build_root = Path(temporary)
        parser_shadow = build_root / "src" / PARSER_SUFFIX
        helper_shadow = build_root / "src" / PARSER_SUFFIX.parent / "AppBuildMetrics.java"
        parser_shadow.parent.mkdir(parents=True)
        helper_shadow.parent.mkdir(parents=True, exist_ok=True)
        parser_shadow.write_text(transformed_source, encoding="utf-8")
        shutil.copyfile(HELPER_SOURCE, helper_shadow)
        classes = build_root / "classes"
        classes.mkdir()
        command = [
            javac,
            "-encoding", "UTF-8",
            "-classpath", str(lib_dir / "*"),
            "-d", str(classes),
            str(parser_shadow),
            str(helper_shadow),
        ]
        completed = subprocess.run(command, text=True, capture_output=True, check=False)
        if completed.returncode != 0:
            raise RuntimeError(
                f"javac failed for {stage_key}:\n{completed.stdout}\n{completed.stderr}"
            )
        class_entries = _class_entries(classes)
        _write_overlay_jar(classes, class_entries, overlay_jar)
        transformed_sha = sha256_file(parser_shadow)

    jars_after = _runtime_jar_manifest(lib_dir)
    if jars_before != jars_after:
        overlay_jar.unlink(missing_ok=True)
        raise RuntimeError(f"runtime JARs changed while building overlay for {stage_key}")

    shadow_source.parent.mkdir(parents=True, exist_ok=True)
    shadow_source.write_text(transformed_source, encoding="utf-8")

    provenance: dict[str, Any] = {
        "schema_version": 1,
        "stage": stage_key,
        "source_root": str(Path(source_root).resolve()),
        "parser_source": str(source_path.resolve()),
        "parser_source_sha256": source_sha,
        "transformed_parser_sha256": transformed_sha,
        "helper_source": str(HELPER_SOURCE.resolve()),
        "helper_source_sha256": helper_sha,
        "transformed_parser_source": str(shadow_source),
        "runtime_root": str(runtime_root),
        "runtime_lib": str(lib_dir),
        "runtime_jars_before": jars_before,
        "runtime_jars_after": jars_after,
        "runtime_jars_unchanged": True,
        "javac_command": command,
        "javac_stdout": completed.stdout,
        "javac_stderr": completed.stderr,
        "overlay_class_entries": class_entries,
        "overlay_jar": str(overlay_jar),
        "overlay_jar_sha256": sha256_file(overlay_jar),
        "instrumentation_anchors": anchors,
    }
    provenance_path.write_text(json.dumps(provenance, indent=2) + "\n", encoding="utf-8")
    return {"overlay_jar": overlay_jar, "provenance": provenance_path}

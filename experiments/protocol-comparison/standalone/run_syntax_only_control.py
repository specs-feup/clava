#!/usr/bin/env python3
"""Run four paired original Clava-JS Text syntax-only controls.

Without --host-release-note this is plan-only. With a release note it compiles
one isolated legacy-Text native object into a new binary, overlays two Java
classes into per-run copies of the frozen Text runtime, then runs four rotated
OFF/ON pairs. It never writes into a frozen stage or the existing native build.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import shlex
import subprocess
import sys
import zipfile
from typing import Any


CLAVA_ROOT = Path(__file__).resolve().parents[3]
DEFAULT_MATRIX = Path("/home/lmsousa/Documents/Projects/SPeCS/ast-protobuf/clava/experiments/protocol-comparison/results/deadline-20260930")
DEFAULT_DIAGNOSTIC = CLAVA_ROOT / "experiments/protocol-comparison/results/grouped-live-corpus-20260930-r2/standalone-overlay/full-js-outer-parse-r1"
TEXT_PARSER_OVERLAY_SHA256 = "edc05b0109a4866e0388e9e14858d94ff9231fb20f57e0fe71c7932b01dbfe72"
FROZEN_TEXT_DUMPER_SOURCE_SHA256 = "32f7cd398ceec8c7e5234fa875c56dea400043973e074195a5de85db7c3a3096"
FROZEN_TEXT_NATIVE_REVISION = "bc498f5cedb88239062eef21a9669bc9bddb0ff7"
PARSER_ENTRY = "pt/up/fe/specs/clang/codeparser/ParallelCodeParser.class"
DUMPER_ENTRY = "pt/up/fe/specs/clang/dumper/ClangAstDumper.class"


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--matrix-root", type=Path, default=DEFAULT_MATRIX)
    parser.add_argument("--diagnostic-root", type=Path, default=DEFAULT_DIAGNOSTIC)
    parser.add_argument("--output-root", type=Path)
    parser.add_argument("--host-release-note", help="exclusive host release reference from the parent")
    return parser.parse_args()


def replace_once(source: str, old: str, new: str, label: str) -> str:
    count = source.count(old)
    if count != 1:
        raise RuntimeError(f"expected one {label} anchor, found {count}")
    return source.replace(old, new, 1)


def native_control_source(source: str) -> str:
    source = replace_once(
        source,
        'static llvm::cl::opt<bool> CompileOnlyOption(\n'
        '        "c", llvm::cl::desc("Parse without linking"),\n'
        '        llvm::cl::cat(MyToolCategory));\n',
        'static llvm::cl::opt<bool> CompileOnlyOption(\n'
        '        "c", llvm::cl::desc("Parse without linking"),\n'
        '        llvm::cl::cat(MyToolCategory));\n'
        'static llvm::cl::opt<bool> SyntaxCheckOnlyOption(\n'
        '        "syntax-check-only",\n'
        '        llvm::cl::desc("Validate syntax without producing an AST dump"),\n'
        '        llvm::cl::cat(MyToolCategory));\n',
        "native syntax-only option declaration",
    )
    source = replace_once(
        source,
        'Argument == "-c" || Argument == "-MD" || HasSeparateValue ||',
        'Argument == "-c" || Argument == "-MD" ||\n'
        '        Argument == "-syntax-check-only" || HasSeparateValue ||',
        "ccache option normalization",
    )
    return replace_once(
        source,
        '    returnValue =\n'
        '        Tool.run(clang::tooling::newFrontendActionFactory<DumpAstAction>().get());',
        '    auto ActionFactory = SyntaxCheckOnlyOption\n'
        '        ? clang::tooling::newFrontendActionFactory<clang::SyntaxOnlyAction>()\n'
        '        : clang::tooling::newFrontendActionFactory<DumpAstAction>();\n'
        '    returnValue = Tool.run(ActionFactory.get());',
        "no-output frontend action selection",
    )


def java_dumper_control_source(source: str) -> str:
    source = replace_once(
        source,
        '        var output = SpecsSystem.runProcess(arguments, lastWorkingFolder,\n'
        '                this::discardOutput,\n'
        '                inputStream -> processOutput(inputStream));',
        '        boolean syntaxOnlyControl = Boolean.getBoolean("clava.astWireBenchmarkSyntaxOnly");\n'
        '        List<String> syntaxArguments = arguments;\n'
        '        if (syntaxOnlyControl) {\n'
        '            syntaxArguments = new ArrayList<>(arguments);\n'
        '            int separatorIndex = syntaxArguments.indexOf("--");\n'
        '            if (separatorIndex >= 0) {\n'
        '                syntaxArguments.add(separatorIndex, "-syntax-check-only");\n'
        '            } else {\n'
        '                syntaxArguments.add("-syntax-check-only");\n'
        '            }\n'
        '        }\n'
        '\n'
        '        var output = SpecsSystem.runProcess(syntaxArguments, lastWorkingFolder,\n'
        '                this::discardOutput,\n'
        '                inputStream -> processOutput(inputStream));',
        "Java syntax validation launch",
    )
    source = replace_once(
        source,
        '        output.getOutputException().ifPresent(exception -> {\n'
        '            throw new RuntimeException("Exception while validating syntax", exception);\n'
        '        });',
        '        output.getOutputException().ifPresent(exception -> {\n'
        '            throw new RuntimeException("Exception while validating syntax", exception);\n'
        '        });\n'
        '        if (Boolean.getBoolean("clava.fullParseMetrics")) {\n'
        '            SYNTAX_CONTROL_METRICS.add(new SyntaxControlMetric(syntaxOnlyControl,\n'
        '                    syntaxArguments, output.getStdErr().length()));\n'
        '        }',
        "syntax invocation diagnostics",
    )
    source = replace_once(
        source,
        "public class ClangAstDumper {",
        "public class ClangAstDumper {\n"
        "\n"
        "    private static final java.util.concurrent.ConcurrentLinkedQueue<SyntaxControlMetric> "
        "SYNTAX_CONTROL_METRICS = new java.util.concurrent.ConcurrentLinkedQueue<>();\n"
        "\n"
        "    public record SyntaxControlMetric(boolean fastSyntaxOnly, List<String> argv, int stderrChars) { }\n"
        "\n"
        "    public static void printSyntaxControlMetrics() {\n"
        "        SyntaxControlMetric metric;\n"
        "        while ((metric = SYNTAX_CONTROL_METRICS.poll()) != null) {\n"
        "            System.err.println(\"CLAVA_SYNTAX_CONTROL {\\\"fast_syntax_only\\\":\" + metric.fastSyntaxOnly()\n"
        "                    + \",\\\"argv\\\":\" + syntaxControlJsonArray(metric.argv())\n"
        "                    + \",\\\"stderr_chars\\\":\" + metric.stderrChars() + \"}\");\n"
        "        }\n"
        "    }",
        "syntax metrics sink",
    )
    helpers = (
        '    private static String syntaxControlJsonArray(List<String> values) {\n'
        '        StringBuilder json = new StringBuilder("[");\n'
        '        for (int i = 0; i < values.size(); i++) {\n'
        '            if (i > 0) {\n'
        '                json.append(\',\');\n'
        '            }\n'
        '            json.append(syntaxControlJsonString(values.get(i)));\n'
        '        }\n'
        '        return json.append(\']\').toString();\n'
        '    }\n\n'
        '    private static String syntaxControlJsonString(String value) {\n'
        '        StringBuilder json = new StringBuilder(value.length() + 2).append(\'"\');\n'
        '        for (int i = 0; i < value.length(); i++) {\n'
        '            char character = value.charAt(i);\n'
        '            switch (character) {\n'
        '            case \'"\' -> json.append("\\\\\\\"");\n'
        '            case \'\\\\\' -> json.append("\\\\\\\\");\n'
        '            case \'\\n\' -> json.append("\\\\n");\n'
        '            case \'\\r\' -> json.append("\\\\r");\n'
        '            case \'\\t\' -> json.append("\\\\t");\n'
        '            default -> {\n'
        '                if (character < 0x20) {\n'
        '                    json.append(String.format(Locale.ROOT, "\\\\u%04x", (int) character));\n'
        '                } else {\n'
        '                    json.append(character);\n'
        '                }\n'
        '            }\n'
        '            }\n'
        '        }\n'
        '        return json.append(\'"\').toString();\n'
        '    }\n\n'
    )
    return replace_once(
        source,
        "    private String processOutput(InputStream inputStream) {",
        helpers + "    private String processOutput(InputStream inputStream) {",
        "JSON argv diagnostics helpers",
    )


def instrument_parse_identity(source: str) -> str:
    source = replace_once(
        source,
        "        FullParseIdentity identity = describeFullParseInputs(inputSources, compilerOptions);\n"
        "        int callOrdinal = FULL_PARSE_CALLS.incrementAndGet();\n",
        "        FullParseIdentity identity = describeFullParseInputs(inputSources, compilerOptions);\n"
        "        List<String> fullParseSourcePaths = inputSources.stream().map(File::getPath).collect(Collectors.toList());\n"
        "        List<String> fullParseCompilerOptions = new ArrayList<>(compilerOptions);\n"
        "        boolean fullParseSyntaxOnly = get(SYNTAX_ONLY);\n"
        "        int callOrdinal = FULL_PARSE_CALLS.incrementAndGet();\n",
        "raw parse identity capture",
    )
    source = replace_once(
        source,
        r'''                            + "\"input_basenames\":%s,\"input_content_sha256\":%s,"
                            + "\"content_hash_phase\":\"after_parse\",\"elapsed_ms\":%.3f,"
                            + "\"outcome\":\"%s\"}%n",
''',
        r'''                            + "\"input_basenames\":%s,\"input_content_sha256\":%s,"
                            + "\"source_paths\":%s,\"compiler_options\":%s,\"syntax_only\":%s,"
                            + "\"content_hash_phase\":\"after_parse\",\"elapsed_ms\":%.3f,"
                            + "\"outcome\":\"%s\"}%n",
''',
        "outer metric raw identity fields",
    )
    source = replace_once(
        source,
        "            long elapsedNanos = System.nanoTime() - started;\n"
        "            identity.finishContentHashes();",
        "            long elapsedNanos = System.nanoTime() - started;\n"
        "            ClangAstDumper.printSyntaxControlMetrics();\n"
        "            identity.finishContentHashes();",
        "post-span syntax diagnostics flush",
    )
    return replace_once(
        source,
        '                    toJsonStringArray(identity.inputContentSha256), elapsedNanos / 1_000_000.0, outcome);',
        '                    toJsonStringArray(identity.inputContentSha256), toJsonStringArray(fullParseSourcePaths),\n'
        '                    toJsonStringArray(fullParseCompilerOptions), fullParseSyntaxOnly,\n'
        '                    elapsedNanos / 1_000_000.0, outcome);',
        "outer metric arguments",
    )


def run_checked(command: list[str], cwd: Path) -> None:
    subprocess.run(command, cwd=cwd, check=True)


def build_native_control(native_root: Path, output_root: Path) -> tuple[Path, dict[str, Any]]:
    revision = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=native_root, text=True).strip()
    status = subprocess.check_output(["git", "status", "--porcelain"], cwd=native_root, text=True).strip()
    if revision != FROZEN_TEXT_NATIVE_REVISION or status:
        raise RuntimeError(f"refusing native source/build drift: revision={revision}, status={status!r}")

    build_dir = native_root / "build"
    source_file = native_root / "src/tool.cpp"
    source_text = source_file.read_text()
    if sha256_file(source_file) != "4807add9b632e5bf982d6b6fea54996edb94e8e8ce3c8459ae32101a941beaaf":
        raise RuntimeError("frozen Text native tool.cpp SHA mismatch")
    overlay_dir = output_root / "preparation/native"
    overlay_dir.mkdir(parents=True)
    overlay_source = overlay_dir / "tool.cpp"
    overlay_source.write_text(native_control_source(source_text))
    object_file = overlay_dir / "tool.cpp.o"
    # ClangResources.getLocalExecutable() resolves a local-build tag to <dir>/tool.
    native_tool = overlay_dir / "tool"

    compile_db = json.loads((build_dir / "compile_commands.json").read_text())
    compile_rows = [row for row in compile_db if Path(row["file"]).resolve() == source_file.resolve()]
    if len(compile_rows) != 1:
        raise RuntimeError(f"expected one frozen tool.cpp compile command, found {len(compile_rows)}")
    compile_row = compile_rows[0]
    compile_argv = shlex.split(compile_row["command"])
    old_obj = "CMakeFiles/tool.dir/src/tool.cpp.o"
    if compile_row["directory"] != str(build_dir) or source_file.as_posix() not in compile_argv or old_obj not in compile_argv:
        raise RuntimeError("frozen tool.cpp compile command no longer matches the expected build")
    compile_argv[compile_argv.index(source_file.as_posix())] = str(overlay_source)
    compile_argv[compile_argv.index(old_obj)] = str(object_file)
    compile_index = compile_argv.index("-c")
    compile_argv[compile_index:compile_index] = ["-iquote", str(native_root / "src")]
    run_checked(compile_argv, build_dir)

    link_argv = shlex.split((build_dir / "CMakeFiles/tool.dir/link.txt").read_text())
    if link_argv.count(old_obj) != 1:
        raise RuntimeError("frozen native link command has an unexpected tool object list")
    link_inputs = []
    for argument in link_argv:
        if argument.endswith(".o"):
            if argument == old_obj:
                continue
            path = Path(argument)
            path = path if path.is_absolute() else build_dir / path
            if not path.is_file():
                raise RuntimeError(f"frozen native link input is missing: {path}")
            link_inputs.append({"path": str(path), "sha256": sha256_file(path)})
    link_argv[link_argv.index(old_obj)] = str(object_file)
    out_index = link_argv.index("-o")
    link_argv[out_index + 1] = str(native_tool)
    dep_indices = [i for i, arg in enumerate(link_argv) if arg.startswith("--dependency-file=")]
    if len(dep_indices) != 1:
        raise RuntimeError("frozen native link command has no unique dependency-file output")
    link_argv[dep_indices[0]] = f"--dependency-file={overlay_dir / 'link.d'}"
    run_checked(link_argv, build_dir)
    if not native_tool.is_file() or not os.access(native_tool, os.X_OK):
        raise RuntimeError("isolated native syntax control binary was not produced")
    provenance = {
        "native_revision": revision,
        "original_tool_sha256": sha256_file(native_root / "build/tool"),
        "source_tool_cpp_sha256": sha256_file(source_file),
        "compile_commands_sha256": sha256_file(build_dir / "compile_commands.json"),
        "link_txt_sha256": sha256_file(build_dir / "CMakeFiles/tool.dir/link.txt"),
        "compile_argv": compile_argv,
        "link_argv": link_argv,
        "frozen_link_object_hashes": link_inputs,
        "overlay_object_sha256": sha256_file(object_file),
        "control_tool_sha256": sha256_file(native_tool),
        "source_quote_include_recreated_with_iquote": str(native_root / "src"),
    }
    (overlay_dir / "native-build-provenance.json").write_text(json.dumps(provenance, indent=2) + "\n")
    return native_tool, provenance


def syntax_probe(original_tool: Path, control_tool: Path, output_root: Path) -> dict[str, Any]:
    probe_dir = output_root / "preparation/probe"
    probe_dir.mkdir(parents=True)
    fixture = probe_dir / "malformed.c"
    fixture.write_text("int main( {\n")

    def invoke(tool: Path, fast: bool) -> subprocess.CompletedProcess[str]:
        argv = [str(tool), "-c", str(fixture), "-id=1", "-system-header-threshold=1"]
        if fast:
            argv.append("-syntax-check-only")
        argv.extend(["--", "-std=c99"])
        return subprocess.run(argv, cwd=probe_dir, capture_output=True, text=True, check=False)

    baseline = invoke(original_tool, False)
    optimized = invoke(control_tool, True)
    baseline_errors = [line for line in baseline.stderr.splitlines() if "error:" in line]
    optimized_errors = [line for line in optimized.stderr.splitlines() if "error:" in line]
    if baseline.returncode == 0 or optimized.returncode == 0 or not baseline_errors or baseline_errors != optimized_errors:
        raise RuntimeError("malformed-input syntax diagnostic probe changed failure behavior")
    return {
        "fixture_sha256": sha256_file(fixture),
        "baseline_status": baseline.returncode,
        "optimized_status": optimized.returncode,
        "diagnostic_error_lines_equal": True,
        "baseline_stderr_chars": len(baseline.stderr),
        "optimized_stderr_chars": len(optimized.stderr),
    }


def overlay_classes(source_runtime: Path, destination_runtime: Path, class_directory: Path,
                    class_entries: list[str], native_tool: Path) -> dict[str, Any]:
    jar = destination_runtime / "lib/ClangAstParser.jar"
    command = ["jar", "uf", str(jar)]
    for entry in class_entries:
        command.extend(["-C", str(class_directory), entry])
    subprocess.run(command, check=True)

    source_jars = {
        path.relative_to(source_runtime).as_posix(): sha256_file(path)
        for path in source_runtime.rglob("*.jar") if path.is_file()
    }
    run_jars = {
        path.relative_to(destination_runtime).as_posix(): sha256_file(path)
        for path in destination_runtime.rglob("*.jar") if path.is_file()
    }
    if set(source_jars) != set(run_jars):
        raise RuntimeError("runtime overlay changed the JAR set")
    for relative, digest in source_jars.items():
        if relative != "lib/ClangAstParser.jar" and run_jars[relative] != digest:
            raise RuntimeError(f"runtime overlay changed unrelated JAR {relative}")

    parser_jar = destination_runtime / "lib/ClangAstParser.jar"
    with zipfile.ZipFile(parser_jar) as archive:
        tag_directory = archive.read("clang-dumper-release.tag").decode("utf-8").strip()
        actual_classes = {name for name in archive.namelist() if name.startswith(PARSER_ENTRY[:-6])
                          or name.startswith(DUMPER_ENTRY[:-6])}
    if Path(tag_directory).resolve() != native_tool.parent.resolve():
        raise RuntimeError("per-run runtime tag does not resolve to the controlled native directory")
    if not {entry for entry in class_entries if entry.endswith(".class")}.issubset(actual_classes):
        raise RuntimeError("runtime parser JAR is missing overlay classes")

    source_jar = source_runtime / "lib/ClangAstParser.jar"
    with zipfile.ZipFile(source_jar) as source_archive, zipfile.ZipFile(parser_jar) as run_archive:
        source_names = set(source_archive.namelist())
        run_names = set(run_archive.namelist())
        if run_names != source_names | set(class_entries):
            raise RuntimeError("parser JAR entry set changed outside the overlay classes")
        allowed_changes = set(class_entries) | {"clang-dumper-release.tag"}
        for entry in source_names - allowed_changes:
            if source_archive.read(entry) != run_archive.read(entry):
                raise RuntimeError(f"parser JAR entry changed outside the overlay: {entry}")
    return {
        "stage_runtime_manifest_matches_except_parser_jar": True,
        "parser_jar_sha256": sha256_file(parser_jar),
        "native_tool": str(native_tool),
        "native_tool_sha256": sha256_file(native_tool),
        "overlay_class_entries": class_entries,
    }


def main() -> int:
    args = parse_args()
    matrix_root = args.matrix_root.resolve()
    diagnostic_root = args.diagnostic_root.resolve()
    output_root = (args.output_root or diagnostic_root / "syntax-only-control-r2").resolve()
    plan = {
        "mode": "plan-only" if not args.host_release_note else "execute",
        "matrix_root": str(matrix_root),
        "diagnostic_root": str(diagnostic_root),
        "output_root": str(output_root),
        "stage": "ccache-text",
        "order": [False, True, True, False, False, True, True, False],
        "pairs": 4,
        "expected_suite_counts": {"total": 164, "passed": 158, "skipped": 6, "failed": 0},
        "parent_host_release_note": args.host_release_note,
    }
    if not args.host_release_note:
        print(json.dumps(plan, indent=2))
        return 0
    if output_root.exists():
        raise SystemExit(f"refusing to overwrite existing output root: {output_root}")
    if not matrix_root.is_dir() or not diagnostic_root.is_dir():
        raise SystemExit("matrix or diagnostic source root is missing")

    scripts_root = matrix_root / "orchestration/experiments/protocol-comparison"
    sys.path.insert(0, str(scripts_root))
    import run_deadline_matrix as matrix
    import run_full_js_parse_diagnostic as diagnostic

    if matrix.DEADLINE_ROOT.resolve() != matrix_root:
        raise SystemExit(f"matrix script resolves a different root: {matrix.DEADLINE_ROOT}")
    os.environ["SPECS_JAVA_LIBS_HOME"] = str(matrix.SPECS_ROOT.resolve())
    os.environ["LARA_FRAMEWORK_HOME"] = str(matrix.LARA_ROOT.resolve())

    comparison = matrix.comparison
    comparison.FIXED_SPECSUTILS_REVISION = matrix.FIXED_SPECSUTILS_REVISION
    comparison.STAGES = tuple(dict(stage) for stage in matrix.STAGE_CONFIG)
    stages = comparison.validate_stages({"ccache-text"})
    if len(stages) != 1:
        raise RuntimeError("expected exactly one original Text stage")
    stage = stages[0]
    output_root.mkdir(parents=True)
    original_tool = Path(stage["dumper"]).resolve()
    native_tool, native_build_provenance = build_native_control(Path(stage["native_root"]).resolve(), output_root)
    probe_result = syntax_probe(original_tool, native_tool, output_root)

    clava_root = Path(stage["root"]) / "clava"
    source_runtime = clava_root / "Clava-JS/java-binaries"
    parser_source_path = clava_root / "ClangAstParser/src/pt/up/fe/specs/clang/codeparser/ParallelCodeParser.java"
    dumper_source_path = clava_root / "ClangAstParser/src/pt/up/fe/specs/clang/dumper/ClangAstDumper.java"
    if sha256_file(parser_source_path) != diagnostic.STAGE_SOURCE_SHA256:
        raise RuntimeError("frozen Text ParallelCodeParser source SHA mismatch")
    if sha256_file(dumper_source_path) != FROZEN_TEXT_DUMPER_SOURCE_SHA256:
        raise RuntimeError("frozen Text ClangAstDumper source SHA mismatch")

    metric_source = diagnostic_root / "source/text/ParallelCodeParser.java"
    if sha256_file(metric_source) != TEXT_PARSER_OVERLAY_SHA256:
        raise RuntimeError("prepared full-suite Text metric source SHA mismatch")

    overlay_src = output_root / "preparation/java-src"
    parser_src = overlay_src / PARSER_ENTRY.replace(".class", ".java")
    dumper_src = overlay_src / DUMPER_ENTRY.replace(".class", ".java")
    parser_src.parent.mkdir(parents=True)
    dumper_src.parent.mkdir(parents=True)
    parser_src.write_text(instrument_parse_identity(metric_source.read_text()))
    dumper_src.write_text(java_dumper_control_source(dumper_source_path.read_text()))
    class_directory = output_root / "preparation/java-classes"
    class_directory.mkdir()
    javac = ["javac", "-classpath", str(source_runtime / "lib/*"), "-d", str(class_directory),
             str(parser_src), str(dumper_src)]
    subprocess.run(javac, check=True)
    class_entries = sorted(path.relative_to(class_directory).as_posix()
                           for path in class_directory.rglob("*.class"))
    if PARSER_ENTRY not in class_entries or DUMPER_ENTRY not in class_entries:
        raise RuntimeError("javac did not produce both controlled classes")

    original_stage_runtime = comparison.stage_runtime
    runtime_gates: list[dict[str, Any]] = []

    def stage_runtime_with_overlay(source: Path, destination: Path, dumper: str | None) -> None:
        original_stage_runtime(source, destination, str(native_tool))
        gate = overlay_classes(source, destination, class_directory, class_entries, native_tool)
        runtime_gates.append(gate)

    comparison.stage_runtime = stage_runtime_with_overlay
    java_option_env = {name: os.environ.get(name, "") for name in
                       ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS")}
    inherited_java_options = {
        name: shlex.split(value) for name, value in java_option_env.items() if value.strip()
    }
    unsafe = [f"{name}:{item}" for name, items in inherited_java_options.items() for item in items
              if item.startswith(("-Xmx", "-Xms", "-javaagent", "-agentlib", "-agentpath"))
              or "ExplicitGC" in item or "DisableExplicitGC" in item]
    if unsafe:
        raise SystemExit(f"requires the inherited default heap, GC and no-agent JVM; found {unsafe}")
    benchmark_flags = [f"{name}:{item}" for name, items in inherited_java_options.items() for item in items
                       if item.startswith(("-Dclava.fullParseMetrics=", "-Dclava.astWireBenchmarkSyntaxOnly="))]
    if benchmark_flags:
        raise SystemExit(f"benchmark properties are set by the runner and must not be inherited: {benchmark_flags}")
    java_options_policy = {
        "inherited": java_option_env,
        "default_heap_gc_and_no_agent": True,
        "heap": "default",
        "explicit_gc": False,
        "agent": None,
    }

    all_rows: list[dict[str, Any]] = []
    results: list[dict[str, Any]] = []
    try:
        for ordinal, fast_syntax in enumerate(plan["order"], 1):
            stage_for_run = dict(stage)
            stage_for_run["dumper"] = str(native_tool)
            repeat = (ordinal + 1) // 2
            base_options = os.environ.get("JAVA_TOOL_OPTIONS", "").strip()
            os.environ["JAVA_TOOL_OPTIONS"] = (
                f"{base_options} -Dclava.fullParseMetrics=true "
                f"-Dclava.astWireBenchmarkSyntaxOnly={'true' if fast_syntax else 'false'}"
            ).strip()
            result = comparison.run_clava_js(
                stage_for_run, output_root, ordinal, measured=True, repeat=repeat, mode="direct"
            )
            expected = plan["expected_suite_counts"]
            for field in ("total_tests", "passed_tests", "skipped_tests", "failed_tests"):
                key = {"total_tests": "total", "passed_tests": "passed", "skipped_tests": "skipped",
                       "failed_tests": "failed"}[field]
                if result.get(field) != expected[key]:
                    raise RuntimeError(f"suite count gate failed in run {ordinal}: {result}")
            if result.get("return_code") != 0 or not result.get("cache_validation", {}).get("passed"):
                raise RuntimeError(f"Text syntax control suite failed in run {ordinal}: {result}")

            run_dir = Path(result["run_dir"])
            log_text = (run_dir / "run.log").read_text(errors="replace")
            syntax_rows = []
            for line in log_text.splitlines():
                marker = "CLAVA_SYNTAX_CONTROL "
                if line.startswith(marker):
                    syntax_rows.append(json.loads(line[len(marker):]))
            if not syntax_rows:
                raise RuntimeError(f"no syntax invocation records in run {ordinal}")
            for row in syntax_rows:
                has_flag = "-syntax-check-only" in row.get("argv", [])
                if row.get("fast_syntax_only") is not fast_syntax or has_flag is not fast_syntax:
                    raise RuntimeError(f"syntax mode/argv mismatch in run {ordinal}: {row}")
                if "-o" in row.get("argv", []):
                    raise RuntimeError(f"syntax validation unexpectedly requested a dump output in run {ordinal}")

            metric_rows = diagnostic.parse_metric_rows(run_dir / "run.log")
            if len(metric_rows) != 300:
                raise RuntimeError(f"expected 300 original full-parse calls, got {len(metric_rows)}")
            for row in metric_rows:
                row.update({"suite": "clava-js", "stage": "ccache-text", "repeat": repeat,
                            "mode": "direct", "fast_syntax_only": fast_syntax})
                if not {"syntax_only", "source_paths", "compiler_options"}.issubset(row):
                    raise RuntimeError(f"raw per-parse identity missing in run {ordinal}")
            metric_path = run_dir / "outer-parse.jsonl"
            metric_path.write_text("".join(json.dumps(row, sort_keys=True) + "\n" for row in metric_rows))
            result.update({"fast_syntax_only": fast_syntax, "syntax_invocations": len(syntax_rows),
                           "outer_parse_records": len(metric_rows), "outer_parse_path": str(metric_path),
                           "outer_parse_sha256": sha256_file(metric_path)})
            (run_dir / "summary.json").write_text(json.dumps(result, indent=2) + "\n")
            results.append(result)
            all_rows.extend(metric_rows)
            print(json.dumps({"run": ordinal, "fast_syntax_only": fast_syntax,
                              "repeat": repeat, "elapsed_s": result.get("elapsed_s"),
                              "syntax_invocations": len(syntax_rows)}, sort_keys=True), flush=True)
    finally:
        comparison.stage_runtime = original_stage_runtime

    (output_root / "outer-parse.jsonl").write_text(
        "".join(json.dumps(row, sort_keys=True) + "\n" for row in all_rows)
    )
    (output_root / "results.json").write_text(json.dumps(results, indent=2) + "\n")
    rows_by_run = {(row["repeat"], row["fast_syntax_only"]): [] for row in all_rows}
    for row in all_rows:
        rows_by_run[(row["repeat"], row["fast_syntax_only"])].append(row)
    pair_summaries = []
    order_variation_ordinals = set()
    for repeat in range(1, 5):
        off_rows = {row["call_ordinal"]: row for row in rows_by_run[(repeat, False)]}
        on_rows = {row["call_ordinal"]: row for row in rows_by_run[(repeat, True)]}
        if set(off_rows) != set(on_rows):
            raise RuntimeError(f"paired call ordinals differ in repeat {repeat}")
        for call_ordinal, off in off_rows.items():
            on = on_rows[call_ordinal]
            if sorted(off["input_content_sha256"]) != sorted(on["input_content_sha256"]):
                raise RuntimeError(f"paired input content sets differ at call {call_ordinal}, repeat {repeat}")
            if off["input_content_sha256"] != on["input_content_sha256"]:
                order_variation_ordinals.add(call_ordinal)
            if off["outcome"] != on["outcome"] or off["syntax_only"] != on["syntax_only"]:
                raise RuntimeError(f"paired call behavior/config differs at call {call_ordinal}, repeat {repeat}")
            pair_summaries.append({
                "repeat": repeat,
                "call_ordinal": call_ordinal,
                "syntax_only": off["syntax_only"],
                "content_set_equal": True,
                "input_order_equal": off["input_content_sha256"] == on["input_content_sha256"],
                "off_elapsed_ms": off["elapsed_ms"],
                "on_elapsed_ms": on["elapsed_ms"],
                "delta_ms_on_minus_off": on["elapsed_ms"] - off["elapsed_ms"],
            })
    bucket_summary = {}
    for syntax_only, label in ((False, "ast_parse"), (True, "syntax_only")):
        deltas = [row["delta_ms_on_minus_off"] for row in pair_summaries if row["syntax_only"] is syntax_only]
        bucket_summary[label] = {
            "paired_calls": len(deltas),
            "sum_delta_ms_on_minus_off": sum(deltas),
            "mean_delta_ms_on_minus_off": sum(deltas) / len(deltas) if deltas else None,
        }
    (output_root / "paired-call-deltas.json").write_text(json.dumps(pair_summaries, indent=2) + "\n")
    (output_root / "execution-manifest.json").write_text(json.dumps({
        **plan,
        "host_release_note": args.host_release_note,
        "native_source_revision": FROZEN_TEXT_NATIVE_REVISION,
        "native_control_sha256": sha256_file(native_tool),
        "native_build_provenance": native_build_provenance,
        "java_options_policy": java_options_policy,
        "syntax_diagnostic_probe": probe_result,
        "runtime_gates": runtime_gates,
        "call_delta_summary": bucket_summary,
        "input_order_variation_ordinals_across_pairs": sorted(order_variation_ordinals),
        "raw_options_and_paths_recorded": True,
        "outer_parse_sha256": sha256_file(output_root / "outer-parse.jsonl"),
        "results_sha256": sha256_file(output_root / "results.json"),
    }, indent=2) + "\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

from __future__ import annotations

from pathlib import Path
import sys
import unittest


sys.path.insert(0, str(Path(__file__).resolve().parent))
import build_overlay  # noqa: E402


PARSER_FIXTURE = '''\
package pt.up.fe.specs.clang.codeparser;

public class ParallelCodeParser extends CodeParser {
    @Override
    public App parse(List<File> inputSources, List<String> compilerOptions, ClavaContext context) {
        Map<String, File> allUserSources = getFileMap(inputSources);
        boolean syntaxOnly = get(SYNTAX_ONLY);
        if (syntaxOnly) {
            return null;
        }
        App app = makeApp(allUserSources);
        new TreeTransformer(ClangAstParser.getTextParsingRules()).transform(app);
        if (get(SHOW_EXEC_INFO)) {
            logExecInfo();
        }
        if (get(SHOW_CLAVA_AST)) {
            logAst(app);
        }
        if (get(SHOW_CODE)) {
            logCode(app);
        }
        return app;
    }

    private Object parseSource() {
        // Braces in comments and literals must not end the outer method: { " } ".
        return null;
    }
}
'''


class BuildOverlayTest(unittest.TestCase):
    def test_classfile_major_reader_and_java17_release_mapping(self) -> None:
        major_61_class_header = bytes.fromhex("cafebabe0000003d")
        self.assertEqual(build_overlay.classfile_major(major_61_class_header), 61)
        self.assertEqual(build_overlay.classfile_major(major_61_class_header) - 44, 17)
        with self.assertRaisesRegex(ValueError, "invalid or truncated"):
            build_overlay.classfile_major(b"not-a-class")

    def test_instrumentation_keeps_parser_body_and_places_timer_anchors(self) -> None:
        transformed, anchors = build_overlay.instrument_source(PARSER_FIXTURE)

        self.assertEqual(anchors, {
            "parse_methods": 1,
            "text_transform_anchors": 1,
            "syntax_only_returns": 1,
            "app_returns": 1,
        })
        method_start = transformed.index("public App parse(")
        body_start = transformed.index("{", method_start)
        timer_start = transformed.index("final long __appBuildStartNanos = System.nanoTime();", body_start)
        self.assertLess(timer_start, transformed.index("__appBuildPreviousShowExecInfo = get(SHOW_EXEC_INFO);"))
        self.assertLess(timer_start, transformed.index("Map<String, File> allUserSources"))

        transform = transformed.index(build_overlay.TEXT_TRANSFORM)
        stop = transformed.index("__appBuildStopNanos = System.nanoTime();", transform)
        self.assertEqual(
            transformed[transform:stop],
            build_overlay.TEXT_TRANSFORM + "\n        ",
        )
        self.assertLess(stop, transformed.index("if (get(SHOW_EXEC_INFO))", stop))
        self.assertLess(stop, transformed.index("if (get(SHOW_CLAVA_AST))", stop))
        self.assertLess(stop, transformed.index("if (get(SHOW_CODE))", stop))

        self.assertLess(
            transformed.index("AppBuildMetrics.recordSyntaxOnly"),
            transformed.index("return null;"),
        )
        self.assertLess(
            transformed.index("AppBuildMetrics.recordSuccess"),
            transformed.index("return app;"),
        )
        self.assertIn("catch (Throwable __appBuildFailure)", transformed)
        self.assertIn("set(SHOW_EXEC_INFO, __appBuildPreviousShowExecInfo);", transformed)
        self.assertIn("if (get(SHOW_CLAVA_AST))", transformed)
        self.assertIn("if (get(SHOW_CODE))", transformed)
        self.assertIn("// Braces in comments and literals", transformed)

    def test_unexpected_source_shape_is_rejected(self) -> None:
        with self.assertRaisesRegex(ValueError, "parse body anchors"):
            build_overlay.instrument_source(PARSER_FIXTURE.replace(
                build_overlay.TEXT_TRANSFORM, "transform(app);"
            ))

    def test_context_tracking_does_not_retain_apps_or_contexts(self) -> None:
        helper = build_overlay.HELPER_SOURCE.read_text(encoding="utf-8")

        self.assertIn("WeakReference<ClavaContext>", helper)
        self.assertIn("candidate.context.get()", helper)
        self.assertIn("iterator.remove()", helper)
        self.assertNotIn("IdentityHashMap<ClavaContext, ContextStamp>", helper)
        self.assertIn('result.put("LIBC_CXX_MODE"', helper)
        self.assertIn('row.put("working_directory"', helper)
        self.assertIn('row.put("metadata_complete"', helper)
        self.assertIn("ManagementFactory.getRuntimeMXBean().getInputArguments()", helper)
        self.assertIn("Runtime.getRuntime().maxMemory()", helper)
        self.assertIn('row.put("jvm_input_arguments"', helper)
        self.assertIn('row.put("jvm_max_memory_bytes"', helper)

    def test_include_directory_audit_is_opt_in_and_success_only(self) -> None:
        helper = build_overlay.HELPER_SOURCE.read_text(encoding="utf-8")
        self.assertIn(
            'auditIncludes = "true".equalsIgnoreCase(environment("APP_BUILD_AUDIT_INCLUDES"));',
            helper,
        )
        success_method = helper[
            helper.index("public static void recordSuccess("):
            helper.index("public static void recordSyntaxOnly(")
        ]
        syntax_method = helper[
            helper.index("public static void recordSyntaxOnly("):
            helper.index("public static void recordFailure(")
        ]
        self.assertIn("originalShowExecInfo, null, auditIncludes);", success_method)
        self.assertIn('"syntax_only", true, originalShowExecInfo, null, false);', syntax_method)

        audit_block_start = helper.index("if (includeAuditEnabled) {")
        audit_block_end = helper.index("List<String> errors = metadataErrors", audit_block_start)
        audit_block = helper[audit_block_start:audit_block_end]
        self.assertIn('row.put("include_directory_audit"', audit_block)
        self.assertIn('row.put("source_parent_audit"', audit_block)
        self.assertEqual(helper.count('row.put("include_directory_audit"'), 1)
        self.assertEqual(helper.count('row.put("source_parent_audit"'), 1)
        self.assertIn("Files.walk(walkRoot, FileVisitOption.FOLLOW_LINKS)", helper)
        self.assertIn('metadata.put("relative_path"', helper)


if __name__ == "__main__":
    unittest.main()

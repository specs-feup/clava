/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.crosstu;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;

import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.util.SpecsSystem;

/** A successful subset must not silently become the analysis group after a transport failure. */
class UsrIncompleteInputRegressionTest {
    @TempDir Path root;

    private App parse(boolean parallel, boolean continueOnErrors) throws Exception {
        SpecsSystem.programStandardInit();
        Files.writeString(root.resolve("supported.c"), "int value=7;");
        // LLVM 18 accepts this type, but Clava does not model BitIntType. The failure is decoding,
        // not invalid C syntax, and used to be swallowed before constructing a partial App.
        Files.writeString(root.resolve("unsupported.c"), "unsigned _BitInt(127) wide;");
        var parser = CodeParser.newInstance();
        parser.set(CodeParser.AST_DUMP_CACHE, false);
        parser.set(CodeParser.SHOW_EXEC_INFO, false);
        parser.set(CodeParser.GENERATED_PARSE_ROOT, root.toFile());
        parser.set(ParallelCodeParser.PARALLEL_PARSING, parallel);
        parser.set(ParallelCodeParser.PARSING_NUM_THREADS, 2);
        parser.set(ParallelCodeParser.CONTINUE_ON_PARSING_ERRORS, continueOnErrors);
        return parser.parse(List.of(root.resolve("supported.c").toFile(), root.resolve("unsupported.c").toFile()),
                List.of("-std=c11"));
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void decodingFailureRefusesAnIncompleteIndexByDefault(boolean parallel) {
        var failure = assertThrows(RuntimeException.class, () -> parse(parallel, false));
        assertTrue(failure.getMessage().contains("incomplete input"), failure::getMessage);
        assertNotNull(failure.getCause());
    }

    @ParameterizedTest @ValueSource(booleans = {false, true})
    void partialAnalysisRequiresExplicitOptInAndRecordsTheOmittedTu(boolean parallel) throws Exception {
        var app = parse(parallel, true);
        assertEquals(1, app.getTranslationUnits().size());
        assertEquals("supported.c", app.getTranslationUnits().get(0).getFile().getName());
        assertEquals(List.of(root.resolve("unsupported.c").toFile()), app.get(App.IGNORED_FILES));
    }
}

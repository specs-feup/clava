/* Copyright 2026 SPeCS. Licensed under the Apache License, Version 2.0. */
package pt.up.fe.specs.clang.wire;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsSystem;

class MemberTemplateFidelityTest {
    @TempDir
    Path temporary;

    @Test
    void keepsEnclosingHeadersBeforeMemberTemplateHeader() throws IOException {
        SpecsSystem.programStandardInit();
        File source = SpecsIo.resourceCopy("cxx/wire_member_template.cpp",
                Files.createDirectories(temporary.resolve("source")).toFile(), false, true);
        App first = parse(source, "first-parse");
        File generated = first.write(Files.createDirectories(temporary.resolve("first-code")).toFile()).get(0);
        String code = Files.readString(generated.toPath());
        assertTrue(code.contains("template<class X>\ntemplate <class V>"), code);
        assertTrue(code.contains("Holder<X>::convert"), code);
        assertTrue(code.contains("PackHolder<Xs...>::count"), code);
        assertTrue(code.contains("SizedHolder<M>::count"), code);
        assertTrue(code.contains("Outer<X, M>::Inner<Y>::method"), code);
        assertTrue(code.contains("Outer<X *, Y>::Inner<Z>::method"), code);

        App second = parse(generated, "second-parse");
        File regenerated = second.write(Files.createDirectories(temporary.resolve("second-code")).toFile()).get(0);
        assertEquals(code, Files.readString(regenerated.toPath()));
    }

    private App parse(File source, String name) throws IOException {
        CodeParser parser = CodeParser.newInstance();
        parser.set(CodeParser.GENERATED_PARSE_ROOT, Files.createDirectories(temporary.resolve(name)).toFile());
        parser.set(CodeParser.AST_DUMP_CACHE, false);
        parser.set(CodeParser.SHOW_EXEC_INFO, false);
        parser.set(ParallelCodeParser.PARALLEL_PARSING, false);
        return parser.parse(List.of(source), List.of("-std=c++17"));
    }
}

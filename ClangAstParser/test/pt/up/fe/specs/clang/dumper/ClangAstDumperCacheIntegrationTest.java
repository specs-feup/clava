/**
 * Copyright 2026 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clang.dumper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clang.ClangAstKeys;
import pt.up.fe.specs.clang.ClangFiles;
import pt.up.fe.specs.clang.ClangResources;
import pt.up.fe.specs.clang.LibcMode;
import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.IntegerLiteral;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.util.SpecsSystem;

class ClangAstDumperCacheIntegrationTest {

    @TempDir
    Path tempFolder;

    @Test
    void cacheHitAvoidsProcessAndMaterializesEquivalentTranslationUnit() throws IOException {
        SpecsSystem.programStandardInit();

        File header = Files.writeString(tempFolder.resolve("header.h"), "#define VALUE 1\n").toFile();
        File source = Files.writeString(tempFolder.resolve("source.cpp"),
                "#include \"header.h\"\nint value = VALUE;\n").toFile();
        CodeParser parserConfig = parserConfig();
        ClangFiles clangFiles = new ClangResources(parserConfig).getClangFiles(LibcMode.BUILTIN_AND_LIBC);
        File workingFolder = tempFolder.resolve("working").toFile();

        ClangAstDumper missDumper = newDumper(parserConfig, clangFiles, workingFolder);
        ClangAstData miss = missDumper.parse(source, "1", Standard.CXX17, config());
        assertNotNull(missDumper.getLastWorkingFolder(), "a cache miss must launch the dumper");

        ClangAstData hit = missDumper.parse(source, "1", Standard.CXX17, config());
        assertNull(missDumper.getLastWorkingFolder(), "a cache hit must not expose a previous working folder");
        assertEquals(miss.get(ClangAstData.TRANSLATION_UNIT).getCode(),
                hit.get(ClangAstData.TRANSLATION_UNIT).getCode());
        assertEquals(miss.get(ClangAstData.ID_TO_FILENAME_MAP), hit.get(ClangAstData.ID_TO_FILENAME_MAP));

        Files.writeString(header.toPath(), "#define VALUE 2\n");
        ClangAstDumper changedHeaderDumper = newDumper(parserConfig, clangFiles, workingFolder);
        ClangAstData changedHeader = changedHeaderDumper.parse(source, "1", Standard.CXX17, config());
        assertNotNull(changedHeaderDumper.getLastWorkingFolder(), "a transitive header change must miss the cache");
        assertEquals("2", changedHeader.get(ClangAstData.TRANSLATION_UNIT).getDescendants(IntegerLiteral.class)
                .stream().findFirst().orElseThrow().getCode());
    }

    @Test
    void effectiveCommandChangeMissesAndShowDumpBypassesCache() throws IOException {
        SpecsSystem.programStandardInit();

        File source = Files.writeString(tempFolder.resolve("source.cpp"), "int value = 1;\n").toFile();
        CodeParser parserConfig = parserConfig();
        ClangFiles clangFiles = new ClangResources(parserConfig).getClangFiles(LibcMode.BUILTIN_AND_LIBC);
        File workingFolder = tempFolder.resolve("working").toFile();

        newDumper(parserConfig, clangFiles, workingFolder).parse(source, "1", Standard.CXX17, config());

        ClangAstDumper changedDumper = newDumper(parserConfig, clangFiles, workingFolder);
        changedDumper.parse(source, "1", Standard.CXX17, config("-DVALUE=2"));
        assertNotNull(changedDumper.getLastWorkingFolder(), "an effective command change must miss the cache");

        ClangAstDumper showDumper = newDumper(parserConfig, clangFiles, workingFolder);
        DataStore showConfig = config();
        showConfig.set(CodeParser.SHOW_CLANG_DUMP, true);
        showDumper.parse(source, "1", Standard.CXX17, showConfig);
        assertNotNull(showDumper.getLastWorkingFolder(), "SHOW_CLANG_DUMP must bypass cached loading");
    }

    @Test
    void failedDumperOutputIsNotReused() throws IOException {
        SpecsSystem.programStandardInit();

        File source = Files.writeString(tempFolder.resolve("source.cpp"), "int value = ;\n").toFile();
        CodeParser parserConfig = parserConfig();
        ClangFiles clangFiles = new ClangResources(parserConfig).getClangFiles(LibcMode.BUILTIN_AND_LIBC);
        File workingFolder = tempFolder.resolve("working").toFile();

        ClangAstDumper firstDumper = newDumper(parserConfig, clangFiles, workingFolder);
        ClangAstData first = firstDumper.parse(source, "1", Standard.CXX17, config());
        assertTrue(first.get(ClangAstData.HAS_ERRORS));

        ClangAstDumper secondDumper = newDumper(parserConfig, clangFiles, workingFolder);
        ClangAstData second = secondDumper.parse(source, "1", Standard.CXX17, config());
        assertTrue(second.get(ClangAstData.HAS_ERRORS));
        assertNotNull(secondDumper.getLastWorkingFolder(), "failed output must not be loaded from the cache");
    }

    @Test
    void includeProbeResultsAreNeverCachedAcrossHeaderCreation() throws IOException {
        SpecsSystem.programStandardInit();

        File source = Files.writeString(tempFolder.resolve("probe.cpp"), """
                #if __has_include("optional.h")
                #include "optional.h"
                int selected = OPTIONAL_VALUE;
                #else
                int selected = 1;
                #endif
                """).toFile();
        CodeParser parserConfig = parserConfig();
        ClangFiles clangFiles = new ClangResources(parserConfig).getClangFiles(LibcMode.BUILTIN_AND_LIBC);
        File workingFolder = tempFolder.resolve("working").toFile();

        ClangAstDumper firstDumper = newDumper(parserConfig, clangFiles, workingFolder);
        ClangAstData first = firstDumper.parse(source, "1", Standard.CXX17, config());
        assertNotNull(firstDumper.getLastWorkingFolder());
        assertEquals("1", first.get(ClangAstData.TRANSLATION_UNIT).getDescendants(IntegerLiteral.class)
                .stream().findFirst().orElseThrow().getCode());

        ClangAstDumper secondDumper = newDumper(parserConfig, clangFiles, workingFolder);
        ClangAstData second = secondDumper.parse(source, "1", Standard.CXX17, config());
        assertNotNull(secondDumper.getLastWorkingFolder(), "a negative include probe must not be cached");

        Files.writeString(tempFolder.resolve("optional.h"), "#define OPTIONAL_VALUE 2\n");
        ClangAstDumper thirdDumper = newDumper(parserConfig, clangFiles, workingFolder);
        ClangAstData third = thirdDumper.parse(source, "1", Standard.CXX17, config());
        assertNotNull(thirdDumper.getLastWorkingFolder(), "probe-sensitive input must remain a cache miss");
        assertEquals("2", third.get(ClangAstData.TRANSLATION_UNIT).getDescendants(IntegerLiteral.class)
                .stream().findFirst().orElseThrow().getCode());
    }

    private CodeParser parserConfig() {
        CodeParser parserConfig = CodeParser.newInstance();
        parserConfig.set(CodeParser.DUMPER_FOLDER, tempFolder.resolve("cache").toFile());
        return parserConfig;
    }

    private ClangAstDumper newDumper(CodeParser parserConfig, ClangFiles clangFiles, File workingFolder) {
        return new ClangAstDumper(false, clangFiles.clangExecutable(), clangFiles.builtinIncludes(),
                clangFiles.systemResourceDir(), parserConfig).setBaseFolder(workingFolder);
    }

    private DataStore config(String... flags) {
        DataStore config = ClangAstKeys.toDataStore(List.of(flags));
        config.set(ClavaNode.CONTEXT, new ClavaContext());
        config.set(ClangAstKeys.LIBC_CXX_MODE, LibcMode.BUILTIN_AND_LIBC);
        return config;
    }
}

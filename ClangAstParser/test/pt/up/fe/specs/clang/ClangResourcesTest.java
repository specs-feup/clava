/**
 * Copyright 2026 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clang;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pt.up.fe.specs.clang.ClangAstWebResource.LocalBuild;
import pt.up.fe.specs.clang.ClangAstWebResource.Release;
import pt.up.fe.specs.clang.codeparser.CodeParser;

public class ClangResourcesTest {

    @TempDir
    Path tempFolder;

    @Test
    public void releaseTagIsParsedAsRelease() {
        var release = assertInstanceOf(Release.class, ClangAstWebResource.parseDumperSource("v16.0.5_3"));

        assertEquals("v16.0.5_3", release.tag());
    }

    @Test
    public void absolutePathIsParsedAsLocalBuild() {
        var localBuild = assertInstanceOf(LocalBuild.class,
                ClangAstWebResource.parseDumperSource(tempFolder.toString()));

        assertEquals(tempFolder.toFile(), localBuild.folder());
    }

    @Test
    public void relativePathIsRejected() {
        assertThrows(RuntimeException.class,
                () -> ClangAstWebResource.parseDumperSource("../clang-dumper/build"));
    }

    @Test
    public void localBuildSelectsExpectedTool() throws IOException {
        var toolName = SupportedPlatform.getCurrentPlatform().isWindows() ? "tool.exe" : "tool";
        var tool = tempFolder.resolve(toolName).toFile();
        assertTrue(tool.createNewFile());

        assertEquals(tool, ClangResources.getLocalExecutable(tempFolder.toFile()));
    }

    @Test
    public void localBuildRequiresExpectedTool() {
        assertThrows(RuntimeException.class, () -> ClangResources.getLocalExecutable(tempFolder.toFile()));
    }

    @Test
    public void builtinCudaIncludesAreAvailableWithSystemLibc() {
        var parser = CodeParser.newInstance();
        parser.set(CodeParser.CUDA_PATH, CodeParser.getBuiltinOption());

        var clangFiles = new ClangResources(parser).getClangFiles(LibcMode.SYSTEM);
        var hasCudaWrapper = clangFiles.builtinIncludes().stream()
                .map(folder -> new File(folder, "__clang_cuda_runtime_wrapper.h"))
                .anyMatch(File::isFile);

        assertTrue(hasCudaWrapper, "Built-in CUDA must provide Clang's CUDA runtime wrapper independently of libc");
    }
}

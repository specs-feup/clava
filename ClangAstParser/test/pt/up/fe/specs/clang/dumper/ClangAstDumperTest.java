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

package pt.up.fe.specs.clang.dumper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pt.up.fe.specs.clang.ClangResources;
import pt.up.fe.specs.clang.codeparser.CodeParser;

public class ClangAstDumperTest {

    @TempDir
    Path tempFolder;

    @Test
    public void builtinCudaUsesArchiveWithEmptyBuiltinIncludes() {
        var parser = CodeParser.newInstance();
        parser.set(CodeParser.DUMPER_FOLDER, tempFolder.toFile());
        parser.set(CodeParser.CUDA_PATH, CodeParser.getBuiltinOption());

        var resourceFolder = new ClangResources(parser).getClangResourceFolder();
        var expectedCudaFolder = new File(resourceFolder, "cudalib");
        var dumper = new ClangAstDumper(false, tempFolder.resolve("clang").toFile(), List.of(), parser);
        var arguments = new ArrayList<String>();

        dumper.addCudaPathArgument(arguments, parser.get(CodeParser.CUDA_PATH));

        assertEquals(List.of("--cuda-path=" + expectedCudaFolder.getAbsolutePath()), arguments);
        assertTrue(new File(expectedCudaFolder, "include/cuda_runtime.h").isFile());
    }
}

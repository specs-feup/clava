/**
 * Copyright 2017 SPeCS.
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

package pt.up.fe.specs.clang.parser.tests;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

import pt.up.fe.specs.clang.ClangAstKeys;
import pt.up.fe.specs.clang.ClangResources;
import pt.up.fe.specs.clang.LibcMode;
import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.parser.CxxCudaTester;

/** Verifies built-in CUDA parsing through the pinned NVIDIA redistribution packages. */
public class CxxCudaTest {
    @TempDir
    static Path cudaCacheFolder;

    @Test
    public void testAtomicAddWithBuiltinLibc() {
        newCudaTester("atomicAdd.cu")
                .set(ClangAstKeys.LIBC_CXX_MODE, LibcMode.BUILTIN_AND_LIBC)
                .test();
    }

    @Test
    public void testAtomicAddWithSystemLibc() {
        newCudaTester("atomicAdd.cu")
                .set(ClangAstKeys.LIBC_CXX_MODE, LibcMode.SYSTEM)
                .onePass()
                .test();
    }

    @Test
    public void testConvolutionCache() {
        newCudaTester("convolution_cache.cu").test();
    }

    @Test
    public void testMultMatrix() {
        newCudaTester("mult_matrix.cu").test();
    }

    @Test
    public void testStreamAdd() {
        newCudaTester("streamAdd.cu").test();
    }

    @Test
    public void testSumArrays() {
        newCudaTester("sumArrays.cu").showCode().test();
    }

    private static CxxCudaTester newCudaTester(String input) {
        assumeTrue(ClangResources.isBuiltinCudaSupported(),
                "Built-in CUDA tests require a supported host platform");
        var tester = new CxxCudaTester(input);
        tester.set(CodeParser.DUMPER_FOLDER, cudaCacheFolder.toFile());
        return tester;
    }
}

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
 * specific language governing permissions and limitations under the License. under the License.
 */

package eu.antarex.clang.parser.tests;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import eu.antarex.clang.parser.AClangAstTester;
import eu.antarex.clang.parser.CxxCudaTester;

public class CxxCudaTest {

    @BeforeClass
    public static void setup() throws Exception {
        AClangAstTester.clear();
    }

    @After
    public void tearDown() throws Exception {
        AClangAstTester.clear();
    }

    @Test
    public void testAtomicAdd() {
        // .addFlags(
        // // "-std=cuda",
        // // "-fms-compatibility", "-D_MSC_VER", "-D_LIBCPP_MSVCRT",
        // // "--cuda-gpu-arch=sm_30",
        // // "--cuda-device-only",
        // // "-ferror-limit=10000",
        // "-ferror-limit=10000")
        // "--cuda-path=/usr/local/cuda-11.4")
        // "/tmp/clang_ast_exe_osboxes/cuda")

        // "--cuda-path=C:\\Program Files\\NVIDIA GPU Computing Toolkit\\CUDA\\v11.3")
        //
        // .addFlags("-x", "cuda", "--cuda-path=C:\\Program Files\\NVIDIA GPU Computing
        // Toolkit\\CUDA\\v11.3",
        // "-nocudalib", "-nocudainc",
        // "--cuda-device-only")
        // .onePass()
        // .showClavaAst()

        new CxxCudaTester("atomicAdd.cu").test();

    }

    @Test
    public void testConvolutionCache() {
        new CxxCudaTester("convolution_cache.cu").test();
    }

    @Test
    public void testMultMatrix() {
        new CxxCudaTester("mult_matrix.cu").test();
    }

    @Test
    public void testStreamAdd() {
        new CxxCudaTester("streamAdd.cu").test();
    }

    @Test
    public void testSumArrays() {
        new CxxCudaTester("sumArrays.cu").showCode().test();
    }

}

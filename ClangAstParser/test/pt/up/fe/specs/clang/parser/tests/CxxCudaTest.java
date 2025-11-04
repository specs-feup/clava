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

import org.junit.jupiter.api.Test;

import pt.up.fe.specs.clang.parser.CxxCudaTester;

/**
 * Disabled tests, they are failing in the CI server. Even when passing the --cuda-path built-in library, the parser
 * fails to find the CUDA library.
 * 
 * @author JBispo
 *
 */
public class CxxCudaTest {
    @Test
    public void testAtomicAdd() {
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

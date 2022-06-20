/**
 * Copyright 2016 SPeCS.
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

package pt.up.fe.specs.cxxweaver.tests;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.cxxweaver.ClavaWeaverTester;
import pt.up.fe.specs.lang.SpecsPlatforms;
import pt.up.fe.specs.util.SpecsSystem;

public class CudaTest {

    @BeforeClass
    public static void setupOnce() {
        SpecsSystem.programStandardInit();
        ClavaWeaverTester.clean();
    }

    @After
    public void tearDown() {
        ClavaWeaverTester.clean();
    }

    private static ClavaWeaverTester newTester() {
        var cudaTester = new ClavaWeaverTester("clava/test/weaver/", Standard.CUDA)
                .setResultPackage("cuda/results")
                .setSrcPackage("cuda/src")
                .set(CodeParser.CUDA_PATH, CodeParser.getBuiltinOption());

        // Windows currently not supported
        if (SpecsPlatforms.isWindows()) {
            cudaTester.doNotRun();
        }

        return cudaTester;
    }

    @Test
    public void testCuda() {
        newTester().test("Cuda.lara", "atomicAdd.cu");
    }

    @Test
    public void testCudaMatrixMul() {
        newTester().test("CudaMatrixMul.lara", "mult_matrix.cu");
    }

    @Test
    public void testCudaQuery() {
        newTester().test("CudaQuery.lara", "sample.cu");
    }
}

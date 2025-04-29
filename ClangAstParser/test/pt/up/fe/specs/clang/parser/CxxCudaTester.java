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

package pt.up.fe.specs.clang.parser;

import java.util.Arrays;
import java.util.List;

import pt.up.fe.specs.lang.SpecsPlatforms;

public class CxxCudaTester extends AClangAstTester {

    public CxxCudaTester(String... files) {
        this(Arrays.asList(files));
    }

    public CxxCudaTester(List<String> files) {
        // super("cxx/cuda", files, Arrays.asList("-std=cuda"));
        super("cxx/cuda", files);

        // Windows currently not supported
        if (SpecsPlatforms.isWindows()) {
            doNotRun();
        }

        enableBuiltinCuda();
    }

}

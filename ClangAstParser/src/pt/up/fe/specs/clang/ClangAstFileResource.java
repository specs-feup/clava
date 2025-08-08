/**
 * Copyright 2018 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clang;

import pt.up.fe.specs.util.providers.FileResourceProvider;

import java.util.function.Supplier;

public enum ClangAstFileResource implements Supplier<FileResourceProvider> {

    BUILTIN_INCLUDES(ClangAstWebResource.BUILTIN_INCLUDES),
    //LIBC_CXX(ClangAstWebResource.LIBC_CXX),
    //LIBC_CXX_WINDOWS(ClangAstWebResource.LIBC_CXX_WINDOWS),
    CUDA_LIB(ClangAstWebResource.CUDA_LIB),
    WIN_EXE(ClangAstWebResource.WIN_EXE),
    WIN_DLL1(ClangAstWebResource.WIN_DLL1),
    WIN_DLL2(ClangAstWebResource.WIN_DLL2),
    WIN_LLVM_DLL(ClangAstWebResource.WIN_LLVM_DLL),
    LINUX_EXE(ClangAstWebResource.LINUX_EXE),
    MAC_OS_EXE(ClangAstWebResource.MAC_OS_EXE);

    private final FileResourceProvider provider;

    ClangAstFileResource(FileResourceProvider provider) {
        this.provider = provider;
    }

    @Override
    public FileResourceProvider get() {
        return provider;
    }
}

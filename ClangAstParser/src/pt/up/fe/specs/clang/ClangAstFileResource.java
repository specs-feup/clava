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

    LIBC_CXX_LLVM(ClangAstWebResource.LIBC_CXX_LLVM),
    LIBC_CXX_WIN32(ClangAstWebResource.LIBC_CXX_WIN32),
    LIBC_CXX_LINUX(ClangAstWebResource.LIBC_CXX_LINUX),
    LIBC_CXX_LINUX_COMPLETE(ClangAstWebResource.LIBC_CXX_LINUX_COMPLETE),
    OPENMP_INCLUDES(ClangAstWebResource.OPENMP_INCLUDES),
    CUDA_LIB(ClangAstWebResource.CUDA_LIB),
    WIN_EXE(ClangAstWebResource.WIN_EXE),
    WIN_DLL1(ClangAstWebResource.WIN_DLL1),
    WIN_DLL2(ClangAstWebResource.WIN_DLL2),
    WIN_DLL3(ClangAstWebResource.WIN_DLL3),
    WIN_DLL4(ClangAstWebResource.WIN_DLL4),
    WIN_DLL5(ClangAstWebResource.WIN_DLL5),
    WIN_DLL6(ClangAstWebResource.WIN_DLL6),
    WIN_DLL7(ClangAstWebResource.WIN_DLL7),
    WIN_DLL8(ClangAstWebResource.WIN_DLL8),
    WIN_DLL9(ClangAstWebResource.WIN_DLL9),
    WIN_CLANG_DLL(ClangAstWebResource.WIN_CLANG_DLL),
    WIN_LLVM_DLL(ClangAstWebResource.WIN_LLVM_DLL),
    LINUX_EXE(ClangAstWebResource.LINUX_EXE),
    LINUX_PLUGIN(ClangAstWebResource.LINUX_PLUGIN),
    LINUX_LLVM_DLL(ClangAstWebResource.LINUX_LLVM_DLL),
    MAC_OS_EXE(ClangAstWebResource.MAC_OS_EXE),
    MAC_OS_LLVM_DLL(ClangAstWebResource.MAC_OS_LLVM_DLL),
    MAC_OS_DLL1(ClangAstWebResource.MAC_OS_DLL1);

    private final FileResourceProvider provider;

    ClangAstFileResource(FileResourceProvider provider) {
        this.provider = provider;
    }

    @Override
    public FileResourceProvider get() {
        return provider;
    }
}

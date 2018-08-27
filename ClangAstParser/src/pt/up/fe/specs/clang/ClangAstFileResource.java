/**
 * Copyright 2018 SPeCS.
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

package pt.up.fe.specs.clang;

import java.util.function.Supplier;

import pt.up.fe.specs.util.providers.FileResourceProvider;

public enum ClangAstFileResource implements Supplier<FileResourceProvider> {

    BUILTIN_INCLUDES_3_8(ClangAstWebResource.BUILTIN_INCLUDES_3_8),
    LIBC_CXX_WINDOWS(ClangAstWebResource.LIBC_CXX_WINDOWS),
    LIBC_CXX_MAC_OS(ClangAstWebResource.LIBC_CXX_MAC_OS),
    LIBC_CXX_LINUX(ClangAstWebResource.LIBC_CXX_LINUX),
    WIN_EXE(ClangAstWebResource.WIN_EXE),
    WIN_DLL1(ClangAstWebResource.WIN_DLL1),
    WIN_DLL2(ClangAstWebResource.WIN_DLL2),
    WIN_DLL3(ClangAstWebResource.WIN_DLL3),
    LINUX_EXE(ClangAstWebResource.LINUX_EXE),
    CENTOS6_EXE(ClangAstWebResource.CENTOS6_EXE),
    MAC_OS_EXE(ClangAstWebResource.MAC_OS_EXE);

    private final FileResourceProvider provider;

    private ClangAstFileResource(FileResourceProvider provider) {
        this.provider = provider;
    }

    @Override
    public FileResourceProvider get() {
        return provider;
    }
}

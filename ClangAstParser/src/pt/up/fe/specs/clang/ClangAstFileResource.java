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

    BUILTIN_INCLUDES(ClangAstWebResource.BUILTIN_INCLUDES),
    LIBC_CXX(ClangAstWebResource.LIBC_CXX),
    // LIBC_CXX_WINDOWS(ClangAstWebResource.LIBC_CXX_WINDOWS),
    // LIBC_CXX_MAC_OS(ClangAstWebResource.LIBC_CXX_MAC_OS),
    // LIBC_CXX_LINUX(ClangAstWebResource.LIBC_CXX_LINUX),
    // LIBC_CXX_CENTOS6(ClangAstWebResource.LIBC_CXX_CENTOS6),
    WIN_EXE(ClangAstWebResource.WIN_EXE),
    WIN_DLL1(ClangAstWebResource.WIN_DLL1),
    WIN_DLL2(ClangAstWebResource.WIN_DLL2),
    LINUX_EXE(ClangAstWebResource.LINUX_EXE),
    LINUX_ARMV7_EXE(ClangAstWebResource.LINUX_ARMV7_EXE),
    CENTOS_EXE(ClangAstWebResource.CENTOS_EXE),
    MAC_OS_EXE(ClangAstWebResource.MAC_OS_EXE);

    private final FileResourceProvider provider;

    private ClangAstFileResource(FileResourceProvider provider) {
        this.provider = provider;
        // this(provider, false);
    }

    // private ClangAstFileResource(FileResourceProvider provider, boolean withVersion) {
    // this.provider = provider.createResourceVersion("_" + provider.getVersion());
    // }

    @Override
    public FileResourceProvider get() {
        return provider;
    }
}

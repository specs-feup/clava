/**
 * Copyright 2016 SPeCS.
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

import pt.up.fe.specs.util.providers.WebResourceProvider;

public interface ClangAstWebResource {

    static WebResourceProvider create_old(String resourceUrl, String version) {
        return WebResourceProvider.newInstance("https://specs.fe.up.pt/resources/clangast/", resourceUrl, version);
    }

    static WebResourceProvider create_old(String resourceUrl) {
        return WebResourceProvider.newInstance("https://specs.fe.up.pt/resources/clangast/", resourceUrl);
    }


    static WebResourceProvider create(String resourceUrl, String version) {
        return WebResourceProvider.newInstance("https://github.com/specs-feup/clava/releases/download/", resourceUrl, version);
    }

    static WebResourceProvider create(String resourceUrl) {
        return create(resourceUrl, "1.0");
    }

    WebResourceProvider BUILTIN_INCLUDES = create("clang_ast_dumper_v12.0.7.1/clang_builtin_includes_v12.0.1.zip", "v12.0.1");

    WebResourceProvider LIBC_CXX = create("clang_ast_dumper_v12.0.7.1/libc_cxx.zip", "v12.0.1");

    WebResourceProvider LIBC_CXX_WINDOWS = create("clang_ast_dumper_v12.0.7.1/libc_cxx_windows.zip", "v12.0.1");

    // For some reason, on Linux, this file no longer downloads
    WebResourceProvider CUDA_LIB = create("clang_ast_dumper_v12.0.7.1/cudalib.zip", "v11.3.0");

    WebResourceProvider WIN_EXE = create("clang_ast_dumper_v12.0.7.1/clang_ast_windows.exe", "v12.0.7");
    WebResourceProvider WIN_DLL1 = create("clang_ast_dumper_v12.0.7.1/libwinpthread-1.dll");
    WebResourceProvider WIN_DLL2 = create("clang_ast_dumper_v12.0.7.1/zlib1.dll");

    WebResourceProvider LINUX_EXE = create("clang_ast_dumper_v12.0.7.1/clang_ast_ubuntu", "v12.0.7.1");

    WebResourceProvider MAC_OS_EXE = create("clang_ast_dumper_v12.0.7.1/clang_ast_macos", "v12.0.7");

}

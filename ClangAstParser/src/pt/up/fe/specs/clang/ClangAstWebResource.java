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

    String ROOT_16_0_5 = "https://github.com/specs-feup/clava/releases/download/clang_ast_dumper_16.0.5/";
    String ROOT_12_0_7 = "https://github.com/specs-feup/clava/releases/download/clang_ast_dumper_v12.0.7.1/";

    WebResourceProvider BUILTIN_INCLUDES = WebResourceProvider.newInstance(ROOT_16_0_5, "libc_cxx.zip", "v16.0.5");

    //WebResourceProvider LIBC_CXX = WebResourceProvider.newInstance(ROOT_16_0_5, "libc_cxx.zip", "v14.0.6");

    //WebResourceProvider LIBC_CXX_WINDOWS = WebResourceProvider.newInstance(ROOT_16_0_5, "libc_cxx.zip", "v14.0.6");

    WebResourceProvider CUDA_LIB = WebResourceProvider.newInstance(ROOT_12_0_7, "cudalib.zip", "v11.3.0");

    WebResourceProvider WIN_EXE = WebResourceProvider.newInstance(ROOT_16_0_5, "clang_ast_windows.exe", "v16.0.5.0");
    WebResourceProvider WIN_DLL1 = WebResourceProvider.newInstance(ROOT_12_0_7, "libwinpthread-1.dll");
    WebResourceProvider WIN_DLL2 = WebResourceProvider.newInstance(ROOT_12_0_7, "zlib1.dll");

    WebResourceProvider LINUX_EXE = WebResourceProvider.newInstance(ROOT_16_0_5, "clang_ast_ubuntu", "v12.0.7.1");
    WebResourceProvider WIN_LLVM_DLL = WebResourceProvider.newInstance(ROOT_16_0_5, "libLLVM-16.dll");

    WebResourceProvider MAC_OS_EXE = WebResourceProvider.newInstance("https://github.com/specs-feup/clava/releases/download/clang_ast_dumper_v12.0.7.1/", "clang_ast_macos", "v12.0.7");

}

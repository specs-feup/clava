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

package pt.up.fe.specs.clang;

import java.util.Arrays;
import java.util.List;

import pt.up.fe.specs.util.providers.WebResourceProvider;

public interface ClangAstWebResource {

    static WebResourceProvider create(String resourceUrl, String version) {
        return WebResourceProvider.newInstance("http://specs.fe.up.pt/resources/clangast/", resourceUrl, version);
    }

    static WebResourceProvider create(String resourceUrl) {
        return WebResourceProvider.newInstance("http://specs.fe.up.pt/resources/clangast/", resourceUrl);
    }

    WebResourceProvider BUILTIN_INCLUDES_3_8 = create("clang_builtin_includes_3.8.zip", "v1.1");

    WebResourceProvider LIBC_CXX_WINDOWS = create("libc_cxx.zip", "v2.1");
    WebResourceProvider LIBC_CXX_MAC_OS = create("libc_cxx_mac_os.zip", "v1.0");

    WebResourceProvider WIN_EXE = create("windows/clang_ast.exe", "v2.12");
    WebResourceProvider WIN_DLL1 = create("windows/libgcc_s_seh-1.dll");
    WebResourceProvider WIN_DLL2 = create("windows/libstdc++-6.dll");
    WebResourceProvider WIN_DLL3 = create("windows/libwinpthread-1.dll");

    WebResourceProvider LINUX_EXE = create("linux_ubuntu_14/clang_ast", "v2.12");

    WebResourceProvider CENTOS6_EXE = create("centos6/clang_ast", "v2.12");

    WebResourceProvider MAC_OS_EXE = create("macos/clang_ast", "v2.12");

    // private static final String BASE_URL = "http://specs.fe.up.pt/resources/clangast/";
    // private static final String BASE_URL = "http://192.168.55.89/resources/clangast/";
    /*
    private final String urlString;
    private final String version;
    
    private ClangAstWebResource(String path) {
        this(path, "v1.0");
    }
    
    private ClangAstWebResource(String path, String version) {
        this.urlString = path;
        this.version = version;
    }
    */
    // @Override
    // public String getUrlString() {
    // return urlString;
    // }
    /*
    @Override
    public String getResourceUrl() {
        return urlString;
    }
    
    @Override
    public String getRootUrl() {
        return BASE_URL;
    }
    
    @Override
    public String getVersion() {
        return version;
    }
    */
    static List<WebResourceProvider> getWindowsResources() {
        return Arrays.asList(WIN_DLL1, WIN_DLL2, WIN_DLL3);
    }
}

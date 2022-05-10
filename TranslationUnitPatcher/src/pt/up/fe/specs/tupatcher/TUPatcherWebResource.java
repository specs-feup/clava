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

package pt.up.fe.specs.tupatcher;

import pt.up.fe.specs.util.providers.WebResourceProvider;

public interface TUPatcherWebResource {

    static WebResourceProvider create(String resourceUrl, String version) {
        return WebResourceProvider.newInstance("http://specs.fe.up.pt/resources/tupatcher/", resourceUrl, version);
    }

    static WebResourceProvider create(String resourceUrl) {
        return WebResourceProvider.newInstance("http://specs.fe.up.pt/resources/tupatcher/", resourceUrl);
    }

    WebResourceProvider WIN_EXE = create("tu_dumper_windows.exe", "v1.0");
    WebResourceProvider LINUX_4_EXE = create("tu_dumper_ubuntu", "v1.0");
    WebResourceProvider LINUX_5_EXE = create("tu_dumper_linux5", "v1.0");

}

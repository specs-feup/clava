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

package pt.up.fe.specs.clava.weaver;

import java.util.List;

import pt.up.fe.specs.util.providers.ResourceProvider;

public class ClavaLaraApis {

    private static final List<ResourceProvider> CLAVA_LARA_API = ResourceProvider
            .getResourcesFromEnum(LaraWeaverApiResource.class, LaraApiResource.class);

    // .getResourcesFromEnum(ClavaApiJsResource.class, LaraWeaverApiResource.class, LaraApiResource.class);
    public static List<ResourceProvider> getApis() {
        return CLAVA_LARA_API;
    }

}

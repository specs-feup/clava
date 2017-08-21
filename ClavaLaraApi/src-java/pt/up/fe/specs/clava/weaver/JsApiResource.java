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

package pt.up.fe.specs.clava.weaver;

import pt.up.fe.specs.util.providers.ResourceProvider;

public enum JsApiResource implements ResourceProvider {
    HDF5("HDF5.js"),
    TYPES("Types.js");

    private final static String BASE_PACKAGE = "clava/js/";

    private final String resource;

    private JsApiResource(String resource) {
        this.resource = BASE_PACKAGE + resource;
    }

    @Override
    public String getResource() {
        return resource;
    }

}

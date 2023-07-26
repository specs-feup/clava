
/**
 * Copyright 2023 SPeCS.
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

import org.lara.interpreter.weaver.utils.LaraResourceProvider;

/**
 * This file has been automatically generated.
 * 
 * @author Joao Bispo, Luis Sousa
 *
 */
public enum ClavaApiJsResource implements LaraResourceProvider {

    JOINPOINTS_JS("Joinpoints.js"),
    CLAVA_JS("clava/Clava.js"),
    CLAVAJAVATYPES_JS("clava/ClavaJavaTypes.js"),
    CLAVATYPE_JS("clava/ClavaType.js"),
    CLAVADATASTORE_JS("clava/util/ClavaDataStore.js"),
    CORE_JS("core.js"),
    JOINPOINTS_JS_1("weaver/JoinPoints.js");

    private final String resource;

    private static final String WEAVER_PACKAGE = "clava/";

    /**
     * @param resource
     */
    private ClavaApiJsResource (String resource) {
      this.resource = WEAVER_PACKAGE + getSeparatorChar() + resource;
    }

    /* (non-Javadoc)
     * @see org.suikasoft.SharedLibrary.Interfaces.ResourceProvider#getResource()
     */
    @Override
    public String getOriginalResource() {
        return resource;
    }

}

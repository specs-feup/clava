/**
 * Copyright 2013 SuikaSoft.
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
 * @author Joao Bispo
 *
 */
public enum LaraWeaverApiResource implements LaraResourceProvider {
    CLAVA_ELSE_JP("jp/ClavaElseJp.js"),
    CLAVA_FILE_JP("jp/ClavaFileJp.js"),
    CLAVA_LOOP_JP("jp/ClavaLoopJp.js"),
    CLAVA_IF_JP("jp/ClavaIfJp.js"),
    CLAVA_BINARY_JP("jp/ClavaBinaryJp.js"),
    CLAVA_CONSTRUCTOR_CALL_JP("jp/ClavaConstructorCallJp.js"),
    CLAVA_CONSTRUCTOR_JP("jp/ClavaConstructorJp.js"),
    CLAVA_PARAM_JP("jp/ClavaParamJp.js"),
    CLAVA_VAR_REF_JP("jp/ClavaVarRefJp.js"),
    CLAVA_VAR_DECL_JP("jp/ClavaVarDeclJp.js"),
    CLAVA_TYPE_JP("jp/ClavaTypeJp.js"),
    CLAVA_FIELD_REF_JP("jp/ClavaFieldRefJp.js"),
    CLAVA_FIELD_JP("jp/ClavaFieldJp.js"),
    CLAVA_MEMBER_CALL_JP("jp/ClavaMemberCallJp.js"),
    CLAVA_CALL_JP("jp/ClavaCallJp.js"),
    CLAVA_FUNCTION_JP("jp/ClavaFunctionJp.js"),
    CLAVA_METHOD_JP("jp/ClavaMethodJp.js"),
    CLAVA_CLASS_JP("jp/ClavaClassJp.js"),
    CLAVA_JOIN_POINT("jp/ClavaJoinPoint.js"),
    COMMON_JOIN_POINTS("jp/CommonJoinPoints.js"),
    JOIN_POINTS("JoinPoints.js"),
    WEAVER_LAUNCHER("WeaverLauncher.js");

    private final String resource;

    private static final String WEAVER_PACKAGE = "clava/";
    private static final String BASE_PACKAGE = "weaver/";

    /**
     * @param resource
     */
    private LaraWeaverApiResource(String resource) {
        this.resource = WEAVER_PACKAGE + getSeparatorChar() + BASE_PACKAGE + resource;
    }

    /* (non-Javadoc)
     * @see org.suikasoft.SharedLibrary.Interfaces.ResourceProvider#getResource()
     */
    @Override
    public String getOriginalResource() {
        return resource;
    }

}

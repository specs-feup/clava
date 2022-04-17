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
	CLAVA_ELSE_JP("jp/ClavaElseJp.lara"),
	CLAVA_FILE_JP("jp/ClavaFileJp.lara"),
	CLAVA_LOOP_JP("jp/ClavaLoopJp.lara"),
	CLAVA_IF_JP("jp/ClavaIfJp.lara"),
	CLAVA_BINARY_JP("jp/ClavaBinaryJp.lara"),
	CLAVA_CONSTRUCTOR_CALL_JP("jp/ClavaConstructorCallJp.lara"),
	CLAVA_CONSTRUCTOR_JP("jp/ClavaConstructorJp.lara"),
	CLAVA_PARAM_JP("jp/ClavaParamJp.lara"),
	CLAVA_VAR_REF_JP("jp/ClavaVarRefJp.lara"),
    CLAVA_VAR_DECL_JP("jp/ClavaVarDeclJp.lara"),    
    CLAVA_DECL_JP("jp/ClavaDeclJp.lara"),    
	CLAVA_TYPE_JP("jp/ClavaTypeJp.lara"),
	CLAVA_FIELD_REF_JP("jp/ClavaFieldRefJp.lara"),
	CLAVA_FIELD_JP("jp/ClavaFieldJp.lara"),
	CLAVA_MEMBER_CALL_JP("jp/ClavaMemberCallJp.lara"),
	CLAVA_CALL_JP("jp/ClavaCallJp.lara"),
	CLAVA_FUNCTION_JP("jp/ClavaFunctionJp.lara"),
	CLAVA_METHOD_JP("jp/ClavaMethodJp.lara"),
    CLAVA_CLASS_JP("jp/ClavaClassJp.lara"),
    CLAVA_CLASS_TYPE_JP("jp/ClavaClassTypeJp.lara"),
    CLAVA_INTERFACE_JP("jp/ClavaInterfaceJp.lara"),
    CLAVA_JOIN_POINT("jp/ClavaJoinPoint.lara"),
    COMMON_JOIN_POINTS("jp/CommonJoinPoints.lara"),
    JOIN_POINTS("JoinPoints.lara"),
    WEAVER_LAUNCHER("WeaverLauncher.lara");

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

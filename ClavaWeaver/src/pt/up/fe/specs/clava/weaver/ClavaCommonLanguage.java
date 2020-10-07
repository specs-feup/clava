/**
 * Copyright 2020 SPeCS.
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

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.util.classmap.ClassMap;

public class ClavaCommonLanguage {

    private static final ClassMap<ClavaNode, String> JOINPOINT_MAPPER;
    static {
        JOINPOINT_MAPPER = new ClassMap<>();
        JOINPOINT_MAPPER.put(CallExpr.class, "CallJp");
        JOINPOINT_MAPPER.put(Expr.class, "ExprJp");
        JOINPOINT_MAPPER.put(FunctionDecl.class, "FunctionJp");
        JOINPOINT_MAPPER.put(Decl.class, "DeclJp");
        JOINPOINT_MAPPER.put(TranslationUnit.class, "FileJp");
        JOINPOINT_MAPPER.put(App.class, "ProgramJp");
        JOINPOINT_MAPPER.put(ClavaNode.class, "JoinPoint");
    }

    public static String getJoinPointName(ClavaNode node) {
        return JOINPOINT_MAPPER.get(node.getClass());
    }

}

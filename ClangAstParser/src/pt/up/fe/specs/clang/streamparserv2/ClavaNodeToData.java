/**
 * Copyright 2018 SPeCS.
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

package pt.up.fe.specs.clang.streamparserv2;

import java.util.HashMap;
import java.util.Map;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.CXXConstructorDecl;
import pt.up.fe.specs.clava.ast.decl.CXXConversionDecl;
import pt.up.fe.specs.clava.ast.decl.CXXDestructorDecl;
import pt.up.fe.specs.clava.ast.decl.CXXMethodDecl;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.decl.data2.CXXMethodDeclDataV2;
import pt.up.fe.specs.clava.ast.decl.data2.ClavaData;
import pt.up.fe.specs.clava.ast.decl.data2.DeclDataV2;
import pt.up.fe.specs.clava.ast.decl.data2.FunctionDeclDataV2;
import pt.up.fe.specs.clava.ast.decl.data2.ParmVarDeclData;
import pt.up.fe.specs.clava.ast.decl.data2.VarDeclDataV2;
import pt.up.fe.specs.clava.ast.expr.CharacterLiteral;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.ImplicitCastExpr;
import pt.up.fe.specs.clava.ast.expr.data2.CastExprData;
import pt.up.fe.specs.clava.ast.expr.data2.CharacterLiteralData;
import pt.up.fe.specs.clava.ast.expr.data2.ExprDataV2;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.stmt.data.StmtData;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.data2.TypeDataV2;

public class ClavaNodeToData {

    private static final Map<Class<? extends ClavaNode>, Class<? extends ClavaData>> CLAVA_NODE_TO_DATA;
    static {
        CLAVA_NODE_TO_DATA = new HashMap<>();

        // DECL
        CLAVA_NODE_TO_DATA.put(FunctionDecl.class, FunctionDeclDataV2.class);
        CLAVA_NODE_TO_DATA.put(CXXConstructorDecl.class, CXXMethodDeclDataV2.class);
        CLAVA_NODE_TO_DATA.put(CXXConversionDecl.class, CXXMethodDeclDataV2.class);
        CLAVA_NODE_TO_DATA.put(CXXDestructorDecl.class, CXXMethodDeclDataV2.class);
        CLAVA_NODE_TO_DATA.put(CXXMethodDecl.class, CXXMethodDeclDataV2.class);
        CLAVA_NODE_TO_DATA.put(VarDecl.class, VarDeclDataV2.class);
        CLAVA_NODE_TO_DATA.put(ParmVarDecl.class, ParmVarDeclData.class);

        // EXPR
        CLAVA_NODE_TO_DATA.put(ImplicitCastExpr.class, CastExprData.class);
        CLAVA_NODE_TO_DATA.put(CharacterLiteral.class, CharacterLiteralData.class);
        // CLAVA_NODE_TO_DATA.put(FloatingLiteral.class, LiteralData.class);
        // CLAVA_NODE_TO_DATA.put(IntegerLiteral.class, LiteralData.class);
        // CLAVA_NODE_TO_DATA.put(StringLiteral.class, LiteralData.class);
    }

    public static Class<? extends ClavaData> getClavaDataClass(Class<? extends ClavaNode> clavaNodeClass) {
        // Check if class is in map
        Class<? extends ClavaData> clavaDataClass = CLAVA_NODE_TO_DATA.get(clavaNodeClass);
        if (clavaDataClass != null) {
            return clavaDataClass;
        }

        // Default to base class for each category
        if (Decl.class.isAssignableFrom(clavaNodeClass)) {
            return DeclDataV2.class;
        }

        if (Expr.class.isAssignableFrom(clavaNodeClass)) {
            return ExprDataV2.class;
        }

        if (Stmt.class.isAssignableFrom(clavaNodeClass)) {
            return StmtData.class;
        }

        if (Type.class.isAssignableFrom(clavaNodeClass)) {
            return TypeDataV2.class;
        }

        throw new RuntimeException("Case not defined for class '" + clavaNodeClass + "'");
    }
}

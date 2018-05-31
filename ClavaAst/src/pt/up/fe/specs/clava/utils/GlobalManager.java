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

package pt.up.fe.specs.clava.utils;

import java.util.HashSet;
import java.util.Set;

import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.decl.data.VarDeclData;
import pt.up.fe.specs.clava.ast.decl.enums.InitializationStyle;
import pt.up.fe.specs.clava.ast.decl.enums.StorageClass;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.language.TLSKind;

public class GlobalManager {

    Set<String> definedVars;

    public GlobalManager() {

        definedVars = new HashSet<>();
    }

    public VarDecl addGlobal(TranslationUnit tunit, String name, Type type, Expr initValue) {

        boolean isDefined = definedVars.contains(name);
        if (!isDefined) {
            definedVars.add(name);
        }

        InitializationStyle initStyle = isDefined ? InitializationStyle.NO_INIT : InitializationStyle.CINIT;
        Expr initExpr = isDefined ? null : initValue;

        boolean isUsed = true;
        boolean isImplicit = false;
        boolean isNrvo = false;

        VarDeclData varDeclData = new VarDeclData(StorageClass.NONE, TLSKind.NONE, false, isNrvo, initStyle, false);
        DeclData declData = new DeclData(false, isImplicit, isUsed, false, false, false);

        VarDecl varDecl = ClavaNodeFactory.varDecl(varDeclData, name, type, declData, ClavaNodeInfo.undefinedInfo(),
                initExpr);

        tunit.addDeclaration(varDecl);

        return varDecl;
    }
}

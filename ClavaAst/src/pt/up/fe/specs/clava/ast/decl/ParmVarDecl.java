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

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;
import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.decl.data.VarDeclData;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.SpecsCollections;

public class ParmVarDecl extends VarDecl {

    private final boolean hasInheritedDefaultArg;

    public ParmVarDecl(boolean hasInheritedDefaultArg, VarDeclData data, String varName, Type type, DeclData declData,
            ClavaNodeInfo info,
            Expr initExpr) {

        this(hasInheritedDefaultArg, data, varName, type, declData, info, SpecsCollections.ofNullable(initExpr));
    }

    protected ParmVarDecl(boolean hasInheritedDefaultArg, VarDeclData data, String varName, Type type,
            DeclData declData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {

        super(data, varName, type, declData, info, children);

        this.hasInheritedDefaultArg = hasInheritedDefaultArg;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new ParmVarDecl(hasInheritedDefaultArg, getVarDeclData(), getDeclName(), getType(), getDeclData(),
                getInfo(), Collections.emptyList());
    }

    /**
     * 
     * @return the FunctionDecl where this ParmVarDecl appears
     */
    public FunctionDecl getFunctionDecl() {
        return getAncestor(FunctionDecl.class);
    }

    @Override
    public String getInitializationCode() {
        if (hasInheritedDefaultArg) {
            return "";
        }

        return super.getInitializationCode();
    }

    /*
    @Override
    public String getDeclName() {
        // ParmVarDecl can have a null name
        return getDeclNameInternal();
    }
    */
}

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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints;

import java.util.Optional;

import pt.up.fe.specs.clava.ast.decl.DeclaratorDecl;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.ast.expr.MSPropertyRefExpr;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ADecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ADeclarator;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVardecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVarref;

public class CxxVarref<Self extends CxxVarref<Self>> extends AVarref<Self> {

    public CxxVarref(DeclRefExpr refExpr, CxxWeaver weaver) {
        super(refExpr, weaver);
    }

    @Override
    public DeclRefExpr getNodeImpl() {
        return (DeclRefExpr) super.getNodeImpl();
    }

    @Override
    public String getNameImpl() {
        return this.getNodeImpl().getRefName();
    }

    @Override
    public void setNameImpl(String name) {
        this.getNodeImpl().setRefName(name);
    }

    @Override
    public String getKindImpl() {
        return this.getNodeImpl().getKind().name().toLowerCase();
    }

    @Override
    public AExpression<?> getUseExprImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().getUseExpr(), getWeaverEngine(), AExpression.class);
    }

    @Override
    public AVardecl<?> getVardeclImpl() {
        ADeclarator<?> declarator = getDeclarationImpl();

        return declarator instanceof AVardecl ? (AVardecl<?>) declarator : null;
    }

    @Override
    public boolean getIsFunctionCallImpl() {
        return this.getNodeImpl().isFunctionCall();
    }

    @Override
    public ADeclarator<?> getDeclarationImpl() {
        Optional<DeclaratorDecl> declarator = this.getNodeImpl().getVariableDeclaration();

        if (!declarator.isPresent()) {
            return null;
        }

        return CxxJoinpoints.create(declarator.get(), getWeaverEngine(), ADeclarator.class);
    }

    @Override
    public ADecl<?> getDeclImpl() {
        return getVardeclImpl();
    }

    @Override
    public String getPropertyImpl() {
        var parent = this.getNodeImpl().getParent();

        if (parent == null) {
            return null;
        }

        if (!(parent instanceof MSPropertyRefExpr)) {
            return null;
        }

        return ((MSPropertyRefExpr) parent).getProperty();
    }

    @Override
    public boolean getHasPropertyImpl() {
        if (!this.getNodeImpl().hasParent()) {
            return false;
        }

        // If parent is a MSPropertyRefExpr, this this varref has a MS-style property
        return this.getNodeImpl().getParent() instanceof MSPropertyRefExpr;
    }

}

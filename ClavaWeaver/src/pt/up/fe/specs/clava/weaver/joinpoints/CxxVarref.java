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

package pt.up.fe.specs.clava.weaver.joinpoints;

import java.util.List;
import java.util.Optional;

import pt.up.fe.specs.clava.ast.decl.DeclaratorDecl;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ADeclarator;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVardecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVarref;

public class CxxVarref extends AVarref {

    private final DeclRefExpr refExpr;

    public CxxVarref(DeclRefExpr refExpr) {
        super(new CxxExpression(refExpr));

        this.refExpr = refExpr;
    }

    @Override
    public DeclRefExpr getNode() {
        return refExpr;
    }

    @Override
    public String getNameImpl() {
        return refExpr.getRefName();
    }

    @Override
    public void defNameImpl(String value) {
        refExpr.setRefName(value);
    }

    @Override
    public void setNameImpl(String name) {
        defNameImpl(name);
    }

    @Override
    public String getKindImpl() {
        return refExpr.getKind().name().toLowerCase();
    }

    @Override
    public AExpression getUseExprImpl() {
        return CxxJoinpoints.create(refExpr.getUseExpr(), AExpression.class);
    }

    /*
    @Override
    public String getUseImpl() {
        ExprUse use = ((Expr) getUseExprImpl().getNode()).use();

        return CxxAttributes.convertUse(use);
    }
    */

    @Override
    public AVardecl getVardeclImpl() {
        ADeclarator declarator = getDeclarationImpl();

        return declarator instanceof AVardecl ? (AVardecl) declarator : null;
    }

    @Override
    public List<? extends AVardecl> selectVardecl() {
        return CxxExpression.selectVarDecl(this);
    }

    @Override
    public Boolean getIsFunctionCallImpl() {
        return refExpr.isFunctionCall();
    }

    @Override
    public ADeclarator getDeclarationImpl() {
        Optional<DeclaratorDecl> declarator = refExpr.getVariableDeclaration();

        if (!declarator.isPresent()) {
            return null;
        }

        return CxxJoinpoints.create(declarator.get(), ADeclarator.class);
    }
}

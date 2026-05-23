/**
 * Copyright 2016 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.stmt.ExprStmt;
import pt.up.fe.specs.clava.weaver.CxxAttributes;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ACast;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ADecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinpoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AStatement;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVardecl;
import pt.up.fe.specs.clava.weaver.enums.ExpressionUse;
import pt.up.fe.specs.util.SpecsLogs;

public class CxxExpression<Self extends CxxExpression<Self>> extends AExpression<Self> {

    public CxxExpression(Expr expr, CxxWeaver weaver) {
        super(expr, weaver);
    }

    @Override
    public Expr getNodeImpl() {
        return (Expr) super.getNodeImpl();
    }

    @Override
    public AVardecl<?> getVardeclImpl() {
        // Get more specific join point for current node
        SpecsLogs.msgInfo("attribute 'vardecl' not implemented yet for joinpoint " + joinPointType());
        return null;
    }

    @Override
    public ExpressionUse getUseImpl() {
        return CxxAttributes.convertUse(this.getNodeImpl().use());
    }

    public static List<? extends AVardecl<?>> selectVarDecl(AExpression<?> expression) {
        AVardecl<?> vardecl = expression.getVardeclImpl();
        if (vardecl == null) {
            return Collections.emptyList();
        }

        return Arrays.asList(vardecl);
    }

    @Override
    public boolean getIsFunctionArgumentImpl() {
        return this.getNodeImpl().isFunctionArgument();
    }

    @Override
    public ACast<?> getImplicitCastImpl() {
        // // Check if expr has an implicit cast
        // expr.hasValue(key)

        return this.getNodeImpl().getImplicitCast()
                .map(castExpr -> CxxJoinpoints.create(castExpr,
                        getWeaverEngine(), ACast.class))
                .orElse(null);
    }

    @Override
    public ADecl<?> getDeclImpl() {
        return this.getNodeImpl().getDecl()
                .map(decl -> CxxJoinpoints.create(decl,
                        getWeaverEngine(), ADecl.class))
                .orElse(null);
    }

    @Override
    public AJoinpoint<?> replaceWithImpl(AJoinpoint<?> node) {
        // If node to replace is statement, check if this expression is inside an ExprStmt
        if (node instanceof AStatement<?> && node.getNodeImpl().getParent() instanceof ExprStmt) {
            return node.getParentImpl().replaceWithImpl(node);
        }

        return super.replaceWithImpl(node);
    }

}

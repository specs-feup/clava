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

package pt.up.fe.specs.clava.weaver.joinpoints;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVardecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.enums.AExpressionUseEnum;

public class CxxExpression extends AExpression {

    private final Expr expr;
    private final ACxxWeaverJoinPoint parent;

    public CxxExpression(Expr expr, ACxxWeaverJoinPoint parent) {
        this.expr = expr;
        this.parent = parent;
    }

    @Override
    public ACxxWeaverJoinPoint getParentImpl() {
        return parent;
    }

    @Override
    public ClavaNode getNode() {
        return expr;
    }

    /*
    @Override
    public String getRefnameImpl() {
        // Ignore nodes such as ImplicitCasts
        ClavaNode normalizedNode = getNodeNormalized();
        if (!(normalizedNode instanceof DeclRefExpr)) {
            return null;
        }
    
        return ((DeclRefExpr) expr).getRefName();
    }
    */

    @Override
    public AJoinPoint getVardeclImpl() {
        // Get more specific join point for current node

        // SpecsLogs.msgInfo("attribute 'vardecl' not implemented yet for joinpoint " + getJoinPointType());
        return null;
        /*
        // DeclRefExpr declRefExpr = toDeclRefExpr(expr);
        // if (declRefExpr == null) {
        // return null;
        // }
        if (!(expr instanceof DeclRefExpr)) {
            return null;
        }
        
        Optional<DeclaratorDecl> varDecl = ((DeclRefExpr) expr).getVariableDeclaration();
        // Optional<DeclaratorDecl> varDecl = declRefExpr.getVariableDeclaration();
        
        if (!varDecl.isPresent()) {
            return null;
        }
        
        return CxxJoinpoints.create(varDecl.get(), null);
        */
    }

    /*
    private DeclRefExpr toDeclRefExpr(ClavaNode node) {
        if (node instanceof DeclRefExpr) {
            return (DeclRefExpr) node;
        }
    
        if (node.getNumChildren() == 1) {
            return toDeclRefExpr(node.getChild(0));
        }
    
        return null;
    }
    */

    @Override
    public String getUseImpl() {
        switch (expr.use()) {
        case READ:
            return AExpressionUseEnum.READ.getName();
        case WRITE:
            return AExpressionUseEnum.WRITE.getName();
        case READWRITE:
            return AExpressionUseEnum.READWRITE.getName();
        default:
            throw new RuntimeException("Case not defined:" + expr.use());
        }

    }

    public static List<? extends AVardecl> selectVarDecl(AExpression expression) {
        AVardecl vardecl = (AVardecl) expression.getVardeclImpl();
        if (vardecl == null) {
            return Collections.emptyList();
        }

        return Arrays.asList(vardecl);
    }

    @Override
    public List<? extends AVardecl> selectVardecl() {
        return selectVarDecl(this);
    }

    @Override
    public Boolean getIsFunctionArgumentImpl() {
        return expr.isFunctionArgument();
    }

}

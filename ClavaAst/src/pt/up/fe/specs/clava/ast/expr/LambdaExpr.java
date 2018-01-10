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

package pt.up.fe.specs.clava.ast.expr;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.CXXMethodDecl;
import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;
import pt.up.fe.specs.clava.ast.expr.data.LambdaExprData;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;

public class LambdaExpr extends Expr {

    private final LambdaExprData lambdaData;

    public LambdaExpr(LambdaExprData lambdaData, ExprData exprData, ClavaNodeInfo info,
            CXXRecordDecl lambdaClass, CompoundStmt body) {

        this(lambdaData, exprData, info, Arrays.asList(lambdaClass, body));
    }

    private LambdaExpr(LambdaExprData lambdaData, ExprData exprData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {

        super(exprData, info, children);

        this.lambdaData = lambdaData;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new LambdaExpr(lambdaData, getExprData(), getInfo(), Collections.emptyList());
    }

    public CXXRecordDecl getLambdaClass() {
        return getChild(CXXRecordDecl.class, 0);
    }

    public CompoundStmt getBody() {
        return getChild(CompoundStmt.class, 1);
    }

    public LambdaExprData getLambdaData() {
        return lambdaData;
    }

    @Override
    public String getCode() {
        System.out.println("LAMBDA CLASS:\n" + getLambdaClass());

        CXXRecordDecl lambdaClass = getLambdaClass();
        List<CXXMethodDecl> operatorsPar = lambdaClass.getMethod("operator()");
        Preconditions.checkArgument(operatorsPar.size() == 1, "Expected size to be 1, is " + operatorsPar.size());
        CXXMethodDecl operatorPar = operatorsPar.get(0);
        String params = operatorPar.getParameters().stream().map(ClavaNode::getCode).collect(Collectors.joining(", "));

        StringBuilder code = new StringBuilder();

        // Build parameters
        code.append("(").append(params).append(")");

        code.append(" -> ");

        code.append(getBody().getCode());

        return code.toString();
    }

}

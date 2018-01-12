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

import java.util.ArrayList;
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
import pt.up.fe.specs.clava.ast.expr.data.lambda.LambdaCaptureKind;
import pt.up.fe.specs.clava.ast.expr.data.lambda.LambdaExprData;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.collections.SpecsList;

public class LambdaExpr extends Expr {

    private final LambdaExprData lambdaData;

    public LambdaExpr(LambdaExprData lambdaData, ExprData exprData, ClavaNodeInfo info,
            CXXRecordDecl lambdaClass, List<Expr> captureArguments, CompoundStmt body) {

        this(lambdaData, exprData, info, SpecsList.newInstance(ClavaNode.class)
                .concat(lambdaClass).concat(captureArguments).concat(body));
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
        return getChild(CompoundStmt.class, getNumChildren() - 1);
    }

    public List<Expr> getCaptureArguments() {
        int startIndex = 1;
        int endIndex = getNumChildren() - 1;

        return SpecsCollections.cast(getChildren().subList(startIndex, endIndex), Expr.class);
    }

    public LambdaExprData getLambdaData() {
        return lambdaData;
    }

    @Override
    public String getCode() {

        // System.out.println("LAMBDA CLASS:\n" + getLambdaClass());
        // System.out.println("LAMBDA DATA:" + lambdaData);

        String captureCode = getCaptureCode();

        CXXRecordDecl lambdaClass = getLambdaClass();
        List<CXXMethodDecl> operatorsPar = lambdaClass.getMethod("operator()");
        Preconditions.checkArgument(operatorsPar.size() == 1, "Expected size to be 1, is " + operatorsPar.size());
        CXXMethodDecl operatorPar = operatorsPar.get(0);
        String params = operatorPar.getParameters().stream().map(ClavaNode::getCode).collect(Collectors.joining(", "));
        // System.out.println("LAMBDA METHOD TYPE:" + operatorPar.getFunctionType());
        // System.out.println("LAMBDA METHOD TYPE DATA:" + operatorPar.getFunctionDeclData());
        // System.out.println("EXPE: " + operatorPar.getFunctionType().getFunctionProtoTypeData().getSpecifier());
        // System.out.println("LAMBDA CLASS:" + lambdaClass);
        StringBuilder code = new StringBuilder();

        // Add capture
        code.append(captureCode);

        // Build parameters
        code.append("(").append(params).append(")");

        code.append(" ");

        if (lambdaData.isMutable()) {
            code.append("mutable ");
        }

        if (lambdaData.isHasExplicitResultType()) {
            code.append("-> ");
            code.append(operatorPar.getReturnType().getCode()).append(" ");
        }

        CompoundStmt body = getBody();
        boolean inline = body.getStatements().size() == 1;
        code.append(getBody().getCode(inline));

        return code.toString();
    }

    private String getCaptureCode() {
        StringBuilder capture = new StringBuilder();

        List<String> captureElements = new ArrayList<>();

        // Add default, if present
        lambdaData.getCaptureDefault().getCode().ifPresent(captureElements::add);

        // Add captures, if present
        List<Expr> captureArgs = getCaptureArguments();
        for (int i = 0; i < captureArgs.size(); i++) {
            LambdaCaptureKind kind = lambdaData.getCaptureKinds().get(i);
            captureElements.add(kind.getCode(captureArgs.get(i).getCode()));
        }

        // // Check capture default
        // switch (lambdaData.getCaptureDefault()) {
        // case BY_COPY:
        // captureElements.add("=");
        // break;
        // case BY_REF:
        //
        //
        // }
        // if(lambdaData.getCaptureDefault() == LambdaCaptureDefault.BY_COPY) {
        // captureElements.add("=")
        // } else if() {

        // }

        capture.append("[").append(captureElements.stream().collect(Collectors.joining(", "))).append("]");

        return capture.toString();
    }

}

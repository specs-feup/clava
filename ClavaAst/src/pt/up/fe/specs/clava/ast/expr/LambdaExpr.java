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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.ast.expr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Interfaces.DataStore;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.CXXMethodDecl;
import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
import pt.up.fe.specs.clava.ast.decl.enums.InitializationStyle;
import pt.up.fe.specs.clava.ast.expr.enums.LambdaCaptureDefault;
import pt.up.fe.specs.clava.ast.expr.enums.LambdaCaptureKind;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.util.SpecsCollections;

/**
 * A C++ lambda expression, which produces a function object (of unspecified type) that can be invoked later.
 * 
 * @author JoaoBispo
 *
 */
public class LambdaExpr extends LambdaExprBase {

    // Data declarations are generated from the wire schema in LambdaExprBase.

    public LambdaExpr(DataStore data, Collection<? extends ClavaNode> children, LambdaExprBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public LambdaExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public CXXRecordDecl getLambdaClass() {
        return getWireLambdaClass();
    }

    public CompoundStmt getBody() {
        return getChild(CompoundStmt.class, getNumChildren() - 1);
    }

    public List<Expr> getCaptureArguments() {
        int startIndex = 0;
        int endIndex = getNumChildren() - 1;

        return SpecsCollections.cast(getChildren().subList(startIndex, endIndex), Expr.class);
    }

    @Override
    public String getCode() {

        String captureCode = getCaptureCode();

        CXXRecordDecl lambdaClass = getLambdaClass();
        List<CXXMethodDecl> operatorsPar = lambdaClass.getMethod("operator()");
        Preconditions.checkArgument(operatorsPar.size() == 1, "Expected size to be 1, is " + operatorsPar.size());
        CXXMethodDecl operatorPar = operatorsPar.get(0);
        String params = operatorPar.getParameters().stream().map(ClavaNode::getCode).collect(Collectors.joining(", "));

        StringBuilder code = new StringBuilder();

        // Add capture
        code.append(captureCode);

        // Build parameters
        code.append("(").append(params).append(")");

        code.append(" ");

        if (getWireIsMutable()) {
            code.append("mutable ");
        }

        if (getWireHasExplicitResultType()) {
            code.append("-> ");
            code.append(operatorPar.getReturnType().getCode(this)).append(" ");
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
        getWireCaptureDefault().getCode().ifPresent(captureElements::add);

        // Add captures, if present
        List<Expr> captureArgs = getCaptureArguments();
        List<LambdaCaptureKind> captureKinds = getWireCaptureKinds();
        List<String> initCaptureNames = getWireInitCaptureNames();
        List<InitializationStyle> captureInitStyles = getWireCaptureInitStyles();
        List<Boolean> capturePackExpansions = getWireCapturePackExpansions();
        List<Boolean> captureIsImplicit = getWireCaptureIsImplicit();
        Preconditions.checkArgument(captureKinds.size() == captureArgs.size(),
                "Expected one capture kind per initializer, got %s kinds and %s initializers",
                captureKinds.size(), captureArgs.size());
        Preconditions.checkArgument(initCaptureNames.size() == captureArgs.size(),
                "Expected one init-capture name per initializer, got %s names and %s initializers",
                initCaptureNames.size(), captureArgs.size());
        Preconditions.checkArgument(captureInitStyles.size() == captureArgs.size(),
                "Expected one capture init style per initializer, got %s styles and %s initializers",
                captureInitStyles.size(), captureArgs.size());
        Preconditions.checkArgument(capturePackExpansions.size() == captureArgs.size(),
                "Expected one pack-expansion flag per initializer, got %s flags and %s initializers",
                capturePackExpansions.size(), captureArgs.size());
        Preconditions.checkArgument(captureIsImplicit.size() == captureArgs.size(),
                "Expected one implicit-capture flag per initializer, got %s flags and %s initializers",
                captureIsImplicit.size(), captureArgs.size());
        for (int i = 0; i < captureArgs.size(); i++) {
            if (captureIsImplicit.get(i)) {
                continue;
            }

            LambdaCaptureKind kind = captureKinds.get(i);
            String name = initCaptureNames.get(i);
            Expr captureArgument = captureArgs.get(i);
            boolean isPackExpansion = capturePackExpansions.get(i);
            String captureCode;
            if (name.isEmpty()) {
                captureCode = getRegularCaptureCode(captureArgument, isPackExpansion);
            } else {
                captureCode = getInitCaptureCode(name, captureArgument, captureInitStyles.get(i), isPackExpansion);
            }
            captureElements.add(kind.getCode(captureCode));
        }

        capture.append("[").append(captureElements.stream().collect(Collectors.joining(", "))).append("]");

        return capture.toString();
    }

    private static String getRegularCaptureCode(Expr captureArgument, boolean isPackExpansion) {
        if (!isPackExpansion) {
            return captureArgument.getCode();
        }

        // Clang stores regular pack captures in a one-element ParenListExpr. The capture-level ellipsis is
        // represented by LambdaCapture::isPackExpansion(), rather than by a PackExpansionExpr child.
        if (captureArgument instanceof ParenListExpr parenList) {
            Preconditions.checkArgument(parenList.getExpressions().size() == 1,
                    "Expected one expression in a regular pack capture, got %s", parenList.getExpressions().size());
            captureArgument = parenList.getExpressions().get(0);
        }

        if (captureArgument instanceof PackExpansionExpr) {
            return captureArgument.getCode();
        }

        return captureArgument.getCode() + "...";
    }

    private static String getInitCaptureCode(String name, Expr initializer, InitializationStyle style,
            boolean isPackExpansion) {
        String captureName = isPackExpansion ? "..." + name : name;
        String initializerCode = initializer.getCode();

        switch (style) {
        case CINIT:
            return captureName + " = " + initializerCode;
        case CALL_INIT:
            return captureName + (initializer instanceof ParenListExpr ? initializerCode : "(" + initializerCode + ")");
        case LIST_INIT:
            return captureName + (initializer instanceof InitListExpr ? initializerCode : "{" + initializerCode + "}");
        case ParenListInit:
            return captureName + (initializer instanceof ParenListExpr ? initializerCode : "(" + initializerCode + ")");
        default:
            throw new IllegalStateException("Unsupported lambda init-capture style: " + style);
        }
    }

}

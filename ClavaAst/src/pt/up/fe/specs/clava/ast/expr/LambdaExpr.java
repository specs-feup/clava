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
import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.CXXMethodDecl;
import pt.up.fe.specs.clava.ast.decl.CXXRecordDecl;
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
public class LambdaExpr extends Expr {

    /// DATAKEYS BEGIN

    public final static DataKey<Boolean> IS_GENERIC_LAMBDA = KeyFactory.bool("isGenericLambda");

    public final static DataKey<Boolean> IS_MUTABLE = KeyFactory.bool("isMutable");

    public final static DataKey<Boolean> HAS_EXPLICIT_PARAMETERS = KeyFactory.bool("hasExplicitParameters");

    public final static DataKey<Boolean> HAS_EXPLICIT_RESULT_TYPE = KeyFactory.bool("hasExplicitResultType");

    public final static DataKey<LambdaCaptureDefault> CAPTURE_DEFAULT = KeyFactory.enumeration("captureDefault",
            LambdaCaptureDefault.class);

    public final static DataKey<CXXRecordDecl> LAMBDA_CLASS = KeyFactory.object("lambdaClass", CXXRecordDecl.class);

    public final static DataKey<List<LambdaCaptureKind>> CAPTURE_KINDS = KeyFactory.generic("captureKinds",
            new ArrayList<LambdaCaptureKind>());

    /// DATAKEYS END

    public LambdaExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public CXXRecordDecl getLambdaClass() {
        return get(LAMBDA_CLASS);
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

        if (get(IS_MUTABLE)) {
            code.append("mutable ");
        }

        if (get(HAS_EXPLICIT_RESULT_TYPE)) {
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
        get(CAPTURE_DEFAULT).getCode().ifPresent(captureElements::add);

        // Add captures, if present
        List<Expr> captureArgs = getCaptureArguments();
        List<LambdaCaptureKind> captureKinds = get(CAPTURE_KINDS);
        for (int i = 0; i < captureArgs.size(); i++) {
            LambdaCaptureKind kind = captureKinds.get(i);
            captureElements.add(kind.getCode(captureArgs.get(i).getCode()));
        }

        capture.append("[").append(captureElements.stream().collect(Collectors.joining(", "))).append("]");

        return capture.toString();
    }

}

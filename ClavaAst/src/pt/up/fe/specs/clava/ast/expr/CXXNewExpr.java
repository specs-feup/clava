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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.ast.expr;

import java.util.Collection;
import java.util.Optional;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.expr.enums.NewInitStyle;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

/**
 * Represents a new-expression e.g: "new Object(foo)".
 * 
 * @author João Bispo
 *
 */
public class CXXNewExpr extends CXXNewExprBase {

    // Data declarations are generated from the wire schema in CXXNewExprBase.

    public CXXNewExpr(DataStore data, Collection<? extends ClavaNode> children, CXXNewExprBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public CXXNewExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Optional<CXXConstructExpr> getConstructorExpr() {
        return getWireConstructExpr();
    }

    public Optional<Expr> getArrayExpr() {
        return getWireArraySize();
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        code.append("new ");

        boolean hasNothrow = getWireOperatorNew().flatMap(fdecl -> SpecsCollections.lastTry(fdecl.getParameters()))
                .filter(param -> param.getCode().startsWith("std::nothrow_"))
                .isPresent();

        if (hasNothrow) {
            code.append("(std::nothrow) ");
        }

        Optional<CXXConstructExpr> constructorExpr = getConstructorExpr();

        if (constructorExpr.isPresent()) {

            // Special case: literal
            Expr expr = constructorExpr.get();
            if (expr instanceof Literal) {
                Type exprType = getExprType();

                if (exprType.isPointer()) {
                    exprType = exprType.getPointeeType();
                }

                code.append(exprType.getCode(this)).append("(").append(expr.getCode()).append(")");
            } else {
                code.append(expr.getCode());
            }

        } else {
            Type exprType = getExprType();

            if (exprType.isPointer()) {
                exprType = exprType.getPointeeType();
            }

            code.append(exprType.getCode(this));

            switch (getWireInitStyle()) {
            case NO_INIT:
                // Do nothing
                break;
            case CALL_INIT:
                code.append("(").append(getWireInitializer().get().getCode()).append(")");
                break;
            case LIST_INIT:
                // TODO: This code generation is not yet verified
                code.append(getWireInitializer().get().getCode());
                break;
            default:
                throw new NotImplementedException(getWireInitStyle());
            }

            getArrayExpr().ifPresent(arrayExpr -> code.append("[").append(arrayExpr.getCode()).append("]"));

        }

        return code.toString();
    }
}

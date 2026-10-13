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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.FieldDecl;
import pt.up.fe.specs.util.SpecsLogs;

/**
 * C or C++ initializer list.
 * 
 * @author JoaoBispo
 *
 */
public class InitListExpr extends InitListExprBase {

    public InitListExpr(DataStore data, java.util.Collection<? extends ClavaNode> children, InitListExprBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public InitListExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public List<Expr> getInitExprs() {
        return getChildren(Expr.class);
    }

    public boolean hasArrayFiller() {
        return getWireArrayFiller().isPresent();
    }

    @Override
    public String getCode() {

        InitListExpr syntaticForm = getWireSyntacticForm().orElse(null);
        if (syntaticForm != null) {
            if (!syntaticForm.getId().equals(getId())) {

                // If there are varrefs with custom names, cannot use syntatic form
                var hasCustomNames = getDescendantsStream()
                        .filter(node -> node instanceof DeclRefExpr && node.hasValue(DeclRefExpr.CUSTOM_NAME))
                        .findFirst().isPresent();

                // System.out.println("SYN FORM: " + syntaticForm.getCode());
                if (!hasCustomNames) {
                    return syntaticForm.getCode();
                }

            }
        }

        String list = getChildren().stream()
                .map(expr -> expr.getCode())
                .collect(Collectors.joining(", "));

        if (hasArrayFiller() && !getWireIsStringLiteralInit()) {

            String exprClassName = getWireArrayFiller().get().getClass().getSimpleName();
            switch (exprClassName) {
            case "ImplicitValueInitExpr":
                list = list + ",";
                break;
            default:
                SpecsLogs.warn("Case not defined:" + exprClassName);
                break;
            }
        }

        if (getWireIsExplicit()) {
            return "{" + list + "}";
        } else {
            return list;
        }
    }

}

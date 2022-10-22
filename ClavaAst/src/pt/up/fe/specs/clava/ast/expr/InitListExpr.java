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

package pt.up.fe.specs.clava.ast.expr;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
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
public class InitListExpr extends Expr {

    /// DATAKEY BEGIN

    /**
     * OPTIONAL - If this initializer list initializes an array with more elements than there are initializers in the
     * list, specifies an expression to be used for value initialization of the rest of the elements.
     */
    public final static DataKey<Optional<Expr>> ARRAY_FILLER = KeyFactory.optional("arrayFiller");

    public final static DataKey<FieldDecl> INITIALIZED_FIELD_IN_UNION = KeyFactory.object("initializedFieldInUnion",
            FieldDecl.class);

    public final static DataKey<Boolean> IS_EXPLICIT = KeyFactory.bool("isExplicit");

    public final static DataKey<Boolean> IS_STRING_LITERAL_INIT = KeyFactory.bool("isStringLiteralInit");

    /**
     * Represents the initializer list as written by the user, which may contain C99 designated initializers
     * (represented as DesignatedInitExprs), initializations of sub-object members without explicit braces, and so on.
     * If the syntactic and semantic forms are the same for this initializer, it returns empty.
     */
    public final static DataKey<Optional<InitListExpr>> SYNTACTIC_FORM = KeyFactory.optional("syntacticForm");

    public final static DataKey<Optional<InitListExpr>> SEMANTIC_FORM = KeyFactory.optional("semanticForm");

    /// DATAKEY END

    public InitListExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public List<Expr> getInitExprs() {
        return getChildren(Expr.class);
    }

    public boolean hasArrayFiller() {
        return get(ARRAY_FILLER).isPresent();
    }

    @Override
    public String getCode() {

        InitListExpr syntaticForm = get(SYNTACTIC_FORM).orElse(null);
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

        if (hasArrayFiller() && !get(IS_STRING_LITERAL_INIT)) {

            String exprClassName = get(ARRAY_FILLER).get().getClass().getSimpleName();
            switch (exprClassName) {
            case "ImplicitValueInitExpr":
                list = list + ",";
                break;
            default:
                SpecsLogs.warn("Case not defined:" + exprClassName);
                break;
            }
        }

        if (get(IS_EXPLICIT)) {
            return "{" + list + "}";
        } else {
            return list;
        }
    }

}

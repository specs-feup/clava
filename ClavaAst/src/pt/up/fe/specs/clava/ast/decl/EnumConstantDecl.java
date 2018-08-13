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

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;
import java.util.Optional;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.NullExpr;

/**
 * Represents the declaration of an enum constant.
 * 
 * @author JoaoBispo
 *
 */
public class EnumConstantDecl extends ValueDecl {

    public EnumConstantDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // private final String value;

    // public EnumConstantDecl(String value, Type type, DeclData declData, ClavaNodeInfo info) {
    // this(value, type, declData, info, Collections.emptyList());
    // }
    //
    // public EnumConstantDecl(String value, Type type, DeclData declData, ClavaNodeInfo info, Expr init) {
    // this(value, type, declData, info, Arrays.asList(init));
    // }
    //
    // private EnumConstantDecl(String declName, Type type, DeclData declData, ClavaNodeInfo info,
    // Collection<? extends ClavaNode> children) {
    //
    // super(declName, type, declData, info, children);
    //
    // this.value = declName;
    // }
    //
    // @Override
    // protected ClavaNode copyPrivate() {
    // return new EnumConstantDecl(value, getType(), getDeclData(), getInfo());
    // }

    @Override
    public String getCode() {
        StringBuilder builder = new StringBuilder();

        // builder.append(value);
        builder.append(get(DECL_NAME));

        getInitExpr().ifPresent(subExpr -> builder.append(" = ").append(subExpr.getCode()));

        return builder.toString();
    }

    public Optional<Expr> getInitExpr() {
        Expr initExpr = getChild(Expr.class, 0);

        if (initExpr instanceof NullExpr) {
            return Optional.empty();
        }

        return Optional.ofNullable(initExpr);

        // if (getNumChildren() == 0) {
        // return Optional.empty();
        // }
        //
        // return Optional.of(getChild(Expr.class, 0));
    }

}

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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.NullExpr;
import pt.up.fe.specs.clava.ast.type.Type;

/**
 * Represents a member of a struct/union/class.
 * 
 * @author JoaoBispo
 *
 */
public class FieldDecl extends DeclaratorDecl {

    private final boolean isMutable;
    private final boolean isModulePrivate;

    public FieldDecl(boolean isMutable, boolean isModulePrivate, String declName, Type type, DeclData declData,
            ClavaNodeInfo info, Expr bitwidth, Expr inClassInitializer) {

        this(isMutable, isModulePrivate, declName, type, declData, info,
                Arrays.asList(nullable(bitwidth), nullable(inClassInitializer)));
    }

    private FieldDecl(boolean isMutable, boolean isModulePrivate, String declName, Type type, DeclData declData,
            ClavaNodeInfo info, Collection<? extends ClavaNode> children) {

        super(declName, type, declData, info, children);

        this.isMutable = isMutable;
        this.isModulePrivate = isModulePrivate;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new FieldDecl(isMutable, isModulePrivate, getDeclName(), getType(), getDeclData(), getInfo(),
                Collections.emptyList());
    }

    public Optional<Expr> getBitwidth() {
        Expr bitwidth = getChild(Expr.class, 0);
        return bitwidth instanceof NullExpr ? Optional.empty() : Optional.of(bitwidth);
    }

    public Optional<Expr> getInitialization() {
        Expr init = getChild(Expr.class, 1);
        return init instanceof NullExpr ? Optional.empty() : Optional.of(init);
    }

    /*
    public Optional<Expr> getInitExpr() {
        if (!hasChildren()) {
            return Optional.empty();
        }
    
        return Optional.of(getChild(Expr.class, 0));
    }
    */

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        if (isMutable) {
            code.append("mutable ");
        }

        // code.append(getTypeCode()).append(getDeclName());
        String name = getDeclName();

        code.append(getType().getCode(this, name));

        getBitwidth().ifPresent(expr -> code.append(": ").append(expr.getCode()));

        // getInitExpr().ifPresent(expr -> code.append(" = ").append(expr.getCode()));
        getInitialization().ifPresent(expr -> code.append(" = ").append(expr.getCode()));

        code.append(";");

        return code.toString();
        // TODO: Should add ';'?
    }

}

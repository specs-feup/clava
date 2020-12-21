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

import java.util.Collection;
import java.util.Optional;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.type.Type;

/**
 * Represents a C++ pseudo-destructor (C++ [expr.pseudo]).
 * 
 * @author JoaoBispo
 *
 */
public class CXXPseudoDestructorExpr extends Expr {

    public static final DataKey<Optional<String>> QUALIFIER = KeyFactory.optional("qualifier");

    public static final DataKey<Boolean> IS_ARROW = KeyFactory.bool("isArrow");

    public static final DataKey<Type> DESTROYED_TYPE = KeyFactory.object("destroyedType", Type.class);

    public CXXPseudoDestructorExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Expr getBase() {
        return getChild(Expr.class, 0);
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        code.append(getBase().getCode());
        code.append(get(IS_ARROW) ? "->" : ".");

        var typeCode = get(DESTROYED_TYPE).getCode(this);
        var qualifier = get(QUALIFIER).orElse("");
        code.append(qualifier).append(typeCode).append("::~").append(typeCode);

        return code.toString();
    }
}

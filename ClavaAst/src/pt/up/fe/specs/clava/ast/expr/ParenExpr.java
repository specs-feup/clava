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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.data.ValueKind;
import pt.up.fe.specs.clava.ast.type.Type;

/**
 * Represents a parethesized expression.
 * 
 * @author JoaoBispo
 *
 */
public class ParenExpr extends Expr {

    public ParenExpr(ValueKind valueKind, Type type, ClavaNodeInfo info, Expr subExpr) {
        this(valueKind, type, info, Arrays.asList(subExpr));
    }

    private ParenExpr(ValueKind valueKind, Type type, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {

        super(valueKind, type, info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new ParenExpr(getValueKind(), getType(), getInfo(), Collections.emptyList());
    }

    public Expr getSubExpr() {
        return getChild(Expr.class, 0);
    }

    @Override
    public String getCode() {
        return "(" + getSubExpr().getCode() + ")";
    }

}

/**
 * Copyright 2017 SPeCS.
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

package pt.up.fe.specs.clava.ast.type;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.type.data.ArrayTypeData;
import pt.up.fe.specs.clava.ast.type.data.TypeData;

public class VariableArrayType extends ArrayType {

    // Dummy
    public VariableArrayType(ArrayTypeData arrayTypeData, TypeData typeData, ClavaNodeInfo info, Type elementType,
            Expr sizeExpr) {
        this(arrayTypeData, typeData, info, Arrays.asList(elementType, sizeExpr));
    }

    protected VariableArrayType(ArrayTypeData arrayTypeData, TypeData typeData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {
        super(arrayTypeData, typeData, info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new VariableArrayType(getArrayTypeData(), getTypeData(), getInfo(), Collections.emptyList());
    }

    @Override
    public Type getElementType() {
        return getChild(Type.class, 0);
    }

    public Expr getExpr() {
        return getChild(Expr.class, 1);
    }

    @Override
    protected String getArrayCode() {
        return getExpr().getCode();
    }

}

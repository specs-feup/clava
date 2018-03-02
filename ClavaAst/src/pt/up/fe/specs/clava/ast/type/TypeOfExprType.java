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

package pt.up.fe.specs.clava.ast.type;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.type.data.TypeData;

public class TypeOfExprType extends Type {

    public TypeOfExprType(TypeData data, ClavaNodeInfo info, Expr underlyingExpr, Type underlyingType) {
        super(data, info, Arrays.asList(underlyingExpr, underlyingType));
    }

    private TypeOfExprType(TypeData data, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(data, info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new TypeOfExprType(getTypeData(), getInfo(), Collections.emptyList());
    }

    public Expr getUnderlyingExpr() {
        return getChild(Expr.class, 0);
    }

    @Override
    protected Type desugarImpl() {
        return getChild(Type.class, 1);
    }

    @Override
    public String getCode(String name) {

        // If GNU, do not change type (i.e., typeof)
        if (getApp().getStandard().isGnu()) {
            return super.getCode(name);
        }

        // Not GNU, change to __typeof__

        String typeCode = super.getCode(name);
        Preconditions.checkArgument(typeCode.startsWith("typeof "), "Expected code of type to start with 'typeof '");

        return "__typeof__" + typeCode.substring("typeof".length());
    }

}

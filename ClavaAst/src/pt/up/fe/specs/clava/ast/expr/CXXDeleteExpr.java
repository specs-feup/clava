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

public class CXXDeleteExpr extends Expr {

    private final boolean isArray;
    private final Long functionAddress;
    private final String operator;
    private final Type functionType;

    public CXXDeleteExpr(boolean isArray, Long functionAddress, String operator, Type functionType,
            Type type, ClavaNodeInfo info, Expr argument) {

        this(isArray, functionAddress, operator, functionType, type, info, Arrays.asList(argument));
    }

    private CXXDeleteExpr(boolean isArray, Long functionAddress, String operator, Type functionType,
            Type type, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(ValueKind.getDefault(), type, info, children);

        this.isArray = isArray;
        this.functionAddress = functionAddress;
        this.operator = operator;
        this.functionType = functionType;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new CXXDeleteExpr(isArray, functionAddress, operator, functionType, getType(), getInfo(),
                Collections.emptyList());
    }

    public Expr getArgument() {
        return getChild(Expr.class, 0);
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        code.append("delete");
        if (isArray) {
            code.append("[]");
        }
        code.append(" ");
        code.append(getArgument().getCode());

        return code.toString();
    }
}

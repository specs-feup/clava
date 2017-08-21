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

import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.data.ValueKind;
import pt.up.fe.specs.clava.ast.type.Type;

public class CXXDefaultArgExpr extends Expr {

    public CXXDefaultArgExpr(ValueKind valueKind, Type type, ClavaNodeInfo info) {
        super(valueKind, type, info, Collections.emptyList());
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new CXXDefaultArgExpr(getValueKind(), getType(), getInfo());
    }

    @Override
    public String getCode() {

        throw new RuntimeException("Should not generate code");
        /*        Type type = getExprType();
        
        // Do not add qualifiers (e.g., const)
        if (type instanceof QualTypeOld) {
            return ((QualTypeOld) type).getUnqualifiedType().getCode();
        }
        
        // Do not add type keywords (e.g., enum, struct...)
        if (type instanceof SimpleTypeWithKeyword) {
            return ((SimpleTypeWithKeyword) type).getInnerType().getCode();
        }
        
        return type.getCode();
        
        // System.out.println("TOSTRING:" + toString());
        // System.out.println("TYPE:" + getExprType().toString());
        // throw new UnsupportedNodeOperationException(this);
         
         */
    }

}

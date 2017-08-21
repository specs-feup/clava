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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.data.BareDeclData;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;

/**
 * C or C++ initializer list.
 * 
 * @author JoaoBispo
 *
 */
public class InitListExpr extends Expr {

    private final boolean hasInitializedFieldInUnion;
    private final BareDeclData fieldData;

    public InitListExpr(boolean hasInitializedFieldInUnion, BareDeclData fieldData, ExprData exprData,
            ClavaNodeInfo info, Collection<? extends Expr> initExprs) {

        super(exprData, info, initExprs);

        this.hasInitializedFieldInUnion = hasInitializedFieldInUnion;
        this.fieldData = fieldData;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new InitListExpr(hasInitializedFieldInUnion, fieldData, getExprData(), getInfo(),
                Collections.emptyList());
    }

    public List<Expr> getInitExprs() {
        return getChildren(Expr.class);
    }

    @Override
    public String getCode() {
        String list = getInitExprs().stream()
                .map(expr -> expr.getCode())
                .collect(Collectors.joining(", ", "{ ", " }"));

        return list;
        /*	
        	if (list.length() < 120) {
        	    return list;
        	}
        
        	String[] elements = list.split(", ");
        	*/
    }
}

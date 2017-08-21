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

package pt.up.fe.specs.clava.ast.expr;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;

public class UserDefinedLiteral extends CallExpr {

    private final String OPERATOR_PREFIX = "operator\"\"";

    public UserDefinedLiteral(ExprData exprData, ClavaNodeInfo info, Expr callee, Expr cookedLiteral) {
        this(exprData, info, Arrays.asList(callee, cookedLiteral));

        Preconditions.checkArgument(cookedLiteral instanceof Literal);
    }

    private UserDefinedLiteral(ExprData exprData, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(exprData, info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new UserDefinedLiteral(getExprData(), getInfo(), Collections.emptyList());
    }

    @Override
    public List<Expr> getArgs() {
        return Collections.emptyList();
    }

    public Expr getCookedLiteralExpr() {
        return getChild(Expr.class, 1);
    }

    public String getLiteral() {
        return ((Literal) getCookedLiteralExpr()).getLiteral();
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        code.append(getLiteral());
        String calleeName = getCalleeName();
        Preconditions.checkArgument(calleeName.startsWith(OPERATOR_PREFIX),
                "Expected calleeName to start with '" + OPERATOR_PREFIX + "', instead is '" + calleeName + "'");

        code.append(calleeName.substring(OPERATOR_PREFIX.length()));

        return code.toString();
    }

}

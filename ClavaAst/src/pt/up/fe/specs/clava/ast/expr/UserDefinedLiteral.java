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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

public class UserDefinedLiteral extends CallExpr {

    // private final String OPERATOR_PREFIX = "operator\"\"";

    public UserDefinedLiteral(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
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
        return getLiteral();
    }

}

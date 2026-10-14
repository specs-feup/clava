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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.ast.expr;

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.util.SpecsLogs;

public class CXXDeleteExpr extends CXXDeleteExprBase {

    public CXXDeleteExpr(DataStore data, java.util.Collection<? extends ClavaNode> children, CXXDeleteExprBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public CXXDeleteExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Expr getArgument() {
        return getChild(Expr.class, 0);
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        if (getWireIsGlobal()) {
            SpecsLogs.warn("Code generation not implemented yet when global is true");
        }

        code.append("delete");
        if (getWireIsArrayAsWritten()) {
            code.append("[]");
        }

        Expr arg = getArgument();
        if (!arg.get(Expr.IS_DEFAULT_ARGUMENT)) {
            code.append(" ");
            code.append(getArgument().getCode());
        }

        return code.toString();
    }
}

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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.ast.expr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.data.offsetof.OffsetOfComponent;
import pt.up.fe.specs.clava.ast.type.Type;

public class OffsetOfExpr extends OffsetOfExprBase {

    // Data declarations are generated from the wire schema in OffsetOfExprBase.

    public OffsetOfExpr(DataStore data, Collection<? extends ClavaNode> children, OffsetOfExprBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public OffsetOfExpr(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        String componentsCode = getComponentsCode();
        return "offsetof(" + getWireSourceType().getCode(this) + ", " + componentsCode + ")";
    }

    private String getComponentsCode() {

        StringBuilder code = new StringBuilder();

        boolean hasCode = false;
        for (OffsetOfComponent component : getWireComponents()) {
            String componentCode = component.getCode();

            if (componentCode.isEmpty()) {
                continue;
            }

            if (component.isField() && hasCode) {
                code.append(".");
            }

            code.append(componentCode);
            hasCode = true;
        }
        return code.toString();
    }
}

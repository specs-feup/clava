/**
 * Copyright 2019 SPeCS.
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

package pt.up.fe.specs.clava.ast.stmt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.attr.Attribute;

public class AttributedStmt extends AttributedStmtBase {

    // Data declarations are generated from the wire schema in AttributedStmtBase.

    public AttributedStmt(DataStore data, Collection<? extends ClavaNode> children, AttributedStmtBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public AttributedStmt(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Stmt getSubStmt() {
        return getChild(Stmt.class, 0);
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        for (Attribute attr : getWireStmtAttributes()) {
            code.append(attr.getCode()).append("\n");
        }

        code.append(getSubStmt().getCode());

        return code.toString();
    }
}

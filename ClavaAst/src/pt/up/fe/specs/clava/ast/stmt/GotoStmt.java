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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.ast.stmt;

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.LabelDecl;

public class GotoStmt extends GotoStmtBase {

    // Data declarations are generated from the wire schema in GotoStmtBase.

    public GotoStmt(DataStore data, Collection<? extends ClavaNode> children, GotoStmtBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public GotoStmt(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Stmt getSubStmt() {
        return getChild(Stmt.class, 0);
    }

    public LabelDecl getLabel() {
        return getWireLabel();
    }

    public void setLabel(LabelDecl labelDecl) {
        set(LABEL, labelDecl);
    }

    @Override
    public String getCode() {
        return "goto " + getLabel().getDeclName() + ";" + ln();
        // return "goto: " + getSubStmt().getCode();
    }
}

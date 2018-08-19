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

package pt.up.fe.specs.clava.ast.stmt;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.util.utilities.StringLines;

public class CapturedStmt extends Stmt {

    public CapturedStmt(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // public CapturedStmt(ClavaNodeInfo info, Collection<? extends Stmt> children) {
    // super(info, children);
    // }
    //
    // @Override
    // protected ClavaNode copyPrivate() {
    // return new CapturedStmt(getInfo(), Collections.emptyList());
    // }

    public List<Stmt> getStatements() {
        return getChildren(Stmt.class);
    }

    @Override
    public List<Stmt> toStatements() {
        return getStatements();
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        code.append("{" + ln());

        for (Stmt stmt : getStatements()) {

            String stmtCode = StringLines.getLines(stmt.getCode()).stream()
                    // Add tab
                    .map(line -> getTab() + line)
                    .collect(Collectors.joining(ln(), "", ln()));
            code.append(stmtCode);
        }

        code.append("}" + ln());

        return code.toString();
    }

    @Override
    public boolean isAggregateStmt() {
        return true;
    }

}

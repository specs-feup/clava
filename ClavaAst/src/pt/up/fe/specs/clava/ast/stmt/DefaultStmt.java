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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;

public class DefaultStmt extends SwitchCase {

    public DefaultStmt(ClavaNodeInfo info, Stmt subStmt) {
        this(info, Arrays.asList(subStmt));
    }

    private DefaultStmt(ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new DefaultStmt(getInfo(), Collections.emptyList());
    }

    @Override
    public Stmt getSubStmt() {
        return getChild(Stmt.class, 0);
    }

    @Override
    public String getCode() {
        StringBuilder builder = new StringBuilder();

        builder.append("default:" + ln())
                .append(indentCode(getSubStmt().getCode()));

        return builder.toString();
    }

}

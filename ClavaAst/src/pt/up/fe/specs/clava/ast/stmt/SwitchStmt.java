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
import java.util.Optional;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.Expr;

/**
 * Represents a switch statement.
 * 
 * @author JoaoBispo
 *
 */
public class SwitchStmt extends Stmt {

    public SwitchStmt(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        StringBuilder builder = new StringBuilder();

        builder.append("switch (").append(getCond().getCode()).append(")").append(getBody().getCode());

        return builder.toString();
    }

    public Expr getCond() {
        return getChild(Expr.class, 0);
    }

    public Stmt getBody() {
        return getChild(Stmt.class, 1);
    }

    public boolean hasDefaultCase() {
        return getDefaultCase()
                .isPresent();
    }

    public List<SwitchCase> getCases() {
        return getBody().getChildrenStream()
                .filter(node -> node instanceof SwitchCase)
                .map(SwitchCase.class::cast)
                .collect(Collectors.toList());
    }

    public Optional<DefaultStmt> getDefaultCase() {
        return getBody().getChildrenStream()
                .filter(node -> node instanceof DefaultStmt)
                .map(DefaultStmt.class::cast)
                .findFirst();
    }

}

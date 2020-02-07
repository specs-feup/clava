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
import java.util.Optional;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;

/**
 * Represents a return statement.
 * 
 * @author JoaoBispo
 *
 */
public class ReturnStmt extends Stmt {

    public ReturnStmt(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public String getCode() {
        StringBuilder builder = new StringBuilder();

        // Add a new line before 'return'
        builder.append(ln() + "return");
        getRetValue().ifPresent(retValue -> builder.append(" ").append(retValue.getCode()));
        builder.append(";" + ln());

        return builder.toString();
    }

    public Optional<Expr> getRetValue() {
        if (!hasChildren()) {
            return Optional.empty();
        }

        return Optional.of(getChild(Expr.class, 0));
    }

    public void setRetValue(Expr expression) {
        if (expression == null) {
            if (hasChildren()) {
                removeChild(0);
            }

            return;
        }

        // Replace return value if present, of add child if not
        getRetValue().ifPresentOrElse(retValue -> NodeInsertUtils.replace(retValue, expression),
                () -> addChild(expression));

    }

}

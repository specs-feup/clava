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
import java.util.Optional;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;

/**
 * A reference to an overloaded function set, either an UnresolvedLookupExpr or an UnresolvedMemberExpr.
 * 
 * @author JoaoBispo
 *
 */
public abstract class OverloadExpr extends Expr {

    private final String qualifier;

    public OverloadExpr(String qualifier, ExprData exprData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {
        super(exprData, info, children);

        this.qualifier = qualifier;
    }

    public Optional<String> getQualifier() {
        return Optional.ofNullable(qualifier);
    }

}

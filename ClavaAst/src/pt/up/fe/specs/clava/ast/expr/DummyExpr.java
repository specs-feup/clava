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

package pt.up.fe.specs.clava.ast.expr;

import java.util.Collection;
import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.DummyNode;
import pt.up.fe.specs.clava.ast.expr.data.ExprData;

/**
 * Dummy statement, for testing purposes.
 * 
 * @author JoaoBispo
 *
 */
public class DummyExpr extends Expr implements DummyNode {

    private final String content;

    public DummyExpr(String content, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        // Using RVALUE as default
        super(ExprData.empty(), info, children);

        this.content = content;
    }

    @Override
    public String getNodeName() {
        return super.getNodeName() + " (" + getOriginalType() + ")";
    }

    public String getNodeCode() {
        return "/* Dummy expr '" + content + "'*/";
    }

    @Override
    public String getCode() {
        return ClavaNodes.toCode(getNodeCode(), this);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new DummyExpr(content, getInfo(), Collections.emptyList());
    }

    @Override
    public String getContent() {
        return content;
    }

}

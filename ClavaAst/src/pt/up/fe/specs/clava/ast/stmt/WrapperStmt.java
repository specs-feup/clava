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

package pt.up.fe.specs.clava.ast.stmt;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;

/**
 * Wraps other nodes inside a statement node.
 * 
 * @author JoaoBispo
 *
 */
public class WrapperStmt extends Stmt {

    public WrapperStmt(ClavaNodeInfo info, ClavaNode wrappedNode) {
        this(info, Arrays.asList(wrappedNode));
    }

    private WrapperStmt(ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new WrapperStmt(getInfo(), Collections.emptyList());
    }

    public ClavaNode getWrappedNode() {
        return getChild(0);
    }

    @Override
    public String getCode() {
        return getWrappedNode().getCode() + ln();
    }

    @Override
    public boolean isWrapper() {
        return true;
    }
}

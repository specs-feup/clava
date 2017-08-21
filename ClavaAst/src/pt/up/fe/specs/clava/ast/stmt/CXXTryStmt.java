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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.util.SpecsCollections;

public class CXXTryStmt extends Stmt {

    public CXXTryStmt(ClavaNodeInfo info, CompoundStmt tryBody, List<CXXCatchStmt> handlers) {
        this(info, SpecsCollections.concat(tryBody, handlers));
    }

    private CXXTryStmt(ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new CXXTryStmt(getInfo(), Collections.emptyList());
    }

    public CompoundStmt getBody() {
        return getChild(CompoundStmt.class, 0);
    }

    public List<CXXCatchStmt> getHandlers() {
        return getChildrenOf(CXXCatchStmt.class);
        // return SpecsCollections.cast(SpecsCollections.subList(getChildren(), 1), CXXCatchStmt.class);
    }

    @Override
    public String getCode() {
        return getCode("");
    }

    public String getCode(String initList) {
        StringBuilder code = new StringBuilder();

        code.append("try").append(initList).append(getBody().getCode());
        // It can have comments (WrapperStmt) has children, use children instead of getHandlers when generating code
        // for (CXXCatchStmt handler : getHandlers()) {
        for (ClavaNode child : SpecsCollections.subList(getChildren(), 1)) {
            code.append(child.getCode());
        }

        return code.toString();
    }

}

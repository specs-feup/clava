/**
 * Copyright 2022 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.LabelDecl;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ALabelDecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ALabelStmt;

public class CxxLabelDecl extends ALabelDecl {

    private final LabelDecl labelDecl;

    public CxxLabelDecl(LabelDecl labelDecl) {
        super(new CxxNamedDecl(labelDecl));
        this.labelDecl = labelDecl;
    }

    @Override
    public ClavaNode getNode() {
        return labelDecl;
    }

    @Override
    public ALabelStmt getLabelStmtImpl() {
        return labelDecl.get(LabelDecl.LABEL_STMT)
                .map(labelStmt -> CxxJoinpoints.create(labelStmt, ALabelStmt.class))
                .orElse(null);
    }
}

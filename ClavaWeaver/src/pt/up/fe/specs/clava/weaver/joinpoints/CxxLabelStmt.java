/**
 * Copyright 2022 SPeCS.
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

package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.LabelDecl;
import pt.up.fe.specs.clava.ast.stmt.LabelStmt;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ALabelDecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ALabelStmt;

public class CxxLabelStmt extends ALabelStmt {

    private final LabelStmt labelStmt;

    public CxxLabelStmt(LabelStmt labelStmt) {
        super(new CxxStatement(labelStmt));
        this.labelStmt = labelStmt;
    }

    @Override
    public ALabelDecl getDeclImpl() {
        return CxxJoinpoints.create(labelStmt.getLabelDecl(), ALabelDecl.class);
    }

    @Override
    public void defDeclImpl(ALabelDecl value) {
        labelStmt.setLabelDecl((LabelDecl) value.getNode());
    }

    @Override
    public void setDeclImpl(ALabelDecl label) {
        defDeclImpl(label);
    }

    @Override
    public ClavaNode getNode() {
        return labelStmt;
    }

}

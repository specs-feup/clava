/**
 * Copyright 2021 SPeCS.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ast.decl.LabelDecl;
import pt.up.fe.specs.clava.ast.stmt.GotoStmt;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AGotoStmt;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ALabelDecl;

public class CxxGotoStmt<Self extends CxxGotoStmt<Self>> extends AGotoStmt<Self> {

    public CxxGotoStmt(GotoStmt gotoStmt, CxxWeaver weaver) {
        super(gotoStmt, weaver);
    }

    @Override
    public GotoStmt getNodeImpl() {
        return (GotoStmt) super.getNodeImpl();
    }

    @Override
    public void setLabelImpl(ALabelDecl<?> label) {
        this.getNodeImpl().setLabel((LabelDecl) label.getNodeImpl());
    }

    @Override
    public ALabelDecl<?> getLabelImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().getLabel(), getWeaverEngine(), ALabelDecl.class);
    }

}

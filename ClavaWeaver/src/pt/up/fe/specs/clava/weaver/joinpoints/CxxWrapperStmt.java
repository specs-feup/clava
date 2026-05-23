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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.comment.Comment;
import pt.up.fe.specs.clava.ast.pragma.Pragma;
import pt.up.fe.specs.clava.ast.stmt.WrapperStmt;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinpoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AWrapperStmt;
import pt.up.fe.specs.clava.weaver.enums.WrapperStatementKind;

public class CxxWrapperStmt<Self extends CxxWrapperStmt<Self>> extends AWrapperStmt<Self> {

    public CxxWrapperStmt(WrapperStmt wrapperStmt, CxxWeaver weaver) {
        super(wrapperStmt, weaver);
    }

    @Override
    public WrapperStmt getNodeImpl() {
        return (WrapperStmt) super.getNodeImpl();
    }

    @Override
    public WrapperStatementKind getKindImpl() {

        ClavaNode wrappedNode = this.getNodeImpl().getWrappedNode();

        if (wrappedNode instanceof Comment) {
            return WrapperStatementKind.COMMENT;
        }

        if (wrappedNode instanceof Pragma) {
            return WrapperStatementKind.PRAGMA;
        }

        throw new RuntimeException("Case not defined for wrapperStmt.kind: " + wrappedNode.getClass().getSimpleName());
    }

    @Override
    public AJoinpoint<?> getContentImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().getWrappedNode(), getWeaverEngine());
    }

}

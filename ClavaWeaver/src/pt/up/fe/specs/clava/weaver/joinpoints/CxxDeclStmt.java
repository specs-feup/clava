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

import pt.up.fe.specs.clava.ast.stmt.DeclStmt;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ADecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ADeclStmt;

public class CxxDeclStmt<Self extends CxxDeclStmt<Self>> extends ADeclStmt<Self> {

    public CxxDeclStmt(DeclStmt declStmt, CxxWeaver weaver) {
        super(declStmt, weaver);
    }

    @Override
    public DeclStmt getNodeImpl() {
        return (DeclStmt) super.getNodeImpl();
    }

    @Override
    public ADecl<?>[] getDeclsImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().getDecls(), getWeaverEngine(), ADecl.class);
    }

}

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

package pt.up.fe.specs.clava.weaver.joinpoints;

import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AExpression;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AVardecl;
import pt.up.fe.specs.util.SpecsCollections;

public class CxxVardecl extends AVardecl {

    private final VarDecl varDecl;
    private final ACxxWeaverJoinPoint parent;

    public CxxVardecl(VarDecl varDecl, ACxxWeaverJoinPoint parent) {
        super(new CxxNamedDecl(varDecl, parent));

        this.varDecl = varDecl;
        this.parent = parent;
    }

    @Override
    public ACxxWeaverJoinPoint getParentImpl() {
        return parent;
    }

    @Override
    public ClavaNode getNode() {
        return varDecl;
    }

    @Override
    public List<? extends AExpression> selectInit() {
        return SpecsCollections.toList(
                varDecl.getInit()
                        .map(init -> (AExpression) CxxJoinpoints.create(init, this)));

    }

    @Override
    public Boolean getHasInitImpl() {
        return varDecl.getInit().isPresent();
    }

}

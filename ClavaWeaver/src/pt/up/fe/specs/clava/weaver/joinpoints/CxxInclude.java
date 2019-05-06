/**
 * Copyright 2018 SPeCS.
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
import pt.up.fe.specs.clava.ast.decl.IncludeDecl;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AInclude;

public class CxxInclude extends AInclude {

    private final IncludeDecl include;
    private final ACxxWeaverJoinPoint parent;

    public CxxInclude(IncludeDecl include, ACxxWeaverJoinPoint parent) {
        super(new CxxDecl(include, parent));
        this.include = include;
        this.parent = parent;
    }

    // @Override
    // public ACxxWeaverJoinPoint getParentImpl() {
    // return parent;
    // }

    @Override
    public ClavaNode getNode() {
        return include;
    }

    @Override
    public String getNameImpl() {
        return include.getInclude().getInclude();
    }

    @Override
    public Boolean getIsAngledImpl() {
        return include.getInclude().isAngled();
    }

    @Override
    public String getFilepathImpl() {
        return include.getInclude().getSourceFile().getAbsolutePath();
    }

    @Override
    public String getRelativeFolderpathImpl() {
        return include.getInclude().getRelativeFolder().getAbsolutePath();
    }

}

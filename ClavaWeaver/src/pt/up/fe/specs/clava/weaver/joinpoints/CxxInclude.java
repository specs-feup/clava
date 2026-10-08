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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clava.ast.decl.IncludeDecl;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AInclude;

public class CxxInclude<Self extends CxxInclude<Self>> extends AInclude<Self> {

    public CxxInclude(IncludeDecl include, CxxWeaver weaver) {
        super(include, weaver);
    }

    @Override
    public IncludeDecl getNodeImpl() {
        return (IncludeDecl) super.getNodeImpl();
    }

    @Override
    public String getNameImpl() {
        return this.getNodeImpl().getInclude().getInclude();
    }

    @Override
    public boolean getIsAngledImpl() {
        return this.getNodeImpl().getInclude().isAngled();
    }

    @Override
    public String getFilepathImpl() {
        return this.getNodeImpl().getInclude().getSourceFile().getAbsolutePath();
    }

    @Override
    public String getRelativeFolderpathImpl() {
        return this.getNodeImpl().getInclude().getRelativeFolder().getAbsolutePath();
    }

}

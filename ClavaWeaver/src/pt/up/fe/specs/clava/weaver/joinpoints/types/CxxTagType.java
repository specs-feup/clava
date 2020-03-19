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

package pt.up.fe.specs.clava.weaver.joinpoints.types;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.TagDecl;
import pt.up.fe.specs.clava.ast.type.TagType;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ADecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ATagType;

public class CxxTagType extends ATagType {
    private final TagType tagType;

    public CxxTagType(TagType tagType) {
        super(new CxxType(tagType));

        this.tagType = tagType;
    }

    @Override
    public ClavaNode getNode() {
        return tagType;
    }

    @Override
    public String getNameImpl() {
        return tagType.get(TagType.DECL).get(TagDecl.DECL_NAME);
    }

    @Override
    public ADecl getDeclImpl() {
        return CxxJoinpoints.create(tagType.getDecl(), ADecl.class);
    }

}

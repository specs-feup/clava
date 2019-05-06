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

package pt.up.fe.specs.clava.weaver.joinpoints;

import pt.up.fe.specs.clang.clava.lara.LaraTagPragma;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.weaver.Insert;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ATag;

public class CxxTag extends ATag {

    private final LaraTagPragma tag;
    private final ACxxWeaverJoinPoint parent;

    public CxxTag(LaraTagPragma reference, ACxxWeaverJoinPoint parent) {
        super(new CxxPragma(reference, parent));
        tag = reference;
        this.parent = parent;
    }

    // @Override
    // public ACxxWeaverJoinPoint getParentImpl() {
    // return parent;
    // }

    @Override
    public ClavaNode getNode() {
        return tag;
    }

    @Override
    public String getIdImpl() {
        return tag.getTagId();
    }

    @Override
    public void insertImpl(String position, String code) {

        Insert insert = Insert.getHelper().fromValue(position);
        if (insert == Insert.AFTER) {

            getTargetImpl().insert(position, code);
        } else {
            super.insertImpl(position, code);
        }
    }
}

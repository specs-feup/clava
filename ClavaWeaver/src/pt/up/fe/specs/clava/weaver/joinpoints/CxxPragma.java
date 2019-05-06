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

import java.util.Arrays;
import java.util.List;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.pragma.Pragma;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.APragma;

public class CxxPragma extends APragma {

    private Pragma pragma;
    private final ACxxWeaverJoinPoint parent;

    public CxxPragma(Pragma pragma, ACxxWeaverJoinPoint parent) {
        this.pragma = pragma;
        this.parent = parent;
    }

    // @Override
    // public ACxxWeaverJoinPoint getParentImpl() {
    // return parent;
    // }

    @Override
    public ClavaNode getNode() {
        return pragma;
    }

    @Override
    public String getNameImpl() {
        return pragma.getName();
    }

    @Override
    public AJoinPoint getTargetImpl() {
        return pragma.getTarget().map(target -> CxxJoinpoints.create(target, this)).orElse(null);
    }

    @Override
    public String getContentImpl() {
        return pragma.getContent();
    }

    @Override
    public void setContentImpl(String content) {
        pragma.setContent(content);
    }

    @Override
    public void setNameImpl(String name) {
        pragma.setName(name);
    }

    public void setPragma(Pragma pragma) {
        this.pragma = pragma;
    }

    @Override
    public List<? extends AJoinPoint> selectTarget() {
        return Arrays.asList(getTargetImpl());
    }

}

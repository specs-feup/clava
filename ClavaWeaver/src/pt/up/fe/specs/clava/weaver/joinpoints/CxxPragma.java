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
import pt.up.fe.specs.clava.ast.pragma.Pragma;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxSelects;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinpoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.APragma;

public class CxxPragma<Self extends CxxPragma<Self>> extends APragma<Self> {

    public CxxPragma(Pragma pragma, CxxWeaver weaver) {
        super(pragma, weaver);
    }

    @Override
    public Pragma getNodeImpl() {
        return (Pragma) super.getNodeImpl();
    }

    @Override
    public String getNameImpl() {
        return this.getNodeImpl().getName();
    }

    @Override
    public AJoinpoint<?> getTargetImpl() {
        return this.getNodeImpl().getTarget().map(target -> CxxJoinpoints.create(target,
                getWeaverEngine(), AJoinpoint.class)).orElse(null);
    }

    @Override
    public String getContentImpl() {
        return this.getNodeImpl().getContent();
    }

    @Override
    public void setContentImpl(String content) {
        this.getNodeImpl().setContent(content);
    }

    @Override
    public void setNameImpl(String name) {
        this.getNodeImpl().setName(name);
    }

    public void setPragma(Pragma pragma) {
        this.node = pragma;
    }

    @Override
    public AJoinpoint<?>[] getGetTargetNodesImpl(String endPragma) {
        var pragmaNodes = this.getNodeImpl().getPragmaNodes(endPragma);
        return CxxSelects.selectedNodesToJps(pragmaNodes.stream(), getWeaverEngine());
    }

}

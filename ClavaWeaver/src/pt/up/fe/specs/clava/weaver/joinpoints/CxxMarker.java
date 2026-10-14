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

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ast.lara.LaraMarkerPragma;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.weaver.CxxSelects;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AMarker;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AScope;
import pt.up.fe.specs.util.SpecsCollections;

public class CxxMarker<Self extends CxxMarker<Self>> extends AMarker<Self> {

    public CxxMarker(LaraMarkerPragma marker, CxxWeaver weaver) {
        super(marker, weaver);
    }

    @Override
    public LaraMarkerPragma getNodeImpl() {
        return (LaraMarkerPragma) super.getNodeImpl();
    }

    @Override
    public String getIdImpl() {
        return this.getNodeImpl().getMarkerId();
    }

    @Override
    public AScope<?> getContentsImpl() {
        AScope<?>[] result = CxxSelects.select(getWeaverEngine(), AScope.class, SpecsCollections.toList(this.getNodeImpl().getTarget()),
                false, node -> node instanceof CompoundStmt && ((CompoundStmt) node).isNestedScope());

        Preconditions.checkArgument(result.length > 0,
                "Could not find the 'scope' associated with the marker '" + this.getNodeImpl().getCode() + "'. Pragma target is: "
                        + this.getNodeImpl().getTarget());
        Preconditions.checkArgument(result.length == 1, "Expected just one scope, but found more than one");

        return result[0];
    }
}

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

import java.util.List;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.lara.LaraMarkerPragma;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.weaver.CxxSelects;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AMarker;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AScope;
import pt.up.fe.specs.util.SpecsCollections;

public class CxxMarker extends AMarker {

    private final LaraMarkerPragma marker;

    public CxxMarker(LaraMarkerPragma marker) {
        super(new CxxPragma(marker));
        this.marker = marker;
    }

    @Override
    public ClavaNode getNode() {
        return marker;
    }

    @Override
    public String getIdImpl() {
        return marker.getMarkerId();
    }

    @Override
    public AScope getContentsImpl() {
        List<? extends AScope> result = CxxSelects.select(AScope.class, SpecsCollections.toList(marker.getTarget()),
                false, node -> node instanceof CompoundStmt && ((CompoundStmt) node).isNestedScope());

        Preconditions.checkArgument(!result.isEmpty(),
                "Could not find the 'scope' associated with the marker '" + marker.getCode() + "'. Pragma target is: "
                        + marker.getTarget());
        Preconditions.checkArgument(result.size() == 1, "Expected just one scope, but found more than one");

        return result.get(0);
    }
}

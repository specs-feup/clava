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

package pt.up.fe.specs.clava.weaver.joinpoints.types;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.type.AdjustedType;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AAdjustedType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;

public class CxxAdjustedType extends AAdjustedType {

    private final AdjustedType adjustedType;

    public CxxAdjustedType(AdjustedType adjustedType) {
        super(new CxxType(adjustedType));

        this.adjustedType = adjustedType;
    }

    @Override
    public ClavaNode getNode() {
        return adjustedType;
    }

    @Override
    public AType getOriginalTypeImpl() {
        return CxxJoinpoints.create(adjustedType.get(AdjustedType.ORIGINAL_TYPE), AType.class);
    }

    @Override
    public Integer[] getArrayDimsArrayImpl() {
        return getOriginalTypeImpl().getArrayDimsArrayImpl();
    }

    @Override
    public Integer getArraySizeImpl() {
        return getOriginalTypeImpl().getArraySizeImpl();
    }

}

/**
 * Copyright 2020 SPeCS.
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

package pt.up.fe.specs.clava.weaver.joinpoints.types;

import pt.up.fe.specs.clava.ast.type.ElaboratedType;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AElaboratedType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;
import pt.up.fe.specs.util.SpecsStrings;

public class CxxElaboratedType<Self extends CxxElaboratedType<Self>> extends AElaboratedType<Self> {

    public CxxElaboratedType(ElaboratedType elaboratedType, CxxWeaver weaver) {
        super(elaboratedType, weaver);
    }

    @Override
    public ElaboratedType getNodeImpl() {
        return (ElaboratedType) super.getNodeImpl();
    }

    @Override
    public String getQualifierImpl() {
        return SpecsStrings.nullIfEmpty(this.getNodeImpl().getQualifier());
    }

    @Override
    public String getKeywordImpl() {
        return SpecsStrings.nullIfEmpty(this.getNodeImpl().getKeyword().getCode());
    }

    @Override
    public AType<?> getNamedTypeImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().get(ElaboratedType.NAMED_TYPE), getWeaverEngine(), AType.class);
    }

}

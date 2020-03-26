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
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints.types;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.type.ElaboratedType;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AElaboratedType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;
import pt.up.fe.specs.util.SpecsStrings;

public class CxxElaboratedType extends AElaboratedType {

    private final ElaboratedType elaboratedType;

    public CxxElaboratedType(ElaboratedType elaboratedType) {
        super(new CxxType(elaboratedType));

        this.elaboratedType = elaboratedType;
    }

    @Override
    public ClavaNode getNode() {
        return elaboratedType;
    }

    @Override
    public String getQualifierImpl() {
        return SpecsStrings.nullIfEmpty(elaboratedType.getQualifier());
        // String qualifier = elaboratedType.get(ElaboratedType.QUALIFIER);
        //
        // return qualifier.isEmpty() ? null : qualifier;
    }

    @Override
    public String getKeywordImpl() {
        return SpecsStrings.nullIfEmpty(elaboratedType.getKeyword().getCode());
    }

    @Override
    public AType getNamedTypeImpl() {
        return CxxJoinpoints.create(elaboratedType.get(ElaboratedType.NAMED_TYPE), AType.class);
    }

}

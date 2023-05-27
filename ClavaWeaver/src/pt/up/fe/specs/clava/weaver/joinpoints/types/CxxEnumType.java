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
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.type.EnumType;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AEnumType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;
import pt.up.fe.specs.util.SpecsLogs;

public class CxxEnumType extends AEnumType {
    private final EnumType enumType;

    public CxxEnumType(EnumType enumType) {
        super(new CxxTagType(enumType));

        this.enumType = enumType;
    }

    @Override
    public ClavaNode getNode() {
        return enumType;
    }

    @Override
    public AType getIntegerTypeImpl() {
        if (getRoot() == null) {
            SpecsLogs.msgInfo("Root not defined, is this a detached join point? -> " + this);
            return null;
        }

        return CxxJoinpoints.create(enumType.getEnumDecl((App) getRootImpl().getNode()).getIntegerType(), AType.class);
    }

}

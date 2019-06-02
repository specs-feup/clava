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
import pt.up.fe.specs.clava.ast.type.BuiltinType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ABuiltinType;

public class CxxBuiltinType extends ABuiltinType {

    private final BuiltinType builtinType;

    public CxxBuiltinType(BuiltinType builtinType) {
        super(new CxxType(builtinType));

        this.builtinType = builtinType;
    }

    @Override
    public ClavaNode getNode() {
        return builtinType;
    }

    @Override
    public String getBuiltinKindImpl() {
        return builtinType.get(BuiltinType.KIND).name();
    }

    @Override
    public Boolean getIsIntegerImpl() {
        return builtinType.get(BuiltinType.KIND).isInteger();
    }

    @Override
    public Boolean getIsFloatImpl() {
        return builtinType.get(BuiltinType.KIND).isFloatingPoint();
    }

    @Override
    public Boolean getIsSignedImpl() {
        return builtinType.get(BuiltinType.KIND).isSignedInteger();
    }

    @Override
    public Boolean getIsUnsignedImpl() {
        return builtinType.get(BuiltinType.KIND).isUnsignedInteger();
    }

}

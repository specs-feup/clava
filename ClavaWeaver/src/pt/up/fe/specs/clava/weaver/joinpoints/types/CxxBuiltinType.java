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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints.types;

import pt.up.fe.specs.clava.ast.type.BuiltinType;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ABuiltinType;

public class CxxBuiltinType<Self extends CxxBuiltinType<Self>> extends ABuiltinType<Self> {

    public CxxBuiltinType(BuiltinType builtinType, CxxWeaver weaver) {
        super(builtinType, weaver);
    }

    @Override
    public BuiltinType getNodeImpl() {
        return (BuiltinType) super.getNodeImpl();
    }

    @Override
    public String getBuiltinKindImpl() {
        return this.getNodeImpl().get(BuiltinType.KIND).name();
    }

    @Override
    public boolean getIsIntegerImpl() {
        return this.getNodeImpl().get(BuiltinType.KIND).isInteger();
    }

    @Override
    public boolean getIsFloatImpl() {
        return this.getNodeImpl().get(BuiltinType.KIND).isFloatingPoint();
    }

    @Override
    public boolean getIsSignedImpl() {
        return this.getNodeImpl().get(BuiltinType.KIND).isSignedInteger();
    }

    @Override
    public boolean getIsUnsignedImpl() {
        return this.getNodeImpl().get(BuiltinType.KIND).isUnsignedInteger();
    }

    @Override
    public boolean getIsVoidImpl() {
        return this.getNodeImpl().get(BuiltinType.KIND).isVoid();
    }

}

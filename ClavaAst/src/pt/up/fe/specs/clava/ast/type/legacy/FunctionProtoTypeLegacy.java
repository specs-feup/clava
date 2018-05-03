/**
 * Copyright 2016 SPeCS.
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

package pt.up.fe.specs.clava.ast.type.legacy;

import java.util.Collection;
import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.type.FunctionProtoType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.data.FunctionProtoTypeData;
import pt.up.fe.specs.clava.ast.type.data.FunctionTypeData;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.util.SpecsCollections;

public class FunctionProtoTypeLegacy extends FunctionProtoType {

    private final FunctionProtoTypeData functionProtoTypeData;

    public FunctionProtoTypeLegacy(FunctionProtoTypeData functionProtoTypeData, FunctionTypeData functionTypeData,
            TypeData typeData, ClavaNodeInfo info, Type returnType, Collection<? extends Type> arguments) {
        this(functionProtoTypeData, functionTypeData, typeData, info, SpecsCollections.concat(returnType, arguments));
    }

    private FunctionProtoTypeLegacy(FunctionProtoTypeData functionProtoTypeData, FunctionTypeData functionTypeData,
            TypeData typeData, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(functionTypeData, typeData, info, children);

        this.functionProtoTypeData = functionProtoTypeData;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new FunctionProtoTypeLegacy(functionProtoTypeData, getFunctionTypeData(), getTypeData(), getInfo(),
                Collections.emptyList());
    }

    @Override
    public FunctionProtoTypeData getFunctionProtoTypeData() {
        return functionProtoTypeData;
    }

}

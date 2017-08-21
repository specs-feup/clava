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

package pt.up.fe.specs.clava.ast.type;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.type.data.FunctionTypeData;
import pt.up.fe.specs.clava.ast.type.data.TypeData;

/**
 * K&R-style function with no information about its arguments (e.g., int foo())
 * 
 * @author JoaoBispo
 *
 */
public class FunctionNoProtoType extends FunctionType {

    public FunctionNoProtoType(FunctionTypeData functionTypeData, TypeData type, ClavaNodeInfo info,
            Type returnType) {

        this(functionTypeData, type, info, Arrays.asList(returnType));
    }

    private FunctionNoProtoType(FunctionTypeData functionTypeData, TypeData type, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {
        super(functionTypeData, type, info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new FunctionNoProtoType(getFunctionTypeData(), getTypeData(), getInfo(), Collections.emptyList());
    }

}

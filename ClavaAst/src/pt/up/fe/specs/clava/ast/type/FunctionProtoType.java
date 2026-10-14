/**
 * 
 * Copyright 2016 SPeCS.
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

package pt.up.fe.specs.clava.ast.type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.type.data.ExceptionSpecification;
import pt.up.fe.specs.clava.ast.type.enums.ExceptionSpecificationType;
import pt.up.fe.specs.clava.language.ReferenceQualifier;
import pt.up.fe.specs.util.SpecsLogs;

public class FunctionProtoType extends FunctionProtoTypeBase {

    // Data declarations are generated from the wire schema in FunctionProtoTypeBase.

    public FunctionProtoType(DataStore data, Collection<? extends ClavaNode> children, FunctionProtoTypeBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public FunctionProtoType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // @Override
    // public int getIndexParamStart() {
    // return getIndexReturnType() + 1;
    // }

    // @Override
    // public int getIndexParamEnd() {
    // return getIndexParamStart() + getWireNumParameters();
    // }

    public String getCodeAfterParams() {
        List<String> codeElements = new ArrayList<>();

        // Add const/volatile
        if (getWireIsConst()) {
            codeElements.add("const");
        }
        if (getWireIsVolatile()) {
            codeElements.add("volatile");
        }

        String referenceCode = getWireReferenceQualifier().getCode();
        if (!referenceCode.isEmpty()) {
            codeElements.add(referenceCode);
        }

        String exceptCode = getWireExceptionSpecification().getCode(this);

        if (!exceptCode.isEmpty()) {
            codeElements.add(exceptCode);
        }

        return codeElements.stream().collect(Collectors.joining(" "));
    }

    @Override
    public int getNumParams() {
        return getWireNumParameters();
    }

    @Override
    public boolean isVariadic() {
        return getWireIsVariadic();
    }

    @Override
    public List<Type> getParamTypes() {
        return getWireParametersTypes();
    }

    @Override
    protected List<DataKey<Type>> getUnderlyingTypeKeys() {
        return Collections.emptyList();
    }

    @Override
    public void setParamType(int paramIndex, Type paramType) {
        if (paramIndex >= getNumParams()) {
            SpecsLogs.msgInfo("Cannot set param '', function has only '" + getNumParams() + "' params");
            return;
        }

        getWireParametersTypes().set(paramIndex, paramType);
        // setChild(getIndexParamStart() + paramIndex, paramType);
    }

    @Override
    public void setParamTypes(List<Type> paramTypes) {
        set(PARAMETERS_TYPES, paramTypes);
    }
}

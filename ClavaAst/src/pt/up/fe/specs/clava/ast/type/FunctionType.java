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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.type.enums.CallingConvention;
import pt.up.fe.specs.util.SpecsLogs;

/**
 * Represents the type of a Function.
 * 
 * @author JoaoBispo
 *
 */
public abstract class FunctionType extends Type {

    /// DATAKEYS BEGIN

    public final static DataKey<Boolean> NO_RETURN = KeyFactory.bool("noReturn");

    public final static DataKey<Boolean> PRODUCES_RESULT = KeyFactory.bool("producesResult");

    public final static DataKey<Boolean> HAS_REG_PARM = KeyFactory.bool("hasRegParm");

    public final static DataKey<Long> REG_PARM = KeyFactory.longInt("regParm");

    public final static DataKey<CallingConvention> CALLING_CONVENTION = KeyFactory.enumeration("callingConvention",
            CallingConvention.class);

    /// DATAKEYS END

    /**
     * FunctionType fields
     */

    public FunctionType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Type getReturnType() {
        int indexReturnType = getIndexReturnType();
        if (indexReturnType < 0) {
            return null;
        }

        return getChild(Type.class, indexReturnType);
    }

    /**
     * Return type comes after desugared type, if present.
     * 
     * @return
     */
    public int getIndexReturnType() {
        return getIndexDesugar() + 1;
    }

    /**
     * Inclusive index.
     * 
     * @return
     */
    public int getIndexParamStart() {

        return getIndexReturnType() + 1;
    }

    /**
     * Exclusive index.
     * 
     * @return
     */
    public int getIndexParamEnd() {
        return getIndexParamStart() + getNumParams();
    }

    public List<Type> getParamTypes() {

        return getChildren().subList(getIndexParamStart(), getIndexParamEnd()).stream()
                .map(child -> (Type) child)
                .collect(Collectors.toList());

        /*
        if (getNumChildren() == 1) {
            Collections.emptyList();
        }
        
        return getChildren(Type.class, 1);
        */
    }

    /**
     * 
     * @return the number of parameters of this function
     */
    abstract public int getNumParams();
    // public int getNumParams() {
    // // First child is the return type, remaining children are the param types
    // return getNumChildren() - 1;
    // }

    public void setReturnType(Type returnType) {
        setChild(getIndexReturnType(), returnType);
    }

    public void setParamType(int paramIndex, Type paramType) {
        if (paramIndex >= getNumParams()) {
            SpecsLogs.msgInfo("Cannot set param '', function has only '" + getNumParams() + "' params");
            return;
        }

        // setChild(paramIndex + 1, paramType);
        setChild(getIndexParamStart() + paramIndex, paramType);
    }

    @Override
    public String getCode(String name) {
        // A name (*) means that it has been called from PointerType with null name
        // if (name != null && !name.isEmpty() && !name.equals("(*)")) {
        // SpecsLogs.msgWarn(
        // ".getCode() for FunctionType using name, check if ok since we do not have information about parameters
        // names");
        // }

        StringBuilder code = new StringBuilder();

        code.append(getReturnType().getCode(name));

        String paramsCode = getParamTypes().stream()
                .map(type -> type.getCode())
                .collect(Collectors.joining(", ", " (", ")"));

        code.append(paramsCode);

        return code.toString();
    }

}

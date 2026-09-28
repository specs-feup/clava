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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.ast.type;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.type.enums.BuiltinKind;
import pt.up.fe.specs.clava.ast.type.enums.CallingConvention;
import pt.up.fe.specs.clava.utils.NullNode;
import pt.up.fe.specs.util.SpecsCheck;

/**
 * Represents the type of a Function.
 * 
 * @author JoaoBispo
 *
 */
public abstract class FunctionType extends Type {

    /// DATAKEYS BEGIN

    public final static DataKey<Boolean> IS_CONST = KeyFactory.bool("isConst");

    public final static DataKey<Boolean> IS_VOLATILE = KeyFactory.bool("isVolatile");

    public final static DataKey<Boolean> IS_RESTRICT = KeyFactory.bool("isRestrict");

    /**
     * @deprecated No need to be stored in a field, can be calculated. Use FunctionType.hasNoReturn() instead
     */
    @Deprecated
    public final static DataKey<Boolean> NO_RETURN = KeyFactory.bool("noReturn");

    public final static DataKey<Boolean> PRODUCES_RESULT = KeyFactory.bool("producesResult");

    public final static DataKey<Boolean> HAS_REG_PARM = KeyFactory.bool("hasRegParm");

    public final static DataKey<Long> REG_PARM = KeyFactory.longInt("regParm");

    public final static DataKey<CallingConvention> CALLING_CONVENTION = KeyFactory.enumeration("callingConvention",
            CallingConvention.class);

    public final static DataKey<Type> RETURN_TYPE = KeyFactory.object("returnType", Type.class);

    /// DATAKEYS END

    /**
     * FunctionType fields
     */

    public FunctionType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public Type getReturnType() {

        Type returnType = get(RETURN_TYPE);

        SpecsCheck.checkArgument(!(returnType instanceof NullNode),
                () -> "Just to confirm if this can be null. It if can, change to Optional");

        return returnType;
    }

    @Override
    public boolean isConst() {
        return get(IS_CONST);
    }

    @Override
    public void removeConst() {
        set(IS_CONST, false);
    }

    @Override
    public Type asConst() {
        var typeCopy = copy();

        typeCopy.set(IS_CONST, true);

        return typeCopy;
    }

    public abstract List<Type> getParamTypes();

    /**
     * 
     * @return the number of parameters of this function
     */
    abstract public int getNumParams();

    abstract public boolean isVariadic();

    public void setReturnType(Type returnType) {
        set(RETURN_TYPE, returnType);
    }

    public abstract void setParamType(int paramIndex, Type paramType);

    public abstract void setParamTypes(List<Type> paramType);

    @Override
    public String getCode(ClavaNode sourceNode, String name) {
        StringBuilder code = new StringBuilder();

        code.append(getReturnType().getCode(sourceNode, name));

        String paramsCode = getParamTypes().stream()
                .map(type -> type.getCode(sourceNode))
                .collect(Collectors.joining(", "));

        if (isVariadic()) {
            paramsCode = paramsCode + ", ...";
        }

        paramsCode = " (" + paramsCode + ")";

        code.append(paramsCode);

        return code.toString();
    }

    /**
     * 
     * @return true if return type is a void type.
     */
    public boolean hasNoReturn() {

        Type returnType = getReturnType();

        if (!(returnType instanceof BuiltinType)) {
            return false;
        }

        return returnType.get(BuiltinType.KIND) == BuiltinKind.Void;
    }

}

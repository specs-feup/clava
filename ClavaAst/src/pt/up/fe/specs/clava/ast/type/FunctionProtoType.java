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
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clava.ast.type;

import java.util.Collection;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.LegacyToDataStore;
import pt.up.fe.specs.clava.ast.NotSupportedByDataStoreException;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.extra.VariadicType;
import pt.up.fe.specs.clava.ast.type.data.FunctionProtoTypeData;
import pt.up.fe.specs.clava.ast.type.data.FunctionTypeData;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.clava.ast.type.enums.ExceptionSpecificationType;
import pt.up.fe.specs.clava.language.ReferenceQualifier;
import pt.up.fe.specs.util.SpecsCollections;

public class FunctionProtoType extends FunctionType {

    /// DATAKEYS BEGIN

    public final static DataKey<Integer> NUM_PARAMETERS = KeyFactory.integer("numParameters");

    public final static DataKey<Boolean> HAS_TRAILING_RETURNS = KeyFactory.bool("hasTrailingReturn");

    public final static DataKey<Boolean> IS_VARIADIC = KeyFactory.bool("isVariadic");

    public final static DataKey<Boolean> IS_CONST = KeyFactory.bool("isConst");

    public final static DataKey<Boolean> IS_VOLATILE = KeyFactory.bool("isVolatile");

    public final static DataKey<Boolean> IS_RESTRICT = KeyFactory.bool("isRestrict");

    public final static DataKey<ReferenceQualifier> REFERENCE_QUALIFIER = KeyFactory
            .enumeration("referenceQualifier", ReferenceQualifier.class);

    public final static DataKey<ExceptionSpecificationType> EXCEPTION_SPECIFICATION_TYPE = KeyFactory
            .enumeration("exceptionSpecificationType", ExceptionSpecificationType.class);

    public final static DataKey<Expr> NOEXCEPT_EXPR = KeyFactory.object("noexceptExpr", Expr.class);

    /// DATAKEYS END

    public FunctionProtoType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public FunctionProtoType(FunctionProtoTypeData functionProtoTypeData, FunctionTypeData functionTypeData,
            TypeData typeData, ClavaNodeInfo info, Type returnType, Collection<? extends Type> arguments) {

        this(new LegacyToDataStore()
                .setNodeInfo(info)
                .setType(typeData)
                .setFunctionType(functionTypeData)
                .setFunctionProtoType(functionProtoTypeData)
                .getData(),

                SpecsCollections.concat(returnType, arguments));

        // Has to check if last argument is of variadic type, and adjust number of arguments accordingly
        int numParams = arguments.size();
        boolean isVariadic = arguments.stream().filter(type -> type instanceof VariadicType)
                .findFirst().isPresent();
        if (isVariadic) {
            --numParams;
        }

        getDataI().add(NUM_PARAMETERS, numParams);
        getDataI().add(IS_VARIADIC, isVariadic);

    }

    /**
     * Legacy support.
     * 
     * @param functionTypeData
     * @param typeData
     * @param info
     * @param children
     */
    protected FunctionProtoType(FunctionTypeData functionTypeData,
            TypeData typeData, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
        super(functionTypeData, typeData, info, children);

    }

    /**
     * @deprecated
     * @return
     */
    @Deprecated
    public FunctionProtoTypeData getFunctionProtoTypeData() {
        throw new NotSupportedByDataStoreException();
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
        return getIndexParamStart() + get(NUM_PARAMETERS);
    }

    public String getCodeAfterParams() {
        StringBuilder code = new StringBuilder();

        // Add const/volatile
        if (get(IS_CONST)) {
            code.append(" const");
        }
        if (get(IS_VOLATILE)) {
            code.append(" volatile");
        }

        String exceptCode = get(EXCEPTION_SPECIFICATION_TYPE).getCode(get(NOEXCEPT_EXPR));
        code.append(exceptCode);

        return code.toString();
    }

}

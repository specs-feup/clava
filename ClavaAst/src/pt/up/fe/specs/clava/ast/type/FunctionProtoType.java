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

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.NotSupportedByDataStoreException;
import pt.up.fe.specs.clava.ast.type.data.FunctionProtoTypeData;
import pt.up.fe.specs.clava.ast.type.data.FunctionTypeData;
import pt.up.fe.specs.clava.ast.type.data.TypeData;

public class FunctionProtoType extends FunctionType {

    /// DATAKEYS BEGIN

    public final static DataKey<Integer> NUM_PARAMETERS = KeyFactory.integer("numParameters");

    /// DATAKEYS END

    // public FunctionProtoType(DataStore data, Collection<? extends ClavaNode> children) {
    // super(data, children);
    // }

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

    /*
    public String getCodeAfterParams() {
        StringBuilder code = new StringBuilder();
    
        // Add const/volatile
        if (ptData.isConst()) {
            code.append(" const");
        }
        if (ptData.isVolatile()) {
            code.append(" volatile");
        }
    
        code.append(getCodeExcept(ptData));
    
        return code.toString();
    }
    */
}

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

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

public class ParmVarDecl extends VarDecl {

    /// DATAKEYS BEGIN

    public final static DataKey<Boolean> HAS_INHERITED_DEFAULT_ARG = KeyFactory.bool("hasInheritedDefaultArg");

    /// DATAKEYS END

    public ParmVarDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    /**
     * 
     * @return the FunctionDecl where this ParmVarDecl appears
     */
    public FunctionDecl getFunctionDecl() {
        return getAncestor(FunctionDecl.class);
    }

    @Override
    public String getInitializationCode() {
        if (get(HAS_INHERITED_DEFAULT_ARG)) {
            return "";
        }

        return super.getInitializationCode();
    }

    /*
    @Override
    public String getDeclName() {
        // ParmVarDecl can have a null name
        return getDeclNameInternal();
    }
    */
}

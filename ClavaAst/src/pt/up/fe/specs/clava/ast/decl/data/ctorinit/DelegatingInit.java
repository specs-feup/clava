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

package pt.up.fe.specs.clava.ast.decl.data.ctorinit;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clava.ast.decl.CXXConstructorDecl;
import pt.up.fe.specs.clava.ast.type.Type;

public class DelegatingInit extends CXXCtorInitializer {

    /// DATAKEYS BEGIN

    public final static DataKey<Type> DELEGATED_TYPE = KeyFactory.object("delegatedType", Type.class);

    /// DATAKEYS END

    @Override
    public String getCode(CXXConstructorDecl sourceNode) {
        return sourceNode.get(CXXConstructorDecl.DECL_NAME) + "(" + get(INIT_EXPR).getCode() + ")";
    }
}

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

package pt.up.fe.specs.clava.ast.type.data;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.type.FunctionProtoType;

public class UnevaluatedExceptionSpecification extends ExceptionSpecification {

    /// DATAKEYS BEGIN

    /**
     * The FunctionDecl corresponding to this exception specification.
     * 
     */
    public final static DataKey<Decl> SOURCE_DECL = KeyFactory.object("sourceDecl", Decl.class);

    /// DATAKEYS END

    @Override
    public String getCode(FunctionProtoType type) {
        return "noexcept";
    }

    public Decl getSourceDecl(FunctionProtoType type) {
        return get(SOURCE_DECL);
    }
}

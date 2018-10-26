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

public class BaseInit extends CXXCtorInitializer {

    /// DATAKEYS BEGIN

    public final static DataKey<Type> BASE_CLASS = KeyFactory.object("baseClass", Type.class);

    /// DATAKEYS END

    @Override
    public String getCode(CXXConstructorDecl sourceNode) {
        // System.out.println("BASE CTOR DATA: " + toString());
        // System.out.println("BASE CTOR CONSTRUCTOR:" + sourceNode.toTree());
        // System.out.println("BASE INIT EXPR: " + get(BaseInit.INIT_EXPR).getCode());
        return get(BASE_CLASS).getCode(sourceNode) + "(" + get(BaseInit.INIT_EXPR).getCode() + ")";
    }
}

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
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.FieldDecl;

public class AnyMemberInit extends CXXCtorInitializer {

    /// DATAKEYS BEGIN

    // TODO: Change to FieldDecl
    public final static DataKey<Decl> ANY_MEMBER_DECL = KeyFactory.object("anyMemberDecl", Decl.class);

    /// DATAKEYS END

    @Override
    public String getCode(CXXConstructorDecl sourceNode) {
        System.out.println("ANY MEMBER DECL:" + get(ANY_MEMBER_DECL));
        // System.out.println("ANY MEMBER CODE:" + get(ANY_MEMBER_DECL).get(FieldDecl.DECL_NAME) + "("
        // + get(INIT_EXPR).getCode() + ")");
        // System.out.println("INIT EXPR:" + get(INIT_EXPR).getCode());
        // System.out.println("INIT EXPR TREE:" + get(INIT_EXPR).toTree());
        // System.out.println("ANY MEMBER CODE:" + get(ANY_MEMBER_DECL).get(FieldDecl.DECL_NAME) + "("
        // + get(INIT_EXPR).getCode() + ")");
        //
        // System.out.println("INIT EXPR:" + get(INIT_EXPR).getCode());
        // System.out.println("INIT EXPR TREE:" + get(INIT_EXPR).toTree());
        return get(ANY_MEMBER_DECL).get(FieldDecl.DECL_NAME) + "(" + get(INIT_EXPR).getCode() + ")";
    }
}

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

import org.suikasoft.jOptions.DataStore.ADataClass;
import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import pt.up.fe.specs.clava.ast.decl.CXXConstructorDecl;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.language.CXXCtorInitializerKind;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

/**
 * Represents a C++ base or member initializer.
 * 
 * This is part of a constructor initializer that initializes one non-static member variable or one base class.
 * 
 * @author JoaoBispo
 *
 */
public abstract class CXXCtorInitializer extends ADataClass<CXXCtorInitializer> {

    /// DATAKEYS BEGIN

    /**
     * The initializer.
     */
    public final static DataKey<Expr> INIT_EXPR = KeyFactory.object("initExpr", Expr.class);

    public final static DataKey<CXXCtorInitializerKind> INIT_KIND = KeyFactory.enumeration("initKind",
            CXXCtorInitializerKind.class);

    /// DATAKEYS END

    public static CXXCtorInitializer newInstance(CXXCtorInitializerKind initKind) {
        return newInstanceEmpty(initKind).set(CXXCtorInitializer.INIT_KIND, initKind);
    }

    private static CXXCtorInitializer newInstanceEmpty(CXXCtorInitializerKind initKind) {
        switch (initKind) {
        case ANY_MEMBER_INITIALIZER:
            return new AnyMemberInit();
        case BASE_INITIALIZER:
            return new BaseInit();
        case DELEGATING_INITIALIZER:
            return new AnyMemberInit();
        default:
            throw new NotImplementedException(initKind);
        }

    }

    public abstract String getCode(CXXConstructorDecl sourceNode);
    /*
    public String getCode(CXXConstructorDecl sourceNode) {
    
        Expr initExpr = getInitExpr();
    
        switch (kind) {
        case DELEGATING_INITIALIZER:
            // return initExpr.getCode();
            return getConstructor().getDeclName() + "(" + initExpr.getCode() + ")";
        case ANY_MEMBER_INITIALIZER:
            return anyMemberData.getDeclName() + "(" + initExpr.getCode() + ")";
        case BASE_INITIALIZER:
            return initType.getCode(this) + "()";
        default:
            throw new RuntimeException("Case not implemented: " + kind + " (loc:" + getParent().getLocation() + ")");
    
        }
    
    }
    */

}

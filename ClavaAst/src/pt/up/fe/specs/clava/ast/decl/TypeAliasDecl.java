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

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.type.Type;

public class TypeAliasDecl extends TypedefNameDecl {

    public TypeAliasDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // public TypeAliasDecl(Type underlyingType, String declName, Type type, DeclData declData, ClavaNodeInfo info) {
    // this(underlyingType, declName, type, declData, info, Collections.emptyList());
    // }
    //
    // private TypeAliasDecl(Type underlyingType, String declName, Type type, DeclData declData, ClavaNodeInfo info,
    // Collection<? extends ClavaNode> children) {
    // super(underlyingType, declName, type, declData, info, children);
    //
    // }

    // @Override
    // protected ClavaNode copyPrivate() {
    // return new TypeAliasDecl(getUnderlyingType(), getDeclName(), getType(), getDeclData(), getInfo(),
    // Collections.emptyList());
    // }

    public Type getAliasedType() {
        // return getChild(Type.class, 0);
        return getUnderlyingType();
    }

    @Override
    public String getCode() {
        return "using " + getDeclName() + " = " + getAliasedType().getCode(this) + ";";
    }

}

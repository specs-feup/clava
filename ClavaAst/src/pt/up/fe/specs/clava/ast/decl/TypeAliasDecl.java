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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.type.Type;

public class TypeAliasDecl extends TypedefNameDecl {

    public TypeAliasDecl(String declName, Type type, DeclData declData, ClavaNodeInfo info,
            Type aliasedType) {

        this(declName, type, declData, info, Arrays.asList(aliasedType));
    }

    private TypeAliasDecl(String declName, Type type, DeclData declData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {
        super(declName, type, declData, info, children);

    }

    @Override
    protected ClavaNode copyPrivate() {
        return new TypeAliasDecl(getDeclName(), getType(), getDeclData(), getInfo(), Collections.emptyList());
    }

    public Type getAliasedType() {
        return getChild(Type.class, 0);
    }

    @Override
    public String getCode() {
        return "using " + getDeclName() + " = " + getAliasedType().getCode() + ";";
    }

}

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
import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.TypedefType;

/**
 * Declaration of a typedef-name via the 'typedef' type specifier.
 * 
 * @author JoaoBispo
 *
 */
public class TypedefDecl extends TypedefNameDecl {

    private final boolean isModulePrivate;

    public TypedefDecl(boolean isModulePrivate, String declName, Type type, DeclData declData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {
        super(declName, type, declData, info, children);

        this.isModulePrivate = isModulePrivate;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new TypedefDecl(isModulePrivate, getDeclName(), getType(), getDeclData(), getInfo(),
                Collections.emptyList());
    }

    @Override
    public String getCode() {
        String typeCode = getType().getCode();
        // String typeCode = getCodeForType();
        // System.out.println("TYPEDEF DECL CODE OLD:" + getType().getCode());
        // System.out.println("TYPEDEF DECL CODE NEW:" + typeCode);
        return "typedef " + typeCode + " " + getTypelessCode();
        // return "typedef " + getType().getCode() + " " + getTypelessCode();
    }

    private String getCodeForType() {
        Type type = getType();

        if (type instanceof TypedefType) {
            return ((TypedefType) type).getTypeClass().getCode();
        }

        return type.getCode();
    }

    @Override
    public String getTypelessCode() {
        return getDeclName();
    }
}

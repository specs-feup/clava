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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.type.data.TypeData;
import pt.up.fe.specs.clava.ast.type.tag.DeclRef;

public class TypedefType extends Type {

    private final DeclRef declInfo;

    public TypedefType(DeclRef declInfo, TypeData typeData, ClavaNodeInfo info, Type classType) {
        this(declInfo, typeData, info, Arrays.asList(classType));
    }

    private TypedefType(DeclRef declInfo, TypeData typeData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {

        super(typeData, info, children);

        this.declInfo = declInfo;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new TypedefType(declInfo, getTypeData(), getInfo(), Collections.emptyList());
    }

    public DeclRef getDeclInfo() {
        return declInfo;
    }

    public Type getTypeClass() {
        return getChild(Type.class, 0);
    }

    @Override
    public String getCode(String name) {

        // System.out.println("TYPEDEF TYPE CODE:" + super.getCode(name));
        // System.out.println("TYPEDEF TYPE CHILD CODE:" + getTypeClass().getCode(name));
        Type typeClass = getTypeClass();

        if (typeClass instanceof ElaboratedType) {
            if (name == null) {
                return declInfo.getDeclType();
            }

            return declInfo.getDeclType() + " " + name;
            // if (declInfo.getDeclType().equals("FILE")) {
            // return declInfo.getDeclType();
            // }
            // System.out.println("TypedefType before:" + super.getCode(name));
            // System.out.println("TypedefType after:" + typeClass.getCode(name));
            // return typeClass.getCode(name);
        }

        Optional<TemplateSpecializationType> templateSpecialization = typeClass.getDescendantsAndSelfStream()
                .filter(descendent -> descendent instanceof TemplateSpecializationType)
                .map(descendent -> (TemplateSpecializationType) descendent)
                .findFirst();

        if (templateSpecialization.isPresent()) {
            // System.out.println("TYPEDEF TEMPLATE:" + templateSpecialization.get());
            // System.out.println("TYPEDEF TEMPLATE CODE:" + templateSpecialization.get().getCode(name));
            return templateSpecialization.get().getCode(name);
        }

        // // If typedef of a TemplateSpecializationType, might need to use more specialized type
        // if (typeClass instanceof TemplateSpecializationType || typeClass instanceof TypedefType) {
        // return getTypeClass().getCode(name);
        // }
        return super.getCode(name);
        // return getType() + " " + name;

        // return getTypeClass().getCode(name);
        // System.out.println("GET CLASS CODE:" + getTypeClass().getCode(name));
        // String typeCode = getType();

        // // HACK
        // if (typeCode.equals("string")) {
        // typeCode = "std::" + typeCode;
        // }

        // return typeCode;
    }

    @Override
    protected Type desugarImpl() {
        return getTypeClass();
    }

}

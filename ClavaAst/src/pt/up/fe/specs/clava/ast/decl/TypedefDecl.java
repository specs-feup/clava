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

import java.util.Collections;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.type.PointerType;
import pt.up.fe.specs.clava.ast.type.Type;

/**
 * Declaration of a typedef-name via the 'typedef' type specifier.
 * 
 * @author JoaoBispo
 *
 */
public class TypedefDecl extends TypedefNameDecl {

    private final String typedefSource;
    private final boolean isModulePrivate;

    public TypedefDecl(Type underlyingType, String typedefSource, boolean isModulePrivate, String declName, Type type,
            DeclData declData,
            ClavaNodeInfo info) {
        super(underlyingType, declName, type, declData, info, Collections.emptyList());

        this.typedefSource = typedefSource;
        this.isModulePrivate = isModulePrivate;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new TypedefDecl(getUnderlyingType(), typedefSource, isModulePrivate, getDeclName(), getType(),
                getDeclData(), getInfo());
    }

    @Override
    public Type getType() {
        // System.out.println("CALLING TYPEDEF_DECL.getType()");
        // System.out.println("Type for decl: " + get(TYPE_FOR_DECL));
        // HACK: Sometimes, the returned type is the same as type being declared, we have not discovered why
        // In this case, desugar type
        Type type = super.getType();

        if (type.getCode(this).equals(getDeclName())) {

            Type desugared = type.desugar();

            // System.out.println("RETURNING DESUGARED: " + desugared);

            return desugared;
        }

        // System.out.println("RETURNING SUPER: " + type);
        return type;
    }

    @Override
    public String getCode() {
        Type type = getType();

        // if (getDeclName().equals("IT")) {
        // System.out.println("LOCATION:" + getLocation());
        // System.out.println("TYPEDEF DECL TYPE:\n" + type);
        // }

        // If pointer to ParenType, there can be complicated situations such
        // as having function pointer with VLAs that need the name of parameters,
        // which are not available for function types in Clang
        if (PointerType.isPointerToParenType(type)) {
            return getLocation().getSource()
                    .orElseThrow(() -> new RuntimeException("Could not find source for location " + getLocation()));
        }

        String typeCode = type.getCode(this);
        // System.out.println("TYPE TREE:" + type.toTree());
        // System.out.println("TYPE CODE:" + typeCode);
        String code = "typedef " + typeCode + " " + getTypelessCode();

        // if (typeCode.equals("std::set<double>::const_iterator")) {
        // System.out.println("TYPEDEF TYPE:" + type);
        // System.out.println("TYPEDEF TYPE ARGS:" + type.getTemplateArgumentTypes());
        // System.out.println("TYPEDEF TYPE ARGS STRINGS:" + type.getTemplateArgumentStrings());
        // }

        // type.setTemplateArgumentTypes(Arrays.asList(ClavaNodeFactory.builtinType("float")));
        // System.out.println("TYPEDEF TYPE 2:" + type);
        // System.out.println("TYPEDEF TYPE 2 code:" + type.getCode());
        // if (getDeclName().equals("IT")) {
        // System.out.println("TYPEDEF DECL CODE:" + code);
        // }

        return code;
    }

    /*
    private String getCodeForType() {
        Type type = getType();
    
        if (type instanceof TypedefType) {
            return ((TypedefType) type).getTypeClass().getCode();
        }
    
        return type.getCode();
    }
    */
    @Override
    public String getTypelessCode() {
        return getDeclName();
    }

}

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

import java.util.Collection;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.EnumDecl;
import pt.up.fe.specs.clava.ast.decl.TagDecl;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.util.lazy.Lazy;

/**
 * Represents an enum.
 * 
 * @author JoaoBispo
 *
 */
public class EnumType extends TagType {

    private static final String ANON_ENUM_PREFIX = "anon_enum_";
    // private final String anonName;

    private final Lazy<String> anonymousId = Lazy
            .newInstance(() -> getContext().get(ClavaContext.ID_GENERATOR).next(ANON_ENUM_PREFIX));

    public EnumType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);

        // SpecsStrings.
        // UUID.randomUUIDcs()
    }

    /*
    public EnumType(DeclRef declInfo, TypeData typeData, ClavaNodeInfo info) {
        this(declInfo, typeData, info, Collections.emptyList());
    }
    
    private EnumType(DeclRef declInfo, TypeData typeData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {
        super(declInfo, TagKind.ENUM, typeData, info, children);
    }
    
    @Override
    protected ClavaNode copyPrivate() {
        return new EnumType(getDeclInfo(), getTypeData(), getInfo(), Collections.emptyList());
    }
    */

    public EnumDecl getEnumDecl(App app) {
        return (EnumDecl) get(DECL);
        /*
        ClavaNode declNode = app.getNode(getDeclInfo().getDeclId());
        
        if (!(declNode instanceof EnumDecl)) {
            throw new WrongClassException(declNode, EnumDecl.class);
        }
        
        return (EnumDecl) declNode;
        */
    }

    @Override
    public String getCode(ClavaNode sourceNode, String name) {

        // System.out.println("SOURCE NODE:" + sourceNode.getClass());

        String baseType = getDecl().get(TagDecl.DECL_NAME);

        // System.out.println("DECL NAME:" + baseType);
        // String baseType = getDecl().get(TagDecl.TYPE_FOR_DECL).getCode();
        // String baseType = baseTypeNode.getCode();
        // String baseType = getBareType();
        if (baseType.isEmpty()) {
            baseType = getBareType();
        }

        if (isAnonymous()) {
            // baseType = "anon_" + getId();
            baseType = anonymousId.get();

        }

        // String enumType = getTagKind().getCode() + " " + baseType;
        String enumType = baseType;

        if (name == null) {
            return enumType;
        }

        return enumType + " " + name;
    }

    public boolean isAnonymous() {
        // HACK: Is it possible to obtain this info from Clang?
        return getBareType().contains("(anonymous at");
    }
    /*
    @Override
    public String getCode(ClavaNode sourceNode, String name) {
    
        EnumDecl enumDecl = (EnumDecl) get(DECL);
    
        String declName = enumDecl.get(EnumDecl.DECL_NAME);
    
        String enumType = getTagKind().getCode();
        if (name == null || name.isEmpty()) {
            System.out.println("ENUM TYPE:" + enumType);
            return enumType;
        }
    
        enumType += " " + declName;
    
        return enumType + " " + name;
    }
    */

}

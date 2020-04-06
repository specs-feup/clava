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

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.TypedefNameDecl;
import pt.up.fe.specs.clava.ast.decl.ValueDecl;

/**
 * Represents the type of a typedef.
 * 
 * @author jbispo
 *
 */
public class TypedefType extends Type {

    /// DATAKEYS BEGIN

    public final static DataKey<TypedefNameDecl> DECL = KeyFactory.object("decl", TypedefNameDecl.class);

    /// DATAKEYS END

    public TypedefType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public Type desugar() {
        return get(DECL).getUnderlyingType();
    }

    /*
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
    */
    /*
    public DeclRef getDeclInfo() {
        return declInfo;
    }
    */

    public Type getTypeClass() {
        Decl decl = get(DECL);

        if (!(decl instanceof ValueDecl)) {
            return null;
        }

        return decl.hasValue(ValueDecl.TYPE) ? decl.get(ValueDecl.TYPE) : null;
        // return getChild(Type.class, 0);
    }

    // public void setTypeClass(Type typeClass) {
    // setChild(0, typeClass);
    // }

    @Override
    public String getCode(ClavaNode sourceNode, String name) {

        // Type typeClass = getTypeClass();
        // if (hasSugar() && desugar() instanceof ElaboratedType) {
        // System.out.println("ELABORATED:" + desugar().getCode(sourceNode, name));
        // return desugar().getCode(sourceNode, name);
        // }
        /*
        System.out.println("TYPE AS STRING:" + get(TYPE_AS_STRING));
        System.out.println("TYPEDEF TYPE:" + this);
        System.out.println("TYPE CLASS:" + getTypeClass());
        // System.out.println("DECL:" + get(DECL));
        // System.out.println("DECL CODE:" + get(DECL).getCode());
        System.out.println("UNDERLYING TYPE: " + get(DECL).get(TypedefNameDecl.UNDERLYING_TYPE).getCode());
          */

        // String type = get(TYPE_AS_STRING);
        // System.out.println("TYPE AS STRING:" + get(TYPE_AS_STRING));
        // String type = get(DECL).get(TypedefNameDecl.UNDERLYING_TYPE).getCode();
        String type = get(DECL).getTypelessCode();

        return name == null ? type : type + " " + name;
        /*
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
            return templateSpecialization.get().getCode(sourceNode, name);
        }
        
        // // If typedef of a TemplateSpecializationType, might need to use more specialized type
        // if (typeClass instanceof TemplateSpecializationType || typeClass instanceof TypedefType) {
        // return getTypeClass().getCode(name);
        // }
        return super.getCode(sourceNode, name);
        // return getType() + " " + name;
        
        // return getTypeClass().getCode(name);
        // System.out.println("GET CLASS CODE:" + getTypeClass().getCode(name));
        // String typeCode = getType();
        
        // // HACK
        // if (typeCode.equals("string")) {
        // typeCode = "std::" + typeCode;
        // }
        
        // return typeCode;
         
         */
    }

    /*
    @Override
    protected Type desugarImpl() {
        return getTypeClass();
    }
    
    @Override
    protected void setDesugarImpl(Type desugaredType) {
        setTypeClass(desugaredType);
    }
    */

}

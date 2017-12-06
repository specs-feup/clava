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
import java.util.List;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.type.data.QualTypeData;
import pt.up.fe.specs.clava.ast.type.data.Qualifier;
import pt.up.fe.specs.clava.ast.type.data.TypeData;

/**
 * Represents a set of qualifiers for a type.
 *
 * @author JoaoBispo
 *
 */
public class QualType extends Type {

    private final QualTypeData qualTypeData;
    // private final List<Qualifier> qualifiers;

    public QualType(QualTypeData qualTypeData, TypeData typeData, ClavaNodeInfo info, Type qualifiedType) {
        this(qualTypeData, typeData, info, Arrays.asList(qualifiedType));
    }

    private QualType(QualTypeData qualTypeData, TypeData typeData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {
        super(typeData, info, children);

        this.qualTypeData = qualTypeData;
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new QualType(qualTypeData, getTypeData(), getInfo(), Collections.emptyList());
    }

    public List<Qualifier> getQualifiers() {
        return qualTypeData.getQualifiers();
    }

    @Override
    public String getCode(String name) {
        String type = getQualifiedType().getCode(name);

        // System.out.println("QUAL:" + getExtendedId());
        // If not a top-level qualifier, has to be put after the type, but before the name
        if (hasParent()) {

            // But only if there are no other QualType ancestors, or if there is,
            // the parent must be a Pointer or a Reference, to avoid invalid double 'const' qualifiers
            boolean hasQualTypeAncestor = getAncestorTry(QualType.class).isPresent();

            ClavaNode parent = getParent();

            boolean allowedTypes = parent instanceof PointerType || parent instanceof ReferenceType;
            // boolean allowedTypes = parent instanceof PointerType || parent instanceof ArrayType;

            if (hasQualTypeAncestor && !allowedTypes) {
                // System.out.println("QUAL: " + getExtendedId());
                // System.out.println("NOT ALLOWED:" + parent.getNodeName());
                return type;
            }

            // System.out.println("QUALTYPE");
            // System.out.println("PARENT:" + getParent().getNodeName());
            // System.out.println("QUALIFIED:" + getQualifiedType().getNodeName());

            // return getCode(type, name);
            /*
            if (name != null) {
                int index = type.indexOf(name);
                Preconditions.checkArgument(index != -1);
                return type.substring(0, index) + qualifier + " " + type.substring(index);
            }
            System.out.println("NOT TOP:" + type + " " + qualifier);
            return type + " " + qualifier;
            */
        }

        return getCode(type, name);
        // Types in C++ should be read right-to-left. However, top-level qualifiers can be written on the left-side
        // http://stackoverflow.com/questions/19415674/what-does-const-mean-in-c
        // System.out.println("TOP:" + qualifier + " " + type);
        // return qualifier + " " + type;

        // String nameString = name == null ? "" : " " + name;
        //
        // // return getQualifiedType().getCode() + " " + qualifier + nameString;
        //
        // return qualifier + " " + getQualifiedType().getCode(nameString);
    }

    private String getCode(String type, String name) {

        String addressQualifier = qualTypeData.getAddressSpaceQualifier().getCode();
        if (!addressQualifier.isEmpty()) {
            addressQualifier += " ";
        }

        // String qualifiersCode = ClavaCode.getQualifiersCode(getQualifiers());
        String qualifiersCode = qualTypeData.getQualifiersCode();
        // Type child = getQualifiedType();

        if (name != null) {
            // if (hasParent()) {
            // SpecsLogs.msgWarn("Qualtype has parent, check if this case is ok");
            // }
            //
            // return qualifiersCode + " " + type;
            int index = type.lastIndexOf(name);
            Preconditions.checkArgument(index != -1);
            return addressQualifier + type.substring(0, index) + qualifiersCode + " " + type.substring(index);
        }

        return addressQualifier + type + " " + qualifiersCode;

    }

    /*
    @Override
    public String getCode(String name) {
    
        // If not a top-level qualifier, has to be put after the type, but before the name
        if (hasParent()) {
    
            // But only if there are no other QualType ancestors, or if there is,
            // the parent must be a Pointer or a Reference, to avoid invalid double 'const' qualifiers
            boolean hasQualTypeAncestor = getAncestorTry(QualType.class).isPresent();
    
            ClavaNode parent = getParent();
    
            boolean allowedTypes = parent instanceof PointerType || parent instanceof ReferenceType;
    
            if (hasQualTypeAncestor && !allowedTypes) {
                return getQualifiedType().getCode(name);
            }
        }
    
        // Types in C++ should be read right-to-left. However, top-level qualifiers can be written on the left-side
        // http://stackoverflow.com/questions/19415674/what-does-const-mean-in-c
        return getCodePrivate(name);
    
        // System.out.println("TOP:" + qualifier + " " + type);
        // return qualifier + " " + type;
    
        // String nameString = name == null ? "" : " " + name;
        //
        // // return getQualifiedType().getCode() + " " + qualifier + nameString;
        //
        // return qualifier + " " + getQualifiedType().getCode(nameString);
    }
    */
    /*
    private String getCodePrivate(String name) {
        String type = getQualifiedType().getCode(name);
        String qualifiersCode = ClavaCode.getQualifiersCode(qualifiers);
    
        // Case where qualifier has to come after the type but before the name taken care previously (we think)
        if (name != null) {
            if (hasParent()) {
                SpecsLogs.msgWarn("Qualtype has parent, check if this case is ok");
            }
    
            return qualifiersCode + " " + type;
        }
    
        return type + " " + qualifiersCode;
    
    }
    */
    public Type getQualifiedType() {
        return getChild(Type.class, 0);
    }

    @Override
    public boolean isConst() {
        if (getQualifiers().contains(Qualifier.CONST)) {
            return true;
        }

        return getQualifiedType().isConst();
    }

}

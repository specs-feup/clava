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
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.type.data.TypeData;

/**
 * Represents a type that was referred to using an elaborated type keyword, e.g., struct S, or via a qualified name,
 * e.g., N::M::type, or both.
 * 
 * <p>
 * This type is used to keep track of a type name as written in the source code, including tag keywords and any
 * nested-name-specifiers. The type itself is always "sugar", used to express what was written in the source code but
 * containing no additional semantic information.
 * 
 * @author JoaoBispo
 *
 */
public class ElaboratedType extends TypeWithKeyword {

    // private static final ClassSet<Type> BARE_TYPE_CLASSES = ClassSet.newInstance(RecordType.class);

    public ElaboratedType(ElaboratedTypeKeyword keyword, TypeData typeData, ClavaNodeInfo info,
            Type namedType) {
        this(keyword, typeData, info, Arrays.asList(namedType));
    }

    private ElaboratedType(ElaboratedTypeKeyword keyword, TypeData typeData, ClavaNodeInfo info,
            Collection<? extends ClavaNode> children) {
        super(keyword, typeData, info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {
        return new ElaboratedType(getKeyword(), getTypeData(), getInfo(), Collections.emptyList());
    }

    @Override
    public Type getNamedType() {
        return getChild(Type.class, 0);
    }

    public void setNamedType(Type namedType) {
        setChild(0, namedType);
    }

    /*
    @Override
    public boolean isAnonymous() {
    // return getNamedType() instanceof NullType;
    return getNamedType().isAnonymous();
    }
    */

    @Override
    public String getCode(ClavaNode sourceNode, String name) {
        // HACK Set of Type classes whose .getCode() is not working properly, using bare type

        // System.out.println("ELABORATED:" + namedType);
        // if (BARE_TYPE_CLASSES.contains(namedType)) {
        // String bareType = getBareType();
        //
        // if (name == null) {
        // return bareType;
        // }
        //
        // return bareType + " " + name;
        // }

        // If named type is a TemplateSpecializationType, return its code
        // Return code of the desugared version
        // return getNamedType().getCode(name);

        String bareType = getBareType();

        // HACK
        // if (getKeyword() != ElaboratedTypeKeyword.STRUCT && ) {
        // if (!bareType.startsWith("struct ")) {
        // System.out.println("BEFORE:" + bareType);
        // bareType = bareType.replaceAll("struct ", "");
        // System.out.println("AFTER:" + bareType);
        // }

        // If named type has template arguments, update them
        Type namedType = getNamedType();

        // System.out.println("BARE TYPE:" + bareType);
        // System.out.println("NAMED TYPE:" + namedType);
        // System.out.println("HAS UPDATED:" + namedType.hasUpdatedTemplateArgTypes());
        // System.out.println("TEMPLATE ARGS:" + namedType.getTemplateArgumentTypes());

        if (bareType.equals("std::set<double>::const_iterator")) {
            // System.out.println("HELLOA!!!");
            // System.out.println("NAMED TYPE:" + namedType);
            // System.out.println("ARGS:" + namedType.getTemplateArgumentTypes());
        }

        if (namedType.hasUpdatedTemplateArgTypes()) {

            int startIndex = bareType.indexOf('<');
            int endIndex = bareType.lastIndexOf('>');
            boolean hasTemplateArgs = startIndex != -1 && endIndex != -1;

            // Preconditions.checkArgument(startIndex != -1 && endIndex != -1,
            // "Named type has template arguments, expected bare type to have them too: " + bareType);

            if (hasTemplateArgs) {
                String templateArgs = namedType.getTemplateArgumentTypes().stream()
                        .map(type -> type.getCode(sourceNode))
                        .collect(Collectors.joining(", "));
                bareType = bareType.substring(0, startIndex + 1) + templateArgs + bareType.substring(endIndex);
            }

        }

        if (name == null) {
            return bareType;
        }

        return bareType + " " + name;

    }

    @Override
    protected Type desugarImpl() {
        return getNamedType();
    }

    @Override
    protected void setDesugarImpl(Type desugaredType) {
        setNamedType(desugaredType);
    }

    /**
     * 
     * @param typeAsString
     * @return
     */
    public ElaboratedType setTypeAsString(String typeAsString) {
        set(ElaboratedType.TYPE_AS_STRING, typeAsString);
        /*
        if (hasDataI()) {
            getDataI().set(ElaboratedType.TYPE_AS_STRING, typeAsString);
        } else {
            getTypeData().setBareType(typeAsString);
        }
        */
        return this;

    }

}

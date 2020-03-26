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
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;

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

    /// DATAKEYS BEGIN

    public final static DataKey<String> QUALIFIER = KeyFactory.string("qualifier");

    public final static DataKey<Type> NAMED_TYPE = KeyFactory.object("namedType", Type.class);

    /// DATAKEYS END

    // private static final ClassSet<Type> BARE_TYPE_CLASSES = ClassSet.newInstance(RecordType.class);

    public ElaboratedType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    /*
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
    */

    public Type getNamedType() {
        return get(NAMED_TYPE);
        // return getChild(Type.class, 0);
    }

    public void setNamedType(Type namedType) {
        set(NAMED_TYPE, namedType);
        // setChild(0, namedType);
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

        String code = getKeyword().getCode();
        if (!code.isEmpty()) {
            code += " ";
        }

        String qualifier = getQualifier();
        code += qualifier + getNamedType().getCode(sourceNode, name);

        // code += getNamedType().getCode(sourceNode, name);

        // if (getNamedType().getCode(sourceNode, name).startsWith("%Dummy")) {
        // System.out.println("NAMED TYPE:" + getNamedType().toTree());
        // }
        // System.out.println("NAMED TYPE:" + getNamedType().getClass());
        // System.out.println("NAMED TYPE CODE:" + getNamedType().getCode(sourceNode, name));
        // System.out.println("ELABORATED CODE:" + code);
        // System.out.println("QUALIFIER:" + get(QUALIFIER));
        // System.out.println("TEMPLATE ARGS:" + getTemplateArgumentStrings(this));
        return code;

        /*     
        String bareType = getBareType();
        
             
        // If named type has template arguments, update them
        Type namedType = getNamedType();
        
             
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
        */
    }

    /**
     * Takes into account if TemplateArguments where changed.
     * 
     * @return
     */
    public String getQualifier() {
        String qualifier = get(QUALIFIER);
        if (!hasUpdatedTemplateArgTypes()) {
            return qualifier;
        }

        if (!qualifier.contains("<") && !qualifier.contains(">")) {
            return qualifier;
            // String templateStrings = getTemplateArgumentStrings(this).stream().collect(Collectors.joining(","));
            // throw new RuntimeException("Expected to find a '<' in qualifier '" + qualifier
            // + "', implement case when this is not present. Template types: " + templateStrings);
        }

        // Not sure if this is the best way to detect the template parameters
        int startIndex = qualifier.indexOf('<');
        int endIndex = qualifier.lastIndexOf('>');

        String newTemplateTypes = getTemplateArgumentStrings(this).stream().collect(Collectors.joining(","));

        String newQualifier = qualifier.substring(0, startIndex + 1) + newTemplateTypes
                + qualifier.substring(endIndex, qualifier.length());

        return newQualifier;
    }

    // @Override
    // protected Type desugarImpl() {
    // return getNamedType();
    // }
    //
    // @Override
    // protected void setDesugarImpl(Type desugaredType) {
    // setNamedType(desugaredType);
    // }

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

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

    public ElaboratedType(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public Type desugar() {
        return getNamedType();
    }

    public Type getNamedType() {
        return get(NAMED_TYPE);
    }

    public void setNamedType(Type namedType) {
        set(NAMED_TYPE, namedType);
    }

    @Override
    public String getCode(ClavaNode sourceNode, String name) {

        String code = getKeyword().getCode();
        if (!code.isEmpty()) {
            code += " ";
        }

        var namedType = getNamedType();

        String qualifier = namedType.hasQualifier() ? "" : getQualifier();
        var namedTypeCode = namedType.getCode(sourceNode, name);
        code += qualifier + namedTypeCode;
        // code += getNamedType().getCode(sourceNode, name);
        // System.out.println("---- BEGIN ----");
        // System.out.println("Named type class: " + getNamedType().getClass());
        // System.out.println("Named type class desugared: " + getNamedType().desugarAll().getClass());
        // System.out.println("Named type as string: " + getNamedType().get(TYPE_AS_STRING));
        // System.out.println("Named type code simple: " + getNamedType().getCode());
        // System.out.println("Elaborated as string: " + get(TYPE_AS_STRING));
        // // System.out.println("Qualifier: " + getQualifier());
        // System.out.println("Elaborated code: " + code);
        // System.out.println("---- END ----");
        // System.out.println("Type as string 2: " + get(TYPE_AS_STRING));
        // System.out.println("named type class: " + getNamedType().getClass());
        // System.out.println("Named type code: " + getNamedType().getCode(sourceNode, name));
        // System.out.println("Code: " + code);
        return code;
    }

    /**
     * Takes into account if TemplateArguments where changed.
     * 
     * @param namedTypeCode
     * 
     * @return
     */
    public String getQualifier() {
        // If code already has qualifier, do not use qualifier
        // Consider only until a template appears
        // var templateIndexBegin = namedTypeCode.indexOf('<');
        // if (templateIndexBegin != -1) {
        // if (namedTypeCode.substring(0, templateIndexBegin).contains("::")) {
        // return "";
        // }
        // }

        String qualifier = get(QUALIFIER);
        if (!hasUpdatedTemplateArgTypes()) {
            return qualifier;
        }

        if (!qualifier.contains("<") && !qualifier.contains(">")) {
            return qualifier;
        }

        // Not sure if this is the best way to detect the template parameters
        int startIndex = qualifier.indexOf('<');
        int endIndex = qualifier.lastIndexOf('>');

        String newTemplateTypes = getTemplateArgumentStrings(this).stream().collect(Collectors.joining(","));

        String newQualifier = qualifier.substring(0, startIndex + 1) + newTemplateTypes
                + qualifier.substring(endIndex, qualifier.length());

        return newQualifier;
    }

    /**
     * 
     * @param typeAsString
     * @return
     */
    public ElaboratedType setTypeAsString(String typeAsString) {
        set(ElaboratedType.TYPE_AS_STRING, typeAsString);

        return this;
    }

}

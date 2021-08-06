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

        String qualifier = getQualifier();
        code += qualifier + getNamedType().getCode(sourceNode, name);

        return code;
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

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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.ast.decl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.attr.Attribute;
import pt.up.fe.specs.clava.ast.attr.enums.AttributeKind;
import pt.up.fe.specs.util.collections.SpecsList;

/**
 * Represents one declaration (or definition).
 * 
 * @author JoaoBispo
 *
 */
public abstract class Decl extends DeclBase {

    // Data declarations are generated from the wire schema in DeclBase.

    public Decl(DataStore data, Collection<? extends ClavaNode> children, DeclBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

    public Decl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    public boolean hasAttribute(AttributeKind kind) {
        return getWireAttributes().stream()
                .filter(attr -> attr.get(Attribute.KIND) == kind)
                .findFirst()
                .isPresent();
    }

    public String getAttributesCode() {
        var code = new StringBuilder();

        // Clang may report GNU attributes in a different order after reparsing. Sort only
        // GNU attribute spellings, keeping inline C++/CUDA and other attributes in place.
        List<Attribute> sortedAttributes = getWireAttributes().stream()
                .filter(Decl::isGnuAttribute)
                .sorted(Comparator.comparing(Attribute::getCode))
                .collect(Collectors.toList());
        Iterator<Attribute> sortedAttributeIterator = sortedAttributes.iterator();

        for (Attribute originalAttr : getWireAttributes()) {
            Attribute attr = isGnuAttribute(originalAttr) ? sortedAttributeIterator.next() : originalAttr;

            // If generic class, do not generated code for it
            if (attr.getClass().equals(Attribute.class)) {
                ClavaLog.info(
                        "Attribute '" + attr.getKind() + "' not implemented, not generating code for it");
                continue;
            }

            code.append(attr.getCode());

            if (attr.getKind().isInline()) {
                code.append(" ");
            } else {
                code.append("\n");
            }

        }

        return code.toString();
    }

    private static boolean isGnuAttribute(Attribute attr) {
        return !attr.getKind().isInline() && attr.getCode().startsWith("__attribute__((");
    }

    @Override
    public SpecsList<DataKey<?>> getSignatureKeys() {
        return super.getSignatureKeys().andAdd(IS_REFERENCED);
    }

}

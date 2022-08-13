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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.enums.Linkage;
import pt.up.fe.specs.clava.ast.decl.enums.NameKind;
import pt.up.fe.specs.clava.ast.decl.enums.Visibility;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.collections.SpecsList;

/**
 * Represents a decl with a name.
 * 
 * TODO: getDeclName() should be made abstract.
 * 
 * @author JoaoBispo
 *
 */
public abstract class NamedDecl extends Decl {

    /// DATAKEYS BEGIN

    public final static DataKey<String> QUALIFIED_PREFIX = KeyFactory.string("qualifiedPrefix");

    public final static DataKey<String> DECL_NAME = KeyFactory.string("declName");

    public final static DataKey<NameKind> NAME_KIND = KeyFactory.enumeration("nameKind", NameKind.class)
            .setDefault(() -> NameKind.IDENTIFIER);

    /**
     * True if this declaration is a C++ class member.
     */
    public final static DataKey<Boolean> IS_CXX_CLASS_MEMBER = KeyFactory.bool("isCXXClassMember");

    /**
     * True if this declaration is an instance member of a C++ class.
     */
    public final static DataKey<Boolean> IS_CXX_INSTANCE_MEMBER = KeyFactory.bool("isCXXInstanceMember");

    /**
     * The linkage of the declaration from a semantic point of view.
     * <p>
     * Entities in anonymous namespaces are external (in c++98).
     */
    public final static DataKey<Linkage> LINKAGE = KeyFactory.enumeration("linkage", Linkage.class);

    /**
     * The visibility of this entity.
     */
    public final static DataKey<Visibility> VISIBILITY = KeyFactory.enumeration("visibility", Visibility.class);

    /// DATAKEYS END

    public NamedDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    protected String processDeclName(String declName) {
        // return declName == null ? "" : declName;
        return declName != null && declName.isEmpty() ? null : declName;
    }

    protected Type processType(Type type) {
        return type == null ? getFactory().nullType() : type.copy();
    }

    public String getDeclName() {
        return get(DECL_NAME);
    }

    public void setDeclName(String declName) {
        set(DECL_NAME, declName);
    }

    public boolean hasDeclName() {

        String declName = getDeclName();

        if (declName.isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * The code of this decl, without the type declaration.
     * 
     * @return
     */
    public String getTypelessCode() {
        return getDeclName();
    }

    /**
     * The qualified prefix that should be used for this record in the scope it currently is.
     * 
     * <p>
     * For instance, if Decl has the qualified prefix a::b, and is inside the namespace a, returns b.
     */
    public Optional<String> getCurrentNamespace() {

        // Get namespace
        String namespace = getNamespace().orElse(null);

        if (namespace == null) {
            return Optional.empty();
        }

        // Split namespace
        List<String> namespaceElements = new ArrayList<>(Arrays.asList(namespace.split("::")));

        // Get namespace chain for this node
        List<ClavaNode> ancestors = this.getAncestors(Arrays.asList(NamespaceDecl.class, TagDecl.class));

        int prefixElementsToRemove = 0;

        var numToCheck = Math.min(ancestors.size(), namespaceElements.size());
        // SpecsCheck.checkArgument(ancestors.size() <= namespaceElements.size(),
        // () -> "Expected number of qualifier ancestors ("
        // + ancestors.stream().map(node -> ((NamedDecl) node).getDeclName()).collect(Collectors.toList())
        // + ") to be less or equal than the number of namespace elements (" + namespaceElements
        // + ")");

        // Remove elements that correspond to the same prefix in current node namespace
        // for (int i = 0; i < ancestors.size(); i++) {
        for (int i = 0; i < numToCheck; i++) {
            var ancestor = ancestors.get(i);
            SpecsCheck.checkArgument(ancestor instanceof NamedDecl, () -> "Expected a NamedDecl, got " + ancestor);

            var namedDecl = (NamedDecl) ancestor;
            // System.out.println("NAMESPACE " + i + ": " + namespaceElements.get(i));
            // System.out.println("NAMED DECL " + i + ": " + namedDecl.getDeclName());
            if (namespaceElements.get(i).equals(namedDecl.getDeclName())) {
                prefixElementsToRemove++;
            } else {
                break;
            }
        }

        // Remove prefix elements
        var currentNamespaceElements = SpecsCollections.subList(namespaceElements, prefixElementsToRemove);

        // No more namespaces, return current namespace elements
        if (currentNamespaceElements.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(currentNamespaceElements.stream().collect(Collectors.joining("::")));
    }

    public Optional<String> getNamespace() {

        // Qualified name has full name
        String qualifiedName = get(QUALIFIED_PREFIX);

        if (qualifiedName.isEmpty()) {
            return Optional.empty();
        }

        String currentString = qualifiedName;

        // Remove template parameters
        int templateParamStart = currentString.indexOf('<');
        if (templateParamStart != -1) {
            currentString = currentString.substring(0, templateParamStart);
        }

        String namespace = currentString;

        // Remove ::, if present
        // TODO: Unnecessary now?
        if (namespace.endsWith("::")) {
            namespace = namespace.substring(0, namespace.length() - "::".length());
        }

        return !namespace.isEmpty() ? Optional.of(namespace) : Optional.empty();
    }

    public String getCurrentQualifiedName() {
        var declName = getDeclName();

        String qualifiedPrefix = getCurrentNamespace().orElse("");

        if (qualifiedPrefix.isEmpty()) {
            return declName;
        }

        return qualifiedPrefix + "::" + declName;
    }

    public String getFullyQualifiedName() {
        var declName = getDeclName();
        return getNamespace()
                .map(namespace -> namespace + "::" + declName)
                .orElse(declName);
    }

    @Override
    public SpecsList<DataKey<?>> getSignatureKeys() {
        return super.getSignatureKeys().andAdd(DECL_NAME).andAdd(QUALIFIED_PREFIX);
    }

    public void setQualifiedName(String qualifiedName) {
        // Split the qualified name into qualified prefix and name
        String name = qualifiedName;
        String qualifiedPrefix = "";

        int lastIndexOfColon = qualifiedName.lastIndexOf("::");

        if (lastIndexOfColon != -1) {
            qualifiedPrefix = qualifiedName.substring(0, lastIndexOfColon);
            name = qualifiedName.substring(lastIndexOfColon + "::".length(), qualifiedName.length());
        }

        set(QUALIFIED_PREFIX, qualifiedPrefix);
        set(DECL_NAME, name);
    }

}

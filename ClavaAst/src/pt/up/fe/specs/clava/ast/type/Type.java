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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.Types;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgument;
import pt.up.fe.specs.clava.ast.type.enums.TypeDependency;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

/**
 * The base class of the type hierarchy.
 *
 * <p>
 * TODO: Type instances should be immutable, setters should return a copy of the node with the modified attribute.
 *
 * @author JoaoBispo
 *
 */
public abstract class Type extends ClavaNode {

    /// DATAKEYS BEGIN

    public final static DataKey<String> TYPE_AS_STRING = KeyFactory.string("typeAsString");
    // This is a static value, extracted from the original code. If it gets stale, the current policy is to
    // remove it.
    // .setDefault(null);

    public final static DataKey<TypeDependency> TYPE_DEPENDENCY = KeyFactory.enumeration("typeDependency",
            TypeDependency.class);

    public final static DataKey<Boolean> IS_VARIABLY_MODIFIED = KeyFactory.bool("isVariablyModified");

    public final static DataKey<Boolean> CONTAINS_UNEXPANDED_PARAMETER_PACK = KeyFactory
            .bool("containsUnexpandedParameterPack");

    public final static DataKey<Boolean> IS_FROM_AST = KeyFactory.bool("isFromAst");

    public final static DataKey<Optional<Type>> UNQUALIFIED_DESUGARED_TYPE = KeyFactory
            .optional("unqualifiedDesugaredType");

    /// DATAKEYS END

    public Type(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    @Override
    public Type copy() {
        return (Type) super.copy();
    }

    @Override
    public Type copy(boolean keepId) {
        return (Type) super.copy(keepId);
    }

    @Override
    public Type copy(boolean keepId, boolean copyChildren) {
        return (Type) super.copy(keepId, copyChildren);
    }

    @Override
    public <T, E extends T> ClavaNode set(DataKey<T> key, E value) {
        return set(key, value, false);
    }

    /**
     * Similar to set(), but does not change the current node, and returns a copy of the current node with the change.
     */

    public <T, E extends T> ClavaNode setCopy(DataKey<T> key, E value) {
        return set(key, value, true);
    }

    /**
     * Since the normal set of Type nodes creates a copy, this is a helper method to be used internally, in case a value
     * needs to be set in place (e.g., compatibility constructors).
     * 
     * @param key
     * @param value
     * @return
     */
    @Override
    public <T, E extends T> ClavaNode setInPlace(DataKey<T> key, E value) {
        return set(key, value, false);
    }

    protected <T, E extends T> ClavaNode set(DataKey<T> key, E value, boolean createCopy) {
        Type typeToChange = createCopy ? copy() : this;

        if (value == null) {
            typeToChange.getData().remove(key);
        } else {
            typeToChange.getData().put(key, value);
        }

        return typeToChange;
    }

    /**
     * Helper method which passes null as the name.
     */
    @Override
    public String getCode() {
        return getCode(null, null);
    }

    public String getCode(String name) {
        return getCode(null, name);
    }

    public String getCode(ClavaNode node) {
        return getCode(node, null);
    }

    /**
     * Type instances getCode() method receive a String with intermediate inner code (e.g., with the name of a
     * variable), in case this is code to declare something.
     *
     * <p>
     * Accepts null in case there is no intermediate code to use.
     * 
     * @param sourceNode
     * @param intermediateCode
     * @return
     */
    public String getCode(ClavaNode sourceNode, String intermediateCode) {
        if (intermediateCode == null) {
            return getBareType();
        }

        return getBareType() + " " + intermediateCode;
    }

    /**
     * TODO: rename to getTypeAsString
     * 
     * @return
     */
    public String getBareType() {
        return getData().get(TYPE_AS_STRING);
    }

    /**
     * TODO: rename to setTypeAsString
     * 
     * @param type
     * @return
     */
    public Type setBareType(String type) {
        return (Type) copy().set(TYPE_AS_STRING, type);
    }

    /**
     * By default, if has sugar, returns the desugared implementation of template args. Otherwise, returns an empty
     * list.
     *
     * @return
     */
    public List<String> getTemplateArgumentStrings(ClavaNode sourceNode) {
        if (!hasSugar()) {
            return Collections.emptyList();
        }

        return desugar().getTemplateArgumentStrings(sourceNode);
    }

    public List<TemplateArgument> getTemplateArguments() {
        if (!hasSugar()) {
            return Collections.emptyList();
        }

        return desugar().getTemplateArguments();
    }

    public List<Type> getTemplateArgumentTypes() {
        if (!hasSugar()) {
            return Collections.emptyList();
        }

        return desugar().getTemplateArgumentTypes();
    }

    /**
     * Sets all argument types of the template.
     * 
     * @param newTemplateArgTypes
     */
    public void setTemplateArgumentTypes(List<Type> newTemplateArgTypes) {

        // If no sugar, do nothing
        if (!hasSugar()) {
            return;
        }

        desugar().setTemplateArgumentTypes(newTemplateArgTypes);
    }

    /**
     * Sets a single argument type of the template. Cannot use an index larger than the number of original arguments.
     * 
     * @param index
     * @param newTemplateArgType
     */
    public void setTemplateArgumentType(int index, Type newTemplateArgType) {

        // If no sugar, do nothing
        if (!hasSugar()) {
            return;
        }

        desugar().setTemplateArgumentType(index, newTemplateArgType);
    }

    /**
     *
     *
     * @return true if there are template arguments, false otherise
     */
    public boolean hasTemplateArgs() {
        return !getTemplateArguments().isEmpty();
    }

    public boolean hasTemplateArgTypes() {
        return !getTemplateArgumentTypes().isEmpty();
    }

    /**
     * 
     * @return true if this type updated its template argument types, false otherwise
     */
    public boolean hasUpdatedTemplateArgTypes() {
        return desugarTry().map(Type::hasUpdatedTemplateArgTypes).orElse(false);
    }

    /**
     * 
     * @return true if the type has some kind of 'sugar' (e.g., typedef). Qualifiers (e.g., const) do not count as
     *         sugar.
     */
    public boolean hasSugar() {

        // Has desugar if calling desugar returns an node different than the current one
        return desugar() != this;
    }

    /**
     * Desugars a type until it finds a Type of the given class.
     * 
     * <p>
     * TODO: Should return self if given type if the same type as this?
     * 
     * @param typeClass
     * @return
     */
    public <T extends Type> Optional<T> desugarToTry(Class<T> typeClass) {

        // If no sugar, return
        if (!hasSugar()) {
            return Optional.empty();
        }

        Type desugared = desugar();

        if (!typeClass.isInstance(desugared)) {
            return desugared.desugarToTry(typeClass);
        }

        return Optional.of(typeClass.cast(desugared));
    }

    public <T extends Type> T desugarTo(Class<T> typeClass) {
        return desugarToTry(typeClass)
                .orElseThrow(() -> new RuntimeException("Could not desugar to type '" + typeClass + "':\n" + this));
    }

    /**
     * Single-step desugar. Returns the type itself if it does not have sugar.
     * 
     * 
     * @return
     */
    public Type desugar() {
        return get(UNQUALIFIED_DESUGARED_TYPE).orElse(this);
    }

    public final Optional<Type> desugarTry() {
        return get(UNQUALIFIED_DESUGARED_TYPE);
    }

    public final List<Type> getDesugars() {
        return getDesugars(false);
    }

    public final List<Type> getDesugars(boolean addSelf) {
        List<Type> desugars = new ArrayList<>();

        if (addSelf) {
            desugars.add(this);
        }

        Type currentDesugar = desugarTry().orElse(null);
        while (currentDesugar != null) {
            desugars.add(currentDesugar);
            currentDesugar = currentDesugar.desugarTry().orElse(null);
        }

        return desugars;
    }

    /**
     * Completely desugars the type.
     * 
     * @return
     */
    public final Type desugarAll() {
        var desugar = desugar();

        if (desugar == this) {
            return this;
        }

        return desugar.desugarAll();
    }

    /**
     * 
     * @return 0 if has sugar, -1 if it does not
     */
    //
    // public int getIndexDesugar() {
    // return -1;
    // }

    public final void setDesugar(Type desugaredType) {
        if (!hasSugar()) {
            throw new RuntimeException("Type does not have sugar:" + this);
        }

        setInPlace(UNQUALIFIED_DESUGARED_TYPE, Optional.ofNullable(desugaredType));
    }

    /**
     *
     * @return true if this type is const-qualified, false otherwise
     */
    public boolean isConst() {
        return false;
    }

    /**
     * If this type is qualified as const, removes the constness.
     */
    public void removeConst() {
        // Do nothing
    }

    public boolean isArray() {
        return false;
    }

    public <T extends Type> T to(Class<T> type) {
        return toTry(type).orElseThrow(
                () -> new RuntimeException("Could not convert type '" + getClass() + "' to '" + type + "':\n" + this));
    }

    /**
     * Goes down the type tree looking for the given type. If a node has more than one child, descending stops.
     *
     * @param type
     * @return
     */
    public <T extends Type> Optional<T> toTry(Class<T> type) {
        if (type.isInstance(this)) {
            return Optional.of(type.cast(this));
        }

        // Continue if there is one child
        if (getNumChildren() == 1) {
            return ((Type) getChild(0)).toTry(type);
        }

        if (this instanceof AttributedType) {
            return ((AttributedType) this).getModifiedType().toTry(type);
        }

        // Stop, can go no further
        return Optional.empty();
    }

    @Override
    public int hashCode() {
        return Types.hashCode(this);
    }

    @Override
    public boolean equals(Object obj) {

        if (!(obj instanceof Type)) {
            return false;
        }

        return Types.isEqual(this, ((Type) obj));
    }

    public Type unqualifiedType() {
        if (this instanceof QualType) {
            return ((QualType) this).getUnqualifiedType();
        }

        return this;
    }

    public Type normalize() {
        return this;
    }

    public List<ClavaNode> getDesugaredNodeFields() {
        List<ClavaNode> fields = new ArrayList<>();
        Type currentType = this;
        while (currentType != null) {
            for (ClavaNode desc : currentType.getNodeFields()) {
                fields.add(desc);
            }

            if (currentType.hasSugar()) {
                currentType = currentType.desugar();
            } else {
                currentType = null;
            }
        }

        return fields;
    }

    public List<Type> getTypeChildren() {
        return getNodeFields().stream()
                .filter(Type.class::isInstance)
                .map(Type.class::cast)
                .collect(Collectors.toList());
    }

    public List<Type> getTypeDescendants() {
        return getTypeDescendantsStream().collect(Collectors.toList());
    }

    public Stream<Type> getTypeDescendantsStream() {
        return getTypeDescendantsStream(new HashSet<>());
    }

    private Stream<Type> getTypeDescendantsStream(Set<Type> seenNodes) {

        // Add current node to seen nodes
        seenNodes.add(this);

        // Get types directly referenced by this Type
        return getTypeChildren().stream()
                // Do not consider types already seen
                .filter(type -> !seenNodes.contains(type))
                // For each children type, return itself and its descendants
                .flatMap(c -> c.getTypeDescendantsAndSelfStream(seenNodes));
    }

    public Stream<Type> getTypeDescendantsAndSelfStream() {
        return Stream.concat(Stream.of(this), getTypeDescendantsStream());
    }

    private Stream<Type> getTypeDescendantsAndSelfStream(Set<Type> seenNodes) {
        return Stream.concat(Stream.of(this), getTypeDescendantsStream(seenNodes));
    }

    public String toFieldTree() {
        return toFieldTree("", true, new HashSet<>());
    }

    private String toFieldTree(String prefix, boolean onlyTypes, Set<String> seenNodes) {
        StringBuilder builder = new StringBuilder();

        boolean newNode = seenNodes.add(get(ID));
        Preconditions.checkArgument(newNode);

        builder.append(prefix);
        builder.append(toString());

        builder.append("\n");

        List<DataKey<?>> keys = getAllKeysWithNodes();
        for (DataKey<?> key : keys) {
            if (!hasValue(key)) {
                continue;
            }

            List<ClavaNode> values = getClavaNode(key);
            if (values.isEmpty()) {
                continue;
            }

            Class<? extends ClavaNode> classToTest = onlyTypes ? Type.class : ClavaNode.class;
            long numberOfTypes = values.stream().filter(value -> classToTest.isInstance(value)).count();
            if (numberOfTypes != values.size()) {
                continue;
            }

            for (ClavaNode field : values) {
                // Check if repeated node
                String nodeString = seenNodes.contains(field.get(ID))
                        ? key.getName() + " -> Repeated: " + field.getClass().getSimpleName() + "(" + field.get(ID)
                                + ")\n"
                        : ((Type) field).toFieldTree(prefix + "  " + key.getName() + " -> ", onlyTypes, seenNodes);

                builder.append(nodeString);

            }
        }

        return builder.toString();
    }

    /**
     * Replaces an underlying type of this instance with new type, if it matches the old type.
     * 
     * <p>
     * Returns a copy of this type with the underlying type changed, or the type itself if no changes were made
     * 
     * @param oldType
     * @param newType
     * @return
     */
    public Type setUnderlyingType(Type oldType, Type newType) {

        // If current node is the one that is to be replaced, just return new type
        if (this.equals(oldType)) {
            return newType;
        }

        List<DataKey<Type>> underlyingTypeKeys = getUnderlyingTypeKeys();

        List<Type> previousTypes = new ArrayList<>();
        List<Type> newTypes = new ArrayList<>();
        for (DataKey<Type> underlyingTypeKey : underlyingTypeKeys) {
            // Set underlying types
            Type underlyingType = get(underlyingTypeKey);
            previousTypes.add(underlyingType);
            newTypes.add(underlyingType.setUnderlyingType(oldType, newType));
        }

        // If add newTypes are the same as the previous types, no changes were made
        boolean noChanges = true;
        for (int i = 0; i < previousTypes.size(); i++) {
            if (previousTypes.get(i) != newTypes.get(i)) {
                noChanges = false;
                break;
            }
        }

        if (noChanges) {
            return this;
        }

        // Create a copy, and set the underlying types
        Type typeCopy = copy();
        for (int i = 0; i < underlyingTypeKeys.size(); i++) {
            typeCopy.set(underlyingTypeKeys.get(i), newTypes.get(i));
        }

        return typeCopy;
    }

    protected List<DataKey<Type>> getUnderlyingTypeKeys() {
        throw new NotImplementedException(getClass());
    }

    public boolean isPointer() {
        return false;
    }

    public Type getPointeeType() {
        throw new RuntimeException("Not implemented for " + getClass());
    }

    public boolean isAuto() {
        return false;
    }

    /**
     * The bit width of this type.
     * 
     * @param node
     * @return the bit width of this type according to the Translation Unit of the given node, or -1 if the bit width is
     *         not defined
     */
    public int getBitwidth(ClavaNode node) {
        // Desugar type, if it is the same as the current one, bitwidth is not defined
        var desugaredType = desugarAll();
        if (desugaredType == this) {
            return -1;
        }

        // Get bitwidth of desugared type
        return desugaredType.getBitwidth(node);
    }

}

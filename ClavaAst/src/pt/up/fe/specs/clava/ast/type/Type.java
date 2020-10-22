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
// public abstract class Type extends ClavaNode implements StringProvider {
public abstract class Type extends ClavaNode {

    /// DATAKEYS BEGIN

    public final static DataKey<String> TYPE_AS_STRING = KeyFactory.string("typeAsString");
    // This is a static value, extracted from the original code. If it gets stale, the current policy is to
    // remove it.
    // .setDefault(null);

    public final static DataKey<Boolean> HAS_SUGAR = KeyFactory.bool("hasSugar");

    public final static DataKey<TypeDependency> TYPE_DEPENDENCY = KeyFactory.enumeration("typeDependency",
            TypeDependency.class);

    public final static DataKey<Boolean> IS_VARIABLY_MODIFIED = KeyFactory.bool("isVariablyModified");

    public final static DataKey<Boolean> CONTAINS_UNEXPANDED_PARAMETER_PACK = KeyFactory
            .bool("containsUnexpandedParameterPack");

    public final static DataKey<Boolean> IS_FROM_AST = KeyFactory.bool("isFromAst");

    // public final static DataKey<Type> UNQUALIFIED_DESUGARED_TYPE = KeyFactory.object("unqualifiedDesugaredType",
    // Type.class);
    public final static DataKey<Optional<Type>> UNQUALIFIED_DESUGARED_TYPE = KeyFactory
            .optional("unqualifiedDesugaredType");

    /// DATAKEYS END

    // private TypeData data;

    public Type(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    /**
     * For legacy.
     * 
     * @param data
     * @param info
     * @param children
     */
    // public Type(TypeData data, ClavaNodeInfo info, Collection<? extends ClavaNode> children) {
    // this(new LegacyToDataStore().setType(data).setNodeInfo(info).getData(), children);
    // /*
    // super(info, children);
    //
    // this.data = data;
    // */
    // }

    // /**
    // * @deprecated
    // * @return
    // */
    // @Deprecated
    // public TypeData getTypeData() {
    // return DataStoreToLegacy.getType(getData());
    // // if (hasDataI()) {
    // // throw new NotSupportedByDataStoreException();
    // // }
    // // return data;
    // }

    /*
    @Override
    public Type copy() {
        Type copy = (Type) super.copy();
    
        // Set app
        // copy.app = app;
    
        return copy;
    }
    */

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
        Type copy = (Type) super.copy(keepId, copyChildren);
        // Type copy = deepCopy(keepId, new HashSet<>());

        // All copy the descendants
        // deepCopy()
        // deepCopy()

        // Set app
        // copy.app = app;

        return copy;
    }

    /*
    @Override
    public Type deepCopy() {
        Set<String> seenNodes = new HashSet<>();
        Type copy = deepCopy(false, seenNodes);
    
        // System.out.println("COPIED NODES:" + seenNodes);
    
        return copy;
    }
    
    @SuppressWarnings("unchecked")
    private Type deepCopy(boolean keepId, Set<String> seenNodes) {
        // Copies the node, without children
        Type copy = (Type) copyPrivate(keepId);
    
        // Type nodes do not have children
    
        // Copy fields that are types
        for (DataKey<?> keyWithNode : getAllKeysWithNodes()) {
            if (!hasValue(keyWithNode)) {
                continue;
            }
    
            // ClavaNode keys
            if (Type.class.isAssignableFrom(keyWithNode.getValueClass())) {
                DataKey<Type> clavaNodeKey = (DataKey<Type>) keyWithNode;
                ClavaNode value = get(clavaNodeKey);
    
                if (!seenNodes.contains(value.getId())) {
                    seenNodes.add(value.getId());
                    copy.set(clavaNodeKey, ((Type) value).deepCopy(keepId, seenNodes), false);
                }
    
                continue;
            }
    
            // Optional nodes
            if (Optional.class.isAssignableFrom(keyWithNode.getValueClass())) {
                // Since this came from getKeysWithNodes(), it is guaranteed that is an Optional of ClavaNode
                DataKey<Optional<?>> optionalKey = (DataKey<Optional<?>>) keyWithNode;
                Optional<?> value = get(optionalKey);
                if (!value.isPresent()) {
                    continue;
                }
    
                Object possibleNode = value.get();
    
                if (!(possibleNode instanceof Type)) {
                    continue;
                }
    
                Type node = (Type) possibleNode;
                seenNodes.add(node.getId());
    
                copy.set(optionalKey, Optional.of(node.deepCopy(keepId, seenNodes)), false);
                continue;
            }
    
            // ClavaLog.info("Case not supported yet:" + keyWithNode);
        }
    
        // if (copy.hasSugar()) {
        // set(UNQUALIFIED_DESUGARED_TYPE, Optional.of(copy.desugar().copyDeep()));
        // }
    
        return copy;
    
    }
    */

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

        // Type typeCopy = copy();
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
        // return getCode(sourceNode, null);
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
        // throw new NotImplementedException(getClass());
        if (intermediateCode == null) {
            return getBareType();
        }

        return getBareType() + " " + intermediateCode;
        // String nameString = name == null ? "" : name;
        // return getType() + " " + nameString;
    }

    // public void setStringType(String stringType) {
    // data = new TypeData(stringType, data);
    // }

    /**
     * TODO: rename to getTypeAsString
     * 
     * @return
     */
    public String getBareType() {
        return getData().get(TYPE_AS_STRING);
        /*
        if (hasDataI()) {
            return getDataI().get(TYPE_AS_STRING);
        }
        
        return data.getBareType();
        */
    }

    /**
     * TODO: rename to setTypeAsString
     * 
     * @param type
     * @return
     */
    public Type setBareType(String type) {
        return (Type) copy().set(TYPE_AS_STRING, type);

        /*
        if (hasDataI()) {
            Type copy = copy();
            copy.getDataI().put(TYPE_AS_STRING, type);
            return copy;
        }
        
        Type copy = copy();
        copy.data.setBareType(type);
        return copy;
        */
    }

    /*
    @Override
    public String toContentString() {
        if (hasDataI()) {
            return super.toContentString();
        }
        return super.toContentString() + getCode();
    }
    */

    /**
     * By default returns false.
     *
     * @return true if type is considered anonymous (e.g., anonymous struct)
     */
    // public boolean isAnonymous() {
    // return getCode().contains("(anonymous ");
    // // return false;
    // }

    /**
     * Code for a literal constant (e.g., 1u when unsigned)
     *
     * @param constant
     * @return
     */
    /*
    public String getConstantCode(String constant) {
        return constant;
    }
    */

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
        // System.out.println("CALLING SET TEMPLATE ARGS");
        // If no sugar, do nothing
        if (!hasSugar()) {
            // System.out.println("NO TEMPLATE ARGS FOUND: " + this);
            return;
        }
        // System.out.println("TEMPLATE ARGS DESUGARING: " + this);
        desugar().setTemplateArgumentTypes(newTemplateArgTypes);

        // Types.updateSugaredType(this);
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

        // Types.updateSugaredType(this);
    }

    /**
     *
     *
     * @return true if there are template arguments, false otherise
     */
    public boolean hasTemplateArgs() {
        return !getTemplateArguments().isEmpty();
        // return !getTemplateArgumentStrings().isEmpty();
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
        // // If no sugar, return false
        // if (!hasSugar()) {
        // // System.out.println("NO SUGAR:" + this.getClass().getSimpleName());
        // return false;
        // }
        // // System.out.println("DESUGARING:" + this.getClass().getSimpleName());
        // return desugarTry().map(Type::hasUpdatedTemplateArgTypes).orElse(false);
    }

    /**
     * 
     * @return true if the type has some kind of 'sugar' (e.g., typedef). Qualifiers (e.g., const) do not count as
     *         sugar.
     */
    public boolean hasSugar() {
        // Has desugar if calling desugar returns an node different than the current one
        return desugar() != this;
        // return get(HAS_SUGAR);
        /*
        if (hasDataI()) {
            return getDataI().get(HAS_SUGAR);
        }
        
        return getTypeData().hasSugar();
        */
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

        /*
        // Check if current type is the asked type
        if (typeClass.isInstance(this)) {
        return Optional.of(typeClass.cast(this));
        }
        
        // If is sugared, desugar and call again
        if (hasSugar()) {
        return desugar().desugar(typeClass);
        }
        
        return Optional.empty();
        */
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
        // if (!hasSugar()) {
        // return this;
        // }
        // // System.out.println("SUGARED NODE: " + this);
        // return get(UNQUALIFIED_DESUGARED_TYPE);
        // return desugarImpl();
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
        // if (!hasSugar()) {
        // return this;
        // }
        //
        // return desugar().desugarAll();
        // return get(UNQUALIFIED_DESUGARED_TYPE).flatMap(Type::desugarTry).orElse(this);
        // if (!hasSugar()) {
        // return this;
        // }
        //
        // return get(UNQUALIFIED_DESUGARED_TYPE).desugar();
    }
    //
    // protected Type desugarImpl() {
    // throw new RuntimeException("deprecated, use UNQUALIFIED_DESUGARED_TYPE");
    // // return get(UNQUALIFIED_DESUGARED_TYPE);
    // // return getChild(Type.class, 0);
    //
    // /*
    // if (hasDataI()) {
    // // If has sugar, first child is always the desugared type
    // return getChild(Type.class, 0);
    // }
    // throw new NotImplementedException(getClass());
    // */
    // }

    /**
     * 
     * @return 0 if has sugar, -1 if it does not
     */

    public int getIndexDesugar() {
        return -1;
        /*
        if (hasSugar()) {
            return 0;
        } else {
            return -1;
        }
        */
    }

    public final void setDesugar(Type desugaredType) {
        if (!hasSugar()) {
            throw new RuntimeException("Type does not have sugar:" + this);
        }

        setInPlace(UNQUALIFIED_DESUGARED_TYPE, Optional.ofNullable(desugaredType));
        // set(UNQUALIFIED_DESUGARED_TYPE, desugaredType);
        // setDesugarImpl(desugaredType);
    }

    // protected void setDesugarImpl(Type desugaredType) {
    // throw new NotImplementedException(getClass());
    // }

    /*
    @Override
    public App getApp() {
        // App appNode = app.get();
        // if (appNode != null) {
        // return appNode;
        // }
    
        // If app not null, return it
        if (app != null) {
            return app;
        }
    
        // Return app of parent node
        if (hasParent()) {
            return getParent().getApp();
        }
    
        throw new RuntimeException(
                "Could not find an 'App' node associated with this type (id: " + getExtendedId().orElse(null) + ")");
    }
    */

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
        // SpecsLogs.msgWarn("Could not find type '" + type + "'");
        return Optional.empty();
    }

    @Override
    public int hashCode() {
        return Types.hashCode(this);
        // final int prime = 31;
        // int result = prime;
        // result = prime * result + ((data == null) ? 0 : data.hashCode());
        // return result;
    }

    @Override
    public boolean equals(Object obj) {

        // if (this == obj) {
        // return true;
        // }
        //
        // if (!super.equals(obj)) {
        // return false;
        // }
        //
        // if (getClass() != obj.getClass()) {
        // return false;
        // }
        // Type other = (Type) obj;

        if (!(obj instanceof Type)) {
            return false;
        }

        return Types.isEqual(this, ((Type) obj));
        // System.out.println("THIS CODE:" + getCode());
        // System.out.println("OTHER CODE:" + ((Type) obj).getCode());
        // return getCode().equals(((Type) obj).getCode());
        /*
        
        if (data == null) {
            if (other.data != null) {
                return false;
            }
        } else if (!data.equals(other.data)) {
            return false;
        }
        return true;
        */
    }

    public Type unqualifiedType() {
        if (this instanceof QualType) {
            return ((QualType) this).getUnqualifiedType();
        }

        return this;
    }

    /*
    @Override
    protected void setData(ClavaData data) {
        throw new RuntimeException(".setData() not allowed for Type nodes, they are considered immutable");
    }
    
    @Override
    public void setId(String newId) {
        throw new RuntimeException(".setId() not allowed for Type nodes, they are considered immutable");
    }
    */
    public Type normalize() {
        return this;
    }

    // /**
    // *
    // * Type nodes are treated as being "unique", and usually do not have children, other types are accessed as
    // * properties of the node. This method returns all the ClavaNode instances referenced in the properties of this
    // * class.
    // *
    // * @return
    // */
    // public List<ClavaNode> getNodes() {
    // List<DataKey<?>> keys = KEYS_WITH_NODES.get(getClass());
    // if (keys == null) {
    // keys = addKeysWithNodes(this);
    // }
    //
    // List<ClavaNode> children = new ArrayList<>();
    //
    // for (DataKey<?> key : keys) {
    // List<ClavaNode> values = getClavaNode(key);
    //
    // children.addAll(values);
    // // children.add(get(key));
    // }
    //
    // return children;
    // }
    //
    // public List<ClavaNode> getDescendantsNodes() {
    // return getDescendantsNodes(new ArrayList<>(), new HashSet<>());
    // }
    //
    // private List<ClavaNode> getDescendantsNodes(List<ClavaNode> descendants, Set<String> seenNodes) {
    // // Get nodes
    // for (ClavaNode node : getNodes()) {
    // if (seenNodes.contains(node.getId())) {
    // continue;
    // }
    //
    // // Add node
    // descendants.add(node);
    // seenNodes.add(node.getId());
    //
    // // Add node's nodes
    // node.get
    // }
    //
    // }

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
        // return getTypeChildren().stream().flatMap(c -> c.getTypeDescendantsAndSelfStream());
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
        /*
        List<ClavaNode> fields = getNodeFields();
        // System.out.println("SOURCE:" + getClass());
        // System.out.println("FIELDS:" + fields);
        
        for (ClavaNode field : fields) {
            if (onlyTypes && !(field instanceof Type)) {
                continue;
            }
        
            // Check if repeated node
            String nodeString = seenNodes.contains(field.get(ID))
                    ? "Repeated: " + field.getClass().getSimpleName() + "(" + field.get(ID) + ")"
                    : ((Type) field).toFieldTree(prefix + "  ", onlyTypes, seenNodes);
        
            builder.append(nodeString);
        }
        */
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

        // if (hasSugar()) {
        // Type desugaredType = desugar();
        // Type changedDesugaredType = desugaredType.setUnderlyingType(oldType, newType);
        //
        // if (desugaredType == changedDesugaredType) {
        // return this;
        // } else {
        // Type typeCopy = copy();
        //
        // typeCopy.setDesugar(changedDesugaredType);
        //
        // return typeCopy;
        // }
        // }

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
        // Type pointerTypeCopy = copy();
        // pointerTypeCopy.set(POINTEE_TYPE, newPointeeType);
        //
        // return pointerTypeCopy;

        // Otherwise, continue searching
        // return setUnderlyingTypeProtected(oldType, newType);
    }

    protected List<DataKey<Type>> getUnderlyingTypeKeys() {
        throw new NotImplementedException(getClass());
    }

    // protected Type setUnderlyingTypeProtected(Type oldType, Type newType) {
    // throw new NotImplementedException(getClass());
    // }

    /**
     * Used for instance, to provide signatures of the Type node.
     */
    // @Override
    // public String getString() {
    // return getCode();
    // }

    public boolean isPointer() {
        return false;
    }

    public Type getPointeeType() {
        throw new RuntimeException("Not implemented for " + getClass());
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

    /*
    private int getBitwidthPrivate(ClavaNode node, Set<Type> seenTypes) {
    
        // If current node has already been seen, bitwidth is not defined
        if (seenTypes.contains(this)) {
            return -1;
        }
    
        // Desugar type, if it is the same as the current one, bitwidth is not defined
        var desugaredType = desugarAll();
        if (desugaredType == this) {
            return -1;
        }
    
        // Get bitwidth of desugared type, while trying to avoid circular dependencies
        seenTypes.add(this);
    
        return getBitwidthPrivate(desugaredType, seenTypes);
    }
    */

}

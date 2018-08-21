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
import java.util.List;
import java.util.Optional;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.Types;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgument;
import pt.up.fe.specs.clava.ast.type.enums.TypeDependency;

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
        Type copy = (Type) super.copy(keepId);

        // Set app
        // copy.app = app;

        return copy;
    }

    /**
     * For Type nodes, this method does not change the current node, but returns a copy of the current node with the
     * change.
     */
    @Override
    public <T, E extends T> ClavaNode set(DataKey<T> key, E value) {
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
     * Type instances getCode() method receive a String (e.g., with the name of a variable), in case this is code to
     * declare something.
     *
     * <p>
     * Accepts null in case there is no name to use.
     * 
     * @param sourceNode
     *            TODO
     */
    public String getCode(ClavaNode sourceNode, String name) {
        // throw new NotImplementedException(getClass());
        if (name == null) {
            return getBareType();
        }

        return getBareType() + " " + name;
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

        Types.updateSugaredType(this);
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

        Types.updateSugaredType(this);
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
        // If no sugar, return false
        if (!hasSugar()) {
            // System.out.println("NO SUGAR:" + this.getClass().getSimpleName());
            return false;
        }
        // System.out.println("DESUGARING:" + this.getClass().getSimpleName());
        return desugar().hasUpdatedTemplateArgTypes();
    }

    /**
     * 
     * @return true if the type has some kind of 'sugar' (e.g., typedef). Qualifiers (e.g., const) do not count as
     *         sugar.
     */
    public boolean hasSugar() {
        return get(HAS_SUGAR);
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
    public final Type desugar() {
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

    /**
     * Completely desugars the type.
     * 
     * @return
     */
    public final Type desugarAll() {
        return get(UNQUALIFIED_DESUGARED_TYPE).map(Type::desugar).orElse(this);
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
}

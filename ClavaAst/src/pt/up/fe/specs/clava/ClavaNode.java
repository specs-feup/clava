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

package pt.up.fe.specs.clava;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.DataStore.DataClass;
import org.suikasoft.jOptions.DataStore.GenericDataClass;
import org.suikasoft.jOptions.DataStore.ListDataStore;
import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.storedefinition.StoreDefinition;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ast.comment.InlineComment;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.context.ClavaFactory;
import pt.up.fe.specs.clava.utils.NodeWithScope;
import pt.up.fe.specs.clava.utils.NullNode;
import pt.up.fe.specs.clava.utils.StmtWithCondition;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.classmap.ClassSet;
import pt.up.fe.specs.util.collections.SpecsList;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.providers.StringProvider;
import pt.up.fe.specs.util.system.Copyable;
import pt.up.fe.specs.util.treenode.ATreeNode;
import pt.up.fe.specs.util.utilities.BuilderWithIndentation;

public abstract class ClavaNode extends ATreeNode<ClavaNode>
        implements DataClass<ClavaNode>, Copyable<ClavaNode>, StringProvider {

    // public static boolean SKIP_EXCEPTION = false;

    /**
     * Maps Type classes to a List of DataKeys corresponding to the properties of that class that return ClavaNode
     * instances.
     */
    private static final Map<Class<? extends ClavaNode>, List<DataKey<?>>> KEYS_WITH_NODES = new ConcurrentHashMap<>();

    /// DATAKEYS BEGIN

    /**
     * Global object with information about the program.
     * 
     * TODO: Should this key be moved to another class?
     */
    public final static DataKey<ClavaContext> CONTEXT = KeyFactory.object("context", ClavaContext.class);

    /**
     * Id of the node.
     */
    public final static DataKey<String> ID = KeyFactory.string("id");

    /**
     * Location of this node. Might not be available (e.g., type nodes).
     */
    public final static DataKey<SourceRange> LOCATION = KeyFactory
            .object("location", SourceRange.class)
            .setDefault(() -> SourceRange.invalidRange());

    /**
     * If this node is part of a macro.
     */
    public final static DataKey<Boolean> IS_MACRO = KeyFactory.bool("isMacro");

    // public final static DataKey<SourceRange> SPELLING_LOCATION = KeyFactory
    // .object("spellingLocation", SourceRange.class)
    // .setDefault(() -> SourceRange.invalidRange());

    public final static DataKey<List<InlineComment>> INLINE_COMMENTS = KeyFactory
            .generic("inlineComments", (List<InlineComment>) new ArrayList<InlineComment>())
            .setDefault(() -> new ArrayList<>())
            .setCopyFunction(inlineComments -> new ArrayList<>(inlineComments));

    /**
     * True if the location of this node is valid, and is in a system header.
     */
    public final static DataKey<Boolean> IS_IN_SYSTEM_HEADER = KeyFactory.bool("isInSystemHeader");

    /**
     * True if this node was created using legacy parser classes.
     */
    // public final static DataKey<Boolean> IS_LEGACY_NODE = KeyFactory.bool("isLegacyNode");

    public final static DataKey<String> PREVIOUS_ID = KeyFactory.string("previousId");

    /// DATAKEYS END

    public static String toTree(Collection<? extends ClavaNode> nodes) {
        return nodes.stream().map(ClavaNode::toTree).collect(Collectors.joining("\n"));
    }

    private final DataStore dataI;
    private final DataClass<ClavaNode> dataClass;
    private boolean disableModification;

    public ClavaNode(DataStore dataI, Collection<? extends ClavaNode> children) {
        super(children);

        SpecsCheck.checkArgument(dataI instanceof ListDataStore,
                () -> "Expected ListDataStore, found  " + dataI.getClass());

        this.dataI = dataI;
        disableModification = false;

        // Set definition of DataStore
        // this.dataI.setDefinition(getClass());

        // To avoid implementing methods again in ClavaNode
        this.dataClass = new GenericDataClass<>(this.dataI);
    }

    protected void setDisableModification(boolean disableModification) {
        this.disableModification = disableModification;
    }

    protected String getTab() {
        return "   ";
    }

    @Override
    public String getDataClassName() {
        return dataI.getName();
    }

    public String indentCode(String code) {
        return ClavaNodes.indentCode(getTab(), code);
    }

    @Override
    public String toContentString() {
        return getData().toInlinedString();
    }

    public StoreDefinition getKeys() {
        return this.dataI.getStoreDefinition().orElseThrow(() -> new RuntimeException(""));
    }

    @Override
    public Optional<StoreDefinition> getStoreDefinition() {
        return this.dataI.getStoreDefinition();
    }

    protected static String toContentString(String previousContentString, String suffix) {

        // Use bridge if there is content and a suffix
        String bridge = !suffix.isEmpty() && !previousContentString.isEmpty() ? ", " : "";

        return previousContentString + bridge + suffix;
    }

    @Override
    public ClavaNode getThis() {
        return this;
    }

    public String getCode() {
        ClavaLog.info("getCode() not implemented for this node: " + this);
        return "<.getCode() not implemented to node " + this.getClass() + ">";
        // throw new NotImplementedException(getClass());
        // stringreturn toUnimplementedCode();
    }

    public SourceRange getLocation() {
        return get(LOCATION);
    }

    public Optional<String> getExtendedId() {
        return Optional.of(getId());
    }

    public String getId() {
        return get(ID);
    }

    /**
     * An id that is stable across compilations and is based on the position of the node in the code.
     * 
     * @return
     */
    public String getStableId() {
        throw new NotImplementedException(this);
    }

    public String getNodeIdSeparator() {
        return "->";
    }

    @Override
    public ClavaNodeIterator getChildrenIterator() {
        return new ClavaNodeIterator(this);
    }

    public App getApp() {
        return getAppTry()
                .orElseThrow(() -> new RuntimeException("App has not been set in ClavaContext: " + this));
    }

    /**
     * 
     * @return
     */
    public Optional<App> getAppTry() {
        // return Optional.of(get(CONTEXT).get(ClavaContext.APP));
        return Optional.of(get(CONTEXT).getApp());

        // ClavaNode root = this;
        // while (root.hasParent()) {
        // root = root.getParent();
        // }
        //
        // if (!(root instanceof App)) {
        // return Optional.empty();
        // }
        //
        // return Optional.of((App) root);
    }

    /**
     *
     * @return true if the node is a 'wrapper' node. Examples of wrapper nodes: WrapperStmt
     */
    public boolean isWrapper() {
        return false;
    }

    public String toJsonContents() {
        BuilderWithIndentation builder = new BuilderWithIndentation();

        // Always has a name
        builder.addLine("\"name\": \"" + getNodeName() + "\",");

        // Id
        builder.addLine("\"id\": \"" + getExtendedId().orElse("<no id>") + "\",");

        // Content
        builder.addLine("\"content\": \"" + SpecsStrings.escapeJson(toContentString()) + "\",");

        // Location
        builder.addLine("\"location\": \""
                + SpecsStrings.escapeJson(getLocationTry().map(loc -> loc.toString()).orElse("<no_location>"))
                + "\",");

        // Field above has ',' because the last field of the JSON is always the children

        return builder.toString();
    }

    public Optional<SourceRange> getLocationTry() {
        return Optional.of(get(LOCATION));
    }

    protected void throwNoCodeGeneration() {
        throw new RuntimeException(
                "Code generation not implemented because the code for this case has not been tested yet");
    }

    protected String ln() {
        return ClavaNodes.ln();
    }

    /**
     * Associates a comment to this node.
     *
     * @param inlineComment
     */
    public void associateComment(InlineComment inlineComment) {
        addInlineComment(inlineComment);
    }

    private void addInlineComment(InlineComment inlineComment) {
        Preconditions.checkArgument(!inlineComment.isStmtComment(),
                "InlineComment must not be a statement comment:" + inlineComment);

        getData().get(INLINE_COMMENTS).add(inlineComment);
    }

    /**
     * By default, copying a node creates an new, unique id for the new copy.
     */
    @Override
    public ClavaNode copy() {
        return copy(false);
    }

    /**
     * 
     * @param keepId
     *            if true, the id of the copy will be the same as the id of the original node
     * @return
     */
    // public ClavaNode copy(boolean keepId) {
    // return copy(keepId, false);
    // }

    public ClavaNode copy(boolean keepId) {
        return copy(keepId, true);
    }

    public ClavaNode copy(boolean keepId, boolean copyChildren) {

        get(CONTEXT).get(ClavaContext.METRICS).incrementNumCopies();

        // Re-implements ATreeNode copy, in order to specify if IDs should change or not
        // return super.copy();

        // TODO: Remove after legacy nodes are replaced
        // If copyPrivate() is overriden, this means it is a legacy node, and needs to call
        // the method version without arguments
        boolean overridesCopyPrivate = isCopyPrivateOverriden();
        ClavaNode newToken = overridesCopyPrivate ? copyPrivate() : copyPrivate(keepId);

        // Check new token does not have children
        if (newToken.getNumChildren() != 0) {
            throw new RuntimeException("Node '" + newToken.getClass().getSimpleName() + "' of type '"
                    + newToken.getNodeName() + "' still has children after copyPrivate(), check implementation");
        }

        if (copyChildren) {

            for (ClavaNode child : getChildren()) {
                // Copy children of token
                // ClavaNode newChildToken = deepCopy ? child.deepCopy(keepId, new HashSet<>()) : child.copy(keepId);
                ClavaNode newChildToken = child.copy(keepId);
                newToken.addChild(newChildToken);
            }
        }

        return newToken;
    }

    private boolean isCopyPrivateOverriden() {
        try {
            Class<?> copyPrivateNoArgsClass = this.getClass().getDeclaredMethod("copyPrivate").getDeclaringClass();
            // System.out.println("COPY PRIVATE DECLARING CLASS:" + copyPrivateNoArgsClass);
            return !copyPrivateNoArgsClass.equals(ClavaNode.class);
        } catch (NoSuchMethodException e) {
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Could not obtain copyPrivate() method through reflection", e);
        }
    }

    /**
     * By default, copying a node creates an new, unique id for the new copy.
     */
    @Override
    protected ClavaNode copyPrivate() {
        // return newInstance(getClass(), Collections.emptyList());
        return copyPrivate(false);
    }

    protected ClavaNode copyPrivate(boolean keepId) {
        return newInstance(keepId, getClass(), Collections.emptyList());
    }

    public boolean hasInlineComments() {
        return getInlineComments().isEmpty();
    }

    public List<InlineComment> getInlineComments() {
        return get(INLINE_COMMENTS);
    }

    /**
     *
     * @return a new list with the removed comments
     */
    public List<InlineComment> removeInlineComments() {

        List<InlineComment> comments = getInlineComments();
        List<InlineComment> copy = new ArrayList<>(comments);
        comments.clear();
        return copy;
    }

    public String getInlineCommentsCode() {
        List<InlineComment> inlineComments = getInlineComments();
        if (inlineComments.isEmpty()) {
            return "";
        }

        String code = getInlineComments().stream()
                .map(InlineComment::getCode)
                .collect(Collectors.joining(" ", " ", ""));

        return code;
    }

    /**
     * Default implementation tests the name of the node, for the prefix "CXX".
     *
     * @return true, if the current node only appears in C++ applications.
     */
    public boolean isCxxNode() {
        return getNodeName().startsWith("CXX");
    }

    public List<ClavaNode> getChildrenNormalized() {
        return getChildren().stream().map(ClavaNodes::normalize).collect(Collectors.toList());
    }

    public ClavaNode setId(String newId) {
        set(ID, newId);
        return this;
    }

    public ClavaNode setLocation(SourceRange location) {
        set(LOCATION, location);
        return this;
    }

    /**
     * 
     * @return the CompoundStmt this node belongs to, or TranslationUnit if the scope is global. A node might not have
     *         scope (e.g., if it is detached from the AST)
     */
    public Optional<ClavaNode> getScope() {
        ClavaNode currentNode = this;
        while (currentNode.hasParent()) {
            ClavaNode parent = currentNode.getParent();

            if (parent instanceof CompoundStmt || parent instanceof TranslationUnit) {
                return Optional.of(parent);
            }

            currentNode = parent;
        }

        return Optional.empty();
    }

    /**
     * Returns the DataStore associated with this node.
     * 
     * <p>
     * Generically, it is not recommended that this method is used outside of ClavaNode classes. <br>
     * Instead, the method .get() / specific setters should be used. <br>
     * However, there might be situations where access to this object is needed (e.g., library operations).
     * 
     * 
     * @return the underlying DataStore of this node
     */
    protected DataStore getData() {
        return dataI;
    }

    // public void setData(DataStore data) {
    // this.dataI.addAll(data);
    // }

    // public boolean hasDataI() {
    // return dataI != null;
    // }

    public Optional<String> getIdSuffix() {
        if (!getExtendedId().isPresent()) {
            return Optional.empty();
        }

        String id = getExtendedId().get();

        if (id == null) {
            return Optional.empty();
        }

        int startIndex = id.lastIndexOf('_');
        if (startIndex == -1) {
            SpecsLogs.warn("Could not find '_' in the id: " + id);
            return Optional.empty();
        }

        return Optional.of(id.substring(startIndex));
    }

    public boolean isNullNode() {
        return this instanceof NullNode;
    }

    public String toTree() {
        return super.toString();
    }

    @Override
    public String toString() {
        return toContentString();
    }

    /**
     * 
     * @return a ClavaFactory where the builders will use the data of this node as the base for new nodes.
     */
    public ClavaFactory getFactoryWithNode() {
        return new ClavaFactory(get(CONTEXT), dataI);
    }

    public ClavaFactory getFactory() {
        return get(CONTEXT).get(ClavaContext.FACTORY);
    }

    /**
     * General getter for ClavaNode fields.
     * 
     * @param key
     * @return
     */
    @Override
    public <T> T get(DataKey<T> key) {
        try {
            T value = dataI.get(key);

            // // Ignore ImplicitCasts
            // if (value instanceof ImplicitCastExpr) {
            // return (T) ((ImplicitCastExpr) value).getSubExpr();
            // }

            return value;
        } catch (Exception e) {
            throw new RuntimeException(
                    "Problem while accessing attribute '" + key + "' in ClavaNode: " + this.getNodeName(), e);
        }

    }

    /**
     * Tries to return a value from the DataStore.
     * 
     * <p>
     * Does not use default values. If the key is not in the map, or there is no value mapped to the given key, returns
     * an empty Optional.
     * 
     * @param key
     * @return
     */
    public <T> Optional<T> getTry(DataKey<T> key) {
        return dataI.getTry(key);
    }

    /**
     * Generic method for setting values.
     * 
     * <p>
     * If null is passed as value, removes current value associated with given key.
     * 
     * @param key
     * @param value
     */
    @Override
    public <T, E extends T> ClavaNode set(DataKey<T> key, E value) {
        if (disableModification) {
            SpecsLogs.warn("Could not perform set: this node is a view, modifications are disabled");
            return this;
        }

        // If value is null, remove value, if present
        if (value == null) {
            if (dataI.hasValue(key)) {
                dataI.remove(key);
            }

            return this;
        }

        dataI.put(key, value);

        return this;
    }

    @Override
    public ClavaNode set(ClavaNode instance) {
        return dataClass.set(instance);
    }

    /**
     * 
     * @param key
     * @return true, if it contains a non-null value for the given key, not considering default values
     */
    @Override
    public <T> boolean hasValue(DataKey<T> key) {
        return dataI.hasValue(key);
    }

    @Override
    public Collection<DataKey<?>> getDataKeysWithValues() {
        return dataClass.getDataKeysWithValues();
    }

    public ClavaContext getContext() {
        return get(CONTEXT);
    }

    /**
     * Helper method which disables sharing the same data object.
     * 
     * @param keepId
     * @param nodeClass
     * @param children
     * @return
     */
    public <T extends ClavaNode> T newInstance(boolean keepId, Class<T> nodeClass, List<ClavaNode> children) {
        return newInstance(keepId, false, nodeClass, children);
    }

    /**
     * Creates a new node using the same data as this node.
     * 
     * @param nodeClass
     * @param children
     * @return
     */
    public <T extends ClavaNode> T newInstance(boolean keepId, boolean shareData, Class<T> nodeClass,
            List<ClavaNode> children) {

        // System.out.println("NODE CLASS: " + nodeClass);
        // if (nodeClass.getSimpleName().equals("NullDecl")) {
        // throw new RuntimeException("STOP");
        // }
        // DataStore newDataStore = dataI.copy();

        // TODO: CHECK IF CANNOT SIMPLY COPY DATA! NEEDS TO AT LEAST SPECIFY DEFINITION OF NEW CLASS
        // SpecsCheck.checkArgument(nodeClass.isInstance(this), () -> "Expected class to be of same instance");
        // Use the same data store
        DataStore newDataStore = shareData ? dataI : dataI.copy();
        // DataStore newDataStore = newDataStore(shareData, nodeClass);

        // Set id
        if (!keepId) {
            String previousId = newDataStore.get(ID);
            String newId = get(CONTEXT).get(ClavaContext.ID_GENERATOR).next("from" + getClass().getSimpleName() + "_");

            newDataStore.put(ID, newId);
            newDataStore.put(PREVIOUS_ID, previousId);

            // if (!SKIP_EXCEPTION && newId.startsWith("from")) {
            // throw new RuntimeException("Copying node:" + this);
            // }

            // if (newId.equals("fromVarDecl_1")) {
            // throw new RuntimeException();
            // // System.out.println("PREVIOUS DATA:::\n" + dataI);
            // // System.out.println("DATA:::\n" + newDataStore);
            // }
        }

        try {
            Constructor<? extends ClavaNode> constructorMethod = nodeClass.getConstructor(DataStore.class,
                    Collection.class);

            try {
                // System.out.println("NODE CLASS: " + nodeClass);
                // System.out.println("CONSTRUCTOR METHOD: " + constructorMethod);
                return nodeClass.cast(constructorMethod.newInstance(newDataStore, children));
            } catch (Exception e) {
                throw new RuntimeException("Could not call constructor for ClavaNode", e);
            }

        } catch (Exception e) {
            // throw new RuntimeException("Could not create constructor for ClavaNode:" + e.getMessage());
            throw new RuntimeException("Could not create constructor for ClavaNode", e);
        }
    }

    // private <T extends ClavaNode> DataStore newDataStore(boolean shareData, Class<T> nodeClass) {
    //
    // if (shareData) {
    // return dataI;
    // }
    //
    // if (!nodeClass.isInstance(this)) {
    // return DataStore.newInstance(StoreDefinitions.fromInterface(nodeClass), true);
    // }
    //
    // return dataI.copy();
    // }

    // /**
    // * Legacy support.
    // *
    // * When all types have DataStore, we can use node.getFactoryWithNode()
    // *
    // * @deprecated getFactoryWithNode()
    // * @param node
    // */
    // @Deprecated
    // public void setNodeData(ClavaNode node) {
    // node.setId(getExtendedId().get());
    // node.setLocation(getLocation());
    // }

    // public ClavaNode setIsLegacyNode(boolean isLegacyNode) {
    // getData().set(IS_LEGACY_NODE, isLegacyNode);
    // return this;
    // }

    public <T extends ClavaNode> Optional<T> getOptionalChild(Class<T> nodeClass, int index) {
        ClavaNode child = getChild(index);

        if (child instanceof NullNode) {
            return Optional.empty();
        }

        return Optional.of(nodeClass.cast(child));
    }

    /**
     * All keys that can potentially have ClavaNodes.
     * 
     * @return
     */
    public List<DataKey<?>> getAllKeysWithNodes() {
        List<DataKey<?>> keys = KEYS_WITH_NODES.get(getClass());
        if (keys == null) {
            keys = addKeysWithNodes(this);
        }

        return keys;
    }

    public List<ClavaNode> getNodeFields() {
        List<DataKey<?>> keys = getAllKeysWithNodes();

        List<ClavaNode> children = new ArrayList<>();

        for (DataKey<?> key : keys) {
            if (!hasValue(key)) {
                continue;
            }
            List<ClavaNode> values = getClavaNode(key);

            // if (values.size() != 0) {
            // System.out.println("KEY '" + key + "' ADDING VALUES: " + values);
            // }
            children.addAll(values);
            // children.add(get(key));
        }

        return children;
    }

    public List<ClavaNode> getNodeFieldsRecursive() {
        return getNodeFieldsRecursive(new ArrayList<>(), new HashSet<>());
    }

    private List<ClavaNode> getNodeFieldsRecursive(List<ClavaNode> descendants, Set<String> seenNodes) {
        // Get nodes
        for (ClavaNode node : getNodeFields()) {
            if (seenNodes.contains(node.getId())) {
                continue;
            }

            // Add node
            descendants.add(node);
            seenNodes.add(node.getId());

            // Add node's nodes
            node.getNodeFieldsRecursive(descendants, seenNodes);
        }

        return descendants;
    }

    public List<ClavaNode> getClavaNode(DataKey<?> key) {
        // ClavaNode keys
        if (ClavaNode.class.isAssignableFrom(key.getValueClass())) {
            return Arrays.asList((ClavaNode) get(key));
        }

        // Optional nodes
        if (Optional.class.isAssignableFrom(key.getValueClass())) {
            Optional<?> optionalValue = (Optional<?>) get(key);

            return optionalValue.filter(value -> value instanceof ClavaNode)
                    .map(node -> Arrays.asList((ClavaNode) node))
                    .orElse(Collections.emptyList());
        }

        // List of nodes
        if (List.class.isAssignableFrom(key.getValueClass())) {
            List<?> values = (List<?>) get(key);

            if (values.isEmpty()) {
                return Collections.emptyList();
            }

            // Only testing first, cast tests all elements
            if (values.get(0) instanceof ClavaNode) {
                return SpecsCollections.cast(values, ClavaNode.class);
            }
        }

        return Collections.emptyList();
    }

    private static List<DataKey<?>> addKeysWithNodes(ClavaNode node) {
        List<DataKey<?>> keysWithNodes = new ArrayList<>();

        // Get all the keys that map to a ClavaNode
        for (DataKey<?> key : node.getKeys().getKeys()) {

            // ClavaNode keys
            if (ClavaNode.class.isAssignableFrom(key.getValueClass())) {
                keysWithNodes.add(key);
                continue;
            }

            // Optional nodes
            if (Optional.class.isAssignableFrom(key.getValueClass())) {
                keysWithNodes.add(key);
                continue;
            }

            // List of nodes
            if (List.class.isAssignableFrom(key.getValueClass())) {
                keysWithNodes.add(key);
                continue;
            }

        }

        // Add to map
        KEYS_WITH_NODES.put(node.getClass(), keysWithNodes);

        return keysWithNodes;
    }

    public <T extends ClavaNode> List<T> getDescendantsAndFields(Class<T> aClass) {
        return getDescendantsAndFields().stream()
                .filter(aClass::isInstance)
                .map(aClass::cast)
                .collect(Collectors.toList());
    }

    public List<ClavaNode> getDescendantsAndFields() {
        return getDescendantsAndFields(new ArrayList<>(), new HashSet<>());
    }

    private List<ClavaNode> getDescendantsAndFields(List<ClavaNode> nodes, Set<String> seenNodes) {
        // List<ClavaNode> nodes = new ArrayList<>();
        //
        // Set<String> seenNodes = new HashSet<>();

        // Fields of node
        for (ClavaNode node : getNodeFields()) {
            if (seenNodes.contains(node.getId())) {
                continue;
            }

            seenNodes.add(node.getId());
            nodes.add(node);

            node.getDescendantsAndFields(nodes, seenNodes);
        }

        // Children
        for (ClavaNode child : getChildren()) {
            if (seenNodes.contains(child.getId())) {
                continue;
            }

            seenNodes.add(child.getId());
            nodes.add(child);

            child.getDescendantsAndFields(nodes, seenNodes);
        }

        // // Descendants
        // for (ClavaNode descendant : getDescendants()) {
        // // Add descendant
        // if (!seenNodes.contains(descendant.getId())) {
        // seenNodes.add(descendant.getId());
        // nodes.add(descendant);
        // }
        //
        // // Add descendant fields
        // for (ClavaNode descendantField : descendant.getNodeFieldsRecursive()) {
        // // Add descendant
        // if (!seenNodes.contains(descendantField.getId())) {
        // seenNodes.add(descendantField.getId());
        // nodes.add(descendantField);
        // }
        // }
        //
        // }

        return nodes;
    }

    /**
     * Copy the node, including nodes in fields.
     * 
     * <p>
     * This function is not recursive, it only copies the first level of nodes it has in the fields (nodes in the fields
     * of the fields are not copied).
     * 
     * @return
     */

    public ClavaNode deepCopy() {
        return deepCopy(false);
    }

    private ClavaNode deepCopy(boolean keepId) {
        return deepCopy(keepId, new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    private ClavaNode deepCopy(boolean keepId, Map<String, ClavaNode> copiedNodes) {

        // Check if node was already copied
        ClavaNode copy = copiedNodes.get(getId());
        if (copy != null) {
            return copy;
        }

        // Simple copy of the node, without children
        // copy = copyPrivate(keepId);
        copy = copy(keepId, false);

        // Add to replacement map
        copiedNodes.put(getId(), copy);

        // Deep copy of the children
        for (ClavaNode child : getChildren()) {
            // Copy children of node
            ClavaNode newChildToken = child.deepCopy(keepId, copiedNodes);
            copy.addChild(newChildToken);
        }

        // Simple copy of fields (1st level only), unless it is a type node, in which case performs a deep copy
        for (DataKey<?> keyWithNode : getAllKeysWithNodes()) {
            if (!hasValue(keyWithNode)) {
                continue;
            }

            // ClavaNode keys
            if (ClavaNode.class.isAssignableFrom(keyWithNode.getValueClass())) {
                DataKey<ClavaNode> clavaNodeKey = (DataKey<ClavaNode>) keyWithNode;
                ClavaNode value = get(clavaNodeKey);

                copy.set(clavaNodeKey, copyNodeValue(value, keepId, copiedNodes));
                // copy.set(clavaNodeKey, value.copy(keepId));
                continue;
            }

            // Optional nodes
            if (Optional.class.isAssignableFrom(keyWithNode.getValueClass())) {
                DataKey<Optional<?>> optionalKey = (DataKey<Optional<?>>) keyWithNode;

                // Since this came from getKeysWithNodes(), it is guaranteed that is has an Optional value that we can
                // test for ClavaNode
                Optional<?> value = get(optionalKey);
                if (!value.isPresent()) {
                    continue;
                }

                Object possibleNode = value.get();

                if (!(possibleNode instanceof ClavaNode)) {
                    continue;
                }

                ClavaNode node = (ClavaNode) possibleNode;

                // copy.set(optionalKey, Optional.of(node.copy(keepId)));
                copy.set(optionalKey, Optional.of(copyNodeValue(node, keepId, copiedNodes)));
                continue;
            }

            // List nodes
            if (List.class.isAssignableFrom(keyWithNode.getValueClass())) {
                DataKey<List<?>> listKey = (DataKey<List<?>>) keyWithNode;

                // Since this came from getKeysWithNodes(), it is guaranteed that is has a List value that we can
                // test for ClavaNodes
                List<?> list = get(listKey);

                if (list.isEmpty()) {
                    // copy.set(listKey, new ArrayList<>());
                    continue;
                }

                if (!(list.get(0) instanceof ClavaNode)) {
                    continue;
                }

                List<ClavaNode> newList = new ArrayList<>(list.size());

                for (Object listValue : list) {
                    // newList.add(((ClavaNode) listValue).copy());
                    newList.add((copyNodeValue((ClavaNode) listValue, false, copiedNodes)));
                }

                copy.set(listKey, newList);
                continue;
            }

            // ClavaLog.info("Case not supported yet:" + keyWithNode);
        }

        // if (copy.hasSugar()) {
        // set(UNQUALIFIED_DESUGARED_TYPE, Optional.of(copy.desugar().copyDeep()));
        // }

        return copy;

    }
    /*
    @SuppressWarnings("unchecked")
    private ClavaNode deepCopy(boolean keepId, Set<String> seenNodes) {
        // return copy(keepId);
    
        // Copy node itself
        // ClavaNode copy = copy(keepId, true);
    
        // Copies the node, without children
        ClavaNode copy = copyPrivate(keepId);
    
        for (ClavaNode child : getChildren()) {
            // Copy children of token
            // ClavaNode newChildToken = deepCopy ? child.deepCopy(keepId, new HashSet<>()) : child.copy(keepId);
            ClavaNode newChildToken = child.deepCopy(keepId, seenNodes);
            copy.addChild(newChildToken);
        }
    
        // System.out.println("DEEP COPYING " + this);
        // if (this instanceof VariableArrayType) {
        // System.out.println("EXPR:" + get(VariableArrayType.SIZE_EXPR));
        // }
        // Copy fields
        for (DataKey<?> keyWithNode : getAllKeysWithNodes()) {
            if (!hasValue(keyWithNode)) {
                continue;
            }
    
            // ClavaNode keys
            if (ClavaNode.class.isAssignableFrom(keyWithNode.getValueClass())) {
                DataKey<ClavaNode> clavaNodeKey = (DataKey<ClavaNode>) keyWithNode;
                ClavaNode value = get(clavaNodeKey);
                if (!seenNodes.contains(value.getId())) {
                    seenNodes.add(value.getId());
                    set(clavaNodeKey, value.deepCopy(keepId, seenNodes));
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
    
                if (!(possibleNode instanceof ClavaNode)) {
                    continue;
                }
    
                ClavaNode node = (ClavaNode) possibleNode;
                seenNodes.add(node.getId());
    
                set(optionalKey, Optional.of(node.deepCopy(keepId, seenNodes)));
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
    //
    // @SuppressWarnings("unchecked")
    // public List<ClavaNode> copyNodeField(DataKey<?> keyWithNode) {
    //
    // if (!hasValue(keyWithNode)) {
    // return Collections.emptyList();
    // }
    //
    // // ClavaNode keys
    // if (ClavaNode.class.isAssignableFrom(keyWithNode.getValueClass())) {
    // DataKey<ClavaNode> clavaNodeKey = (DataKey<ClavaNode>) keyWithNode;
    // ClavaNode value = get(clavaNodeKey);
    // ClavaNode copy = value.copy();
    // set(clavaNodeKey, copy);
    // return Arrays.asList(copy);
    // }
    //
    // // Optional nodes
    // if (Optional.class.isAssignableFrom(keyWithNode.getValueClass())) {
    // DataKey<Optional<?>> optionalKey = (DataKey<Optional<?>>) keyWithNode;
    // Optional<?> value = get(optionalKey);
    // if (!value.isPresent()) {
    // return Collections.emptyList();
    // }
    //
    // Object possibleNode = value.get();
    //
    // if (!(possibleNode instanceof ClavaNode)) {
    // return Collections.emptyList();
    // }
    //
    // ClavaNode node = (ClavaNode) possibleNode;
    // ClavaNode copy = node.copy();
    // set(optionalKey, Optional.of(copy));
    // return Arrays.asList(copy);
    // }
    //
    // return Collections.emptyList();
    //
    // // ClavaLog.info("Case not supported yet:" + keyWithNode);
    //
    // }

    /**
     * Deep copy node if it is a Type node, simple copy otherwise.
     * 
     * @param value
     * @param keepId
     * @param copiedNodes
     * @return
     */
    private ClavaNode copyNodeValue(ClavaNode value, boolean keepId, Map<String, ClavaNode> copiedNodes) {
        return value instanceof Type ? value.deepCopy(keepId, copiedNodes) : value.copy(keepId);
    }

    @SuppressWarnings("unchecked")
    public void replaceNodeField(DataKey<?> keyWithNode, List<ClavaNode> newValue) {

        if (!hasValue(keyWithNode)) {
            // System.out.println("DOES NOT HAVE VALUE");
            return;
        }

        // ClavaNode keys
        if (ClavaNode.class.isAssignableFrom(keyWithNode.getValueClass())) {
            DataKey<ClavaNode> clavaNodeKey = (DataKey<ClavaNode>) keyWithNode;
            set(clavaNodeKey, newValue.get(0));
            // System.out.println("SETTING NEW VALUE:" + newValue.get(0).getCode());
            // ClavaNode value = get(clavaNodeKey);
            // ClavaNode copy = value.copy();
            // set(clavaNodeKey, copy);
            //
            // return Arrays.asList(copy);
        }

        // Optional nodes
        if (Optional.class.isAssignableFrom(keyWithNode.getValueClass())) {
            // System.out.println("OPTIONAL KEY");
            DataKey<Optional<?>> optionalKey = (DataKey<Optional<?>>) keyWithNode;
            Optional<?> value = get(optionalKey);
            if (!value.isPresent()) {
                // System.out.println("NO VALUE");
                return;
            }

            Object possibleNode = value.get();

            if (!(possibleNode instanceof ClavaNode)) {
                // System.out.println("NOT A CLAVANODE");
                return;
            }

            setInPlace(optionalKey, Optional.of(newValue.get(0)));
            // System.out.println("SETTING OPTIONAL");
            // ClavaNode copy = node.copy();
            // set(optionalKey, Optional.of(copy));
            // return Arrays.asList(copy);
        }

        // return Collections.emptyList();

        // ClavaLog.info("Case not supported yet:" + keyWithNode);

    }

    /**
     * @deprecated
     * @param key
     * @param value
     * @return
     */
    @Deprecated
    public <T, E extends T> ClavaNode setInPlace(DataKey<T> key, E value) {
        return set(key, value);
    }

    /**
     * Keys that currently have nodes assigned.
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public List<DataKey<?>> getKeysWithNodes() {

        List<DataKey<?>> keys = new ArrayList<>();

        for (DataKey<?> key : getAllKeysWithNodes()) {

            if (!hasValue(key)) {
                continue;
            }

            // ClavaNode keys
            if (ClavaNode.class.isAssignableFrom(key.getValueClass())) {
                keys.add(key);
                continue;
            }

            // Optional nodes
            if (Optional.class.isAssignableFrom(key.getValueClass())) {
                DataKey<Optional<?>> optionalKey = (DataKey<Optional<?>>) key;
                Optional<?> value = get(optionalKey);
                if (!value.isPresent()) {
                    continue;
                }

                Object possibleNode = value.get();

                if (!(possibleNode instanceof ClavaNode)) {
                    continue;
                }

                keys.add(key);
                continue;
            }

            if (List.class.isAssignableFrom(key.getValueClass())) {
                DataKey<List<?>> listKey = (DataKey<List<?>>) key;
                List<?> list = get(listKey);
                if (list.isEmpty()) {
                    continue;
                }

                // Check if elements of the list are ClavaNodes
                boolean clavaNodeList = list.stream()
                        .filter(elem -> elem instanceof ClavaNode)
                        .count() == list.size();

                if (!clavaNodeList) {
                    continue;
                }

                keys.add(key);
                continue;
            }
        }

        return keys;
        // ClavaLog.info("Case not supported yet:" + keyWithNode);

    }

    // @SuppressWarnings("unchecked")
    public List<ClavaNode> getNodes(DataKey<?> keyWithNodes) {

        List<ClavaNode> nodes = new ArrayList<>();

        for (DataKey<?> key : getKeysWithNodes()) {

            Object value = get(key);

            if (value instanceof ClavaNode) {
                nodes.add((ClavaNode) value);
                continue;
            }

            if (value instanceof Optional) {
                nodes.add((ClavaNode) ((Optional<?>) value).get());
                continue;
            }

            // ClavaLog.info("Case not supported yet:" + keyWithNode);

        }

        return nodes;

    }

    /**
     * A String that uniquely identifies the contents of this node.
     * 
     * @return
     */
    public String getNodeSignature() {

        StringBuilder signature = new StringBuilder();

        signature.append(getClass().getSimpleName());

        for (DataKey<?> key : getSignatureKeys()) {
            // if (getClass() == CXXConstructExpr.class) {
            // System.out.println("CURRENT SIG:" + signature);
            // System.out.println("CURRENT KEY:" + key);
            // System.out.println("KEY VALUE:" + get(key).toString());
            // }
            Object value = get(key);
            String valueSig = value instanceof StringProvider ? ((StringProvider) value).getString() : value.toString();
            signature.append("_").append(valueSig);
            // if (getClass() == CXXConstructExpr.class) {
            // System.out.println("SIG AFTER:" + signature);
            // }
        }

        for (String customString : getSignatureCustomStrings()) {
            signature.append("_").append(customString);
        }

        return signature.toString();

        // return getClass().getSimpleName() + "_" + getLocation();
    }

    public SpecsList<DataKey<?>> getSignatureKeys() {
        return SpecsList.convert(new ArrayList<DataKey<?>>()).andAdd(LOCATION).andAdd(PREVIOUS_ID);
        // List<DataKey<?>> signatureKeys = new ArrayList<>();
        // signatureKeys.add(LOCATION);
        // return signatureKeys;
    }

    /**
     * By default, returns empty.
     * 
     * @return
     */
    public SpecsList<String> getSignatureCustomStrings() {
        return SpecsList.newInstance(String.class);
    }

    @Override
    public String getString() {
        return getClass().getSimpleName() + " (" + getId() + ")";
    }

    /**
     * If node is inside a StmtWithConditin (but not in a CompoundStmt), returns the statement.
     * 
     * @return
     */
    public Optional<Stmt> getStmtWithConditionAncestor() {
        // Check if inside condition of while, for or if
        ClavaNode node = getAscendantsStream()
                .filter(ascendant -> ascendant instanceof StmtWithCondition || ascendant instanceof CompoundStmt)
                .findFirst()
                .orElse(null);

        if (node == null || node instanceof CompoundStmt) {
            return Optional.empty();
        }

        return Optional.of((Stmt) node);
    }

    // public <T> copyField(DataKey<T> key) {
    // Type pointeeCopy = type.get(PointerType.POINTEE_TYPE).copy();
    // type.set(PointerType.POINTEE_TYPE, pointeeCopy);
    // }

    /**
     * 
     * @return the children inside the scope declared by this node (e.g., body of a loop), or empty list if node does
     *         not have a scope
     */
    public List<ClavaNode> getScopeChildren() {

        if (!(this instanceof NodeWithScope)) {
            return Collections.emptyList();
        }

        return ((NodeWithScope) this).getNodeScope()
                .map(scope -> scope.getChildren())
                .orElse(Collections.emptyList());
    }

    /**
     * 
     * 
     * @param classes
     * @return a list with the ancestors of the current node of the given classes. The top-most ancestor appears first
     *         in the list
     */
    public List<ClavaNode> getAncestors(Collection<Class<? extends ClavaNode>> classes) {
        var currentNode = this;
        ClassSet<ClavaNode> classSet = new ClassSet<>();
        classSet.addAll(classes);

        List<ClavaNode> ancestors = new ArrayList<>();
        while (currentNode != null) {
            var parent = currentNode.getParent();

            if (parent != null && classSet.contains(parent.getClass())) {
                ancestors.add(0, parent);
            }

            currentNode = parent;
        }

        return ancestors;
    }
}

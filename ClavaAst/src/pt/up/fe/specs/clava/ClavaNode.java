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
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.DataStore.DataClass;
import org.suikasoft.jOptions.DataStore.GenericDataClass;
import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.storedefinition.StoreDefinition;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.DataStoreToLegacy;
import pt.up.fe.specs.clava.ast.LegacyToDataStore;
import pt.up.fe.specs.clava.ast.comment.InlineComment;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.context.ClavaFactory;
import pt.up.fe.specs.clava.utils.NullNode;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.system.Copyable;
import pt.up.fe.specs.util.treenode.ATreeNode;
import pt.up.fe.specs.util.utilities.BuilderWithIndentation;

public abstract class ClavaNode extends ATreeNode<ClavaNode> implements DataClass<ClavaNode>, Copyable<ClavaNode> {

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

    public final static DataKey<SourceRange> SPELLING_LOCATION = KeyFactory
            .object("spellingLocation", SourceRange.class)
            .setDefault(() -> SourceRange.invalidRange());

    public final static DataKey<List<InlineComment>> INLINE_COMMENTS = KeyFactory
            .generic("inlineComments", (List<InlineComment>) new ArrayList<InlineComment>())
            .setDefault(() -> new ArrayList<>())
            .setCopyFunction(inlineComments -> new ArrayList<>(inlineComments));

    /**
     * True if this node was created using legacy parser classes.
     */
    public final static DataKey<Boolean> IS_LEGACY_NODE = KeyFactory.bool("isLegacyNode");

    /// DATAKEYS END

    public static String toTree(Collection<? extends ClavaNode> nodes) {
        return nodes.stream().map(ClavaNode::toTree).collect(Collectors.joining("\n"));
    }

    private final DataStore dataI;
    private final DataClass<ClavaNode> dataClass;
    private boolean disableModification;

    public ClavaNode(ClavaNodeInfo nodeInfo, Collection<? extends ClavaNode> children) {
        this(new LegacyToDataStore().setNodeInfo(nodeInfo).getData(), children);
    }

    public ClavaNode(DataStore dataI, Collection<? extends ClavaNode> children) {
        super(children);

        this.dataI = dataI;
        disableModification = false;

        // Set definition of DataStore
        this.dataI.setDefinition(getClass());

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
        throw new NotImplementedException(getClass());
        // stringreturn toUnimplementedCode();
    }

    public SourceRange getLocation() {
        return get(LOCATION);
    }

    /**
     * TODO: Make method private when all accesses are "fixed"
     * 
     * @deprecated use .getExtendedId() instead
     * @return
     */
    @Deprecated
    public Optional<ClavaId> getClavaId() {
        return getInfo().getId();
    }

    public Optional<String> getExtendedId() {
        return Optional.of(getId());
    }

    public String getId() {
        return get(ID);
    }

    /**
     * TODO: Make method protected when all accesses are "fixed"
     * 
     * @deprecated
     * @return
     */
    @Deprecated
    public ClavaNodeInfo getInfo() {
        return DataStoreToLegacy.getNodeInfo(getData());
    }

    @Override
    public ClavaNodeIterator getChildrenIterator() {
        return new ClavaNodeIterator(this);
    }

    public App getApp() {
        return getAppTry()
                .orElseThrow(() -> new RuntimeException("Node does not have a parent: " + this));
    }

    public Optional<App> getAppTry() {
        ClavaNode root = this;
        while (root.hasParent()) {
            root = root.getParent();
        }

        if (!(root instanceof App)) {
            return Optional.empty();
        }

        return Optional.of((App) root);
    }

    /**
     * Helper method which generates template code for nodes that have not implemented yet getCode().
     *
     * @param node
     * @return
     */
    private String toUnimplementedCode() {
        return ClavaNodes.toCode("NOT IMPLEMENTED: " + getClass().getSimpleName(), this);
    }

    protected static List<ClavaNode> sanitize(ClavaNodeInfo info, ClavaNode... nodes) {
        return sanitize(info, Arrays.asList(nodes));
    }

    /**
     * Replaces null elements with NullNodes with the given info.
     *
     * @param info
     * @param nodes
     * @return
     */
    protected static List<ClavaNode> sanitize(ClavaNodeInfo info, Collection<? extends ClavaNode> nodes) {
        return nodes.stream()
                .map(node -> node == null ? ClavaNodeFactory.nullNode(info) : node)
                .collect(Collectors.toList());
    }

    /**
     * Helper method that returns a child that can be a NullNode. Ignores WrapperStmt, Pragma and Comment nodes.
     *
     * @param index
     * @param castTo
     * @return
     */
    protected <T extends ClavaNode> Optional<T> getNullable(int index, Class<T> castTo) {
        // Get child
        ClavaNode child = ClavaNodes.getChild(this, index);

        // If NullNode return empty Optional, otherwise cast and return optional
        return child instanceof pt.up.fe.specs.clava.ast.extra.NullNode ? Optional.empty()
                : Optional.of(castTo.cast(child));
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return this == obj;
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

    /**
     * @deprecated
     * @param expr
     * @return
     */
    @Deprecated
    protected static Expr nullable(Expr expr) {
        return expr == null ? LegacyToDataStore.getFactory().nullExpr() : expr;
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
    public ClavaNode copy(boolean keepId) {

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

        for (ClavaNode child : getChildren()) {
            // Copy children of token
            ClavaNode newChildToken = child.copy(keepId);

            newToken.addChild(newChildToken);
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
            SpecsLogs.msgWarn("Could not find '_' in the id: " + id);
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
            return dataI.get(key);
        } catch (Exception e) {
            throw new RuntimeException("Problem while accessing attribute '" + key + "' in ClavaNode: " + this, e);
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
            SpecsLogs.msgWarn("Could not perform set: this node is a view, modifications are disabled");
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
    public Collection<DataKey<?>> keysWithValues() {
        return dataClass.keysWithValues();
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

        // DataStore newDataStore = dataI.copy();

        // Use the same datastore
        DataStore newDataStore = shareData ? dataI : dataI.copy();

        // Set id
        if (!keepId) {
            String newId = get(CONTEXT).get(ClavaContext.ID_GENERATOR).next("from" + getClass().getSimpleName() + "_");
            newDataStore.put(ID, newId);
        }

        try {
            Constructor<? extends ClavaNode> constructorMethod = nodeClass.getConstructor(DataStore.class,
                    Collection.class);

            try {
                return nodeClass.cast(constructorMethod.newInstance(newDataStore, children));
            } catch (Exception e) {
                throw new RuntimeException("Could not call constructor for ClavaNode", e);
            }

        } catch (Exception e) {
            // throw new RuntimeException("Could not create constructor for ClavaNode:" + e.getMessage());
            throw new RuntimeException("Could not create constructor for ClavaNode", e);
        }
    }

    /**
     * Legacy support.
     * 
     * When all types have DataStore, we can use node.getFactoryWithNode()
     * 
     * @deprecated getFactoryWithNode()
     * @param node
     */
    @Deprecated
    public void setNodeData(ClavaNode node) {
        node.setId(getExtendedId().get());
        node.setLocation(getLocation());
    }

    public ClavaNode setIsLegacyNode(boolean isLegacyNode) {
        getData().set(IS_LEGACY_NODE, isLegacyNode);
        return this;
    }

}

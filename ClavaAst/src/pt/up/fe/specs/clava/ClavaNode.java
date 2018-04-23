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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ast.ClavaData;
import pt.up.fe.specs.clava.ast.ClavaNodeConstructors;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.comment.InlineComment;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.NullExpr;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.NullNode;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.treenode.ATreeNode;
import pt.up.fe.specs.util.utilities.BuilderWithIndentation;

public abstract class ClavaNode extends ATreeNode<ClavaNode> {

    private static final ClavaNodeConstructors CLAVA_NODE_CONSTRUCTORS = new ClavaNodeConstructors();

    private List<InlineComment> inlineComments;

    // private final ClavaId id;
    // private final Location location;
    private ClavaNodeInfo info;

    private ClavaData data;

    public ClavaNode(ClavaNodeInfo nodeInfo, Collection<? extends ClavaNode> children) {
        super(children);

        info = nodeInfo != null ? nodeInfo : ClavaNodeInfo.undefinedInfo();
        inlineComments = null;
        data = null;
    }

    public ClavaNode(ClavaData data, Collection<? extends ClavaNode> children) {
        super(children);

        info = null;
        inlineComments = null;
        this.data = data;
    }

    // private ClavaNode(ClavaId id, Location location, Collection<? extends ClavaNode> children) {
    // super(children);
    //
    // this.id = id;
    // this.location = location;
    // }

    protected String getTab() {
        return "   ";
    }

    public String indentCode(String code) {
        return ClavaNodes.indentCode(getTab(), code);
    }

    @Override
    public String toContentString() {
        if (hasData()) {
            return getData().toString();
        }

        if (getId().isPresent()) {
            // return "(0x" + Long.toHexString(getId().get().getId()) + ") ";
            // String location = getLocation().isValid() ? getLocation().toString() + " " : "";
            String location = "";
            return location + "(" + getId().get().getId() + ") ";
        }
        return "";

    }

    protected static String toContentString(String previousContentString, String suffix) {

        // Use bridge if there is content and a suffix
        String bridge = !suffix.isEmpty() && !previousContentString.isEmpty() ? ", " : "";

        return previousContentString + bridge + suffix;
    }
    // protected String toContentString(ClavaNode node, String suffix) {
    // String previousContentString = node.super.toContentString();
    // // Use bridge if there is content and a suffix
    // String bridge = !suffix.isEmpty() && !previousContentString.isEmpty() ? "; " : "";
    //
    // return previousContentString + bridge + suffix;
    // }

    @Override
    public ClavaNode getThis() {
        return this;
    }

    // public abstract String getCode();
    public String getCode() {
        return toUnimplementedCode();
    }

    public SourceRange getLocation() {
        if (hasData()) {
            return getData().getLocation();
        }

        SourceRange sourceRange = info.getLocation();

        if (sourceRange == null) {
            return SourceRange.invalidRange();
        }

        return sourceRange;
    }

    /**
     * TODO: Make method private when all accesses are "fixed"
     * 
     * @deprecated use .getExtendedId() instead
     * @return
     */
    @Deprecated
    public Optional<ClavaId> getId() {
        if (hasData()) {
            throw new RuntimeException("Not implemented for nodes with ClavaData");
        }
        return info.getId();
    }

    public Optional<String> getExtendedId() {
        if (hasData()) {
            return Optional.ofNullable(data.getId());
        }
        return info.getId().map(id -> id.getExtendedId());
    }

    /**
     * TODO: Make method protected when all accesses are "fixed"
     * 
     * @deprecated
     * @return
     */
    @Deprecated
    public ClavaNodeInfo getInfo() {
        if (hasData()) {
            throw new RuntimeException("Not implemented for nodes with ClavaData");
        }
        return info;
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
        // ClavaNode child = getChild(index);

        // If NullNode return empty Optional, otherwise cast and return optional
        return child instanceof NullNode ? Optional.empty() : Optional.of(castTo.cast(child));
    }

    /*
    public ClavaId getParentId() {
        return getId()
                .orElseThrow(() -> new RuntimeException("There is no ID defined"))
                .getParent()
                .orElseThrow(() -> new RuntimeException("Could not find a parent in id '" + getId() + "'"));
    
        // return getParentIdTry()
        // .orElseThrow(() -> new RuntimeException("Could not find a parent in id '" + getId() + "'"));
    }
    */

    // public Optional<ClavaId> getParentIdTry() {
    // if (!getId().isPresent()) {
    // return Optional.empty();
    // }
    //
    // return getId().get().getParent();
    //
    // }

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
        if (hasData()) {
            return Optional.of(data.getLocation());
        }

        return info.getLocationTry();
    }

    protected static Expr nullable(Expr expr) {
        return expr == null ? new NullExpr() : expr;
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
        if (hasData()) {
            data.addInlineComment(inlineComment);
            return;
        }

        Preconditions.checkArgument(!inlineComment.isStmtComment(),
                "InlineComment must not be a statement comment:" + inlineComment);

        if (inlineComments == null) {
            inlineComments = new ArrayList<>();
            inlineComments.add(inlineComment);
            return;
        }

        // If there is already an inline comment, add
        inlineComments.add(inlineComment);
        // Preconditions.checkArgument(this.inlineComment == null, "Node already has an inline comment");
        //
        // this.inlineComment = inlineComment;
    }

    @Override
    public ClavaNode copy() {
        if (hasData()) {
            return super.copy();
        }

        // Create copy
        ClavaNode copy = super.copy();

        // Associate inline comment
        copy.inlineComments = new ArrayList<>(getInlineComments());

        return copy;
    }

    @Override
    protected ClavaNode copyPrivate() {
        if (hasData()) {
            return CLAVA_NODE_CONSTRUCTORS.newClavaNode(getClass(), getData(), Collections.emptyList());
        }

        throw new NotImplementedException(this);
    }

    public boolean hasInlineComments() {
        return getInlineComments().isEmpty();
    }

    public List<InlineComment> getInlineComments() {
        if (hasData()) {
            return data.getInlineComments();
        }
        return inlineComments == null ? Collections.emptyList() : inlineComments;
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
        // if (inlineComments == null) {
        // return Collections.emptyList();
        // }
        //
        // List<InlineComment> detachedComments = inlineComments;
        // inlineComments = null;
        //
        // return detachedComments;
    }

    public String getInlineCommentsCode() {
        List<InlineComment> inlineComments = getInlineComments();
        if (inlineComments.isEmpty()) {
            return "";
        }

        String code = getInlineComments().stream()
                .map(InlineComment::getCode)
                .collect(Collectors.joining(" ", " ", ""));
        // if (code.equals(" // const object with mutable member (duplicates comment)")) {
        // System.out.println("STACK:");
        // System.out.println(Arrays.toString(Thread.currentThread().getStackTrace()));
        // }
        // System.out.println("COMMENT:" + code);

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

    // public void setInfo(ClavaNodeInfo info) {
    // this.info = info != null ? info : ClavaNodeInfo.undefinedInfo();
    // }

    public List<ClavaNode> getChildrenNormalized() {
        return getChildren().stream().map(ClavaNodes::normalize).collect(Collectors.toList());
    }

    public void setId(String newId) {
        if (hasData()) {
            // data = data.setId(newId);
            data.setId(newId);
            return;
        }
        info.setId(newId);
    }

    public void setData(ClavaData data) {
        this.data = data;
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
     * The data associated with this node.
     * 
     * <p>
     * While in transition between ClavaData nodes and Legacy nodes, ClavaData nodes should not be directly accessed
     * (e.g., .getData()). To maintain compatibility, implement getters in the ClavaNode instances.
     * 
     * @return
     */
    public ClavaData getData() {
        if (data == null) {
            throw new RuntimeException("ClavaData is not defined");
        }
        return data;
    }

    // protected void setData(ClavaData data) {
    // this.data = data;
    // }

    /**
     * TODO: If legacy code is removed, this method becomes obsolete.
     * 
     * @return
     */
    public boolean hasData() {
        return data != null;
    }

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
}

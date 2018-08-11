/**
 * Copyright 2012 SPeCS Research Group.
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

package pt.up.fe.specs.clang.ast;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import pt.up.fe.specs.clang.ast.genericnode.ClangRootNode;
import pt.up.fe.specs.clang.clavaparser.utils.ClangGenericParsers;
import pt.up.fe.specs.clava.ClavaId;
import pt.up.fe.specs.clava.ClavaId.RelationType;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.util.exceptions.NotImplementedException;
import pt.up.fe.specs.util.stringparser.StringParser;
import pt.up.fe.specs.util.stringparser.StringParsers;
import pt.up.fe.specs.util.treenode.ATreeNode;
import pt.up.fe.specs.util.treenode.utils.JsonWriter;
import pt.up.fe.specs.util.utilities.BuilderWithIndentation;

/**
 * Base node for all the nodes in the Clang AST.
 * 
 * @author Joao Bispo
 * 
 */
public abstract class ClangNode extends ATreeNode<ClangNode> {

    // public static Map<Class<?>, Integer> classes = new HashMap<>();
    // private final boolean countInstances = false;

    private String name;
    private ClavaId clavaId = null;
    // private final Lazy<ClavaNodeInfo> info;

    public ClangNode(String name) {
        super(Collections.emptyList());

        this.name = name;

        // info = new ThreadSafeLazy<>(() -> toInfo());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isManuallyCreated() {
        return false;
    }

    public abstract Optional<String> getContentTry();

    public void setContent(String content) {
        throw new NotImplementedException(getThis().getClass());
    }

    public String getContent() {
        return getContentTry()
                .orElseThrow(() -> new RuntimeException("No content defined in node '" + getName() + "'"));
    }

    public boolean hasId() {
        return getIdRawTry().isPresent();
    }

    /**
     * @return
     */
    protected abstract Optional<String> getIdRawTry();

    public String getIdRaw() {
        return getIdRawTry().get();
    }

    /*
    public String getAddress() {
    return getAddressTry().get();
    }
    */

    public ClavaNodeInfo getInfo() {
        // Initialize ClavaId, if it has not been yet
        if (hasId() && clavaId == null) {
            clavaId = parseAddress(new StringParser(getIdRaw()));
        }

        return new ClavaNodeInfo(clavaId, getLocation());
        // return info.get();
    }

    /**
     * @deprecated getExtendedId and getId are the same, check which one to replace
     * @return
     */
    @Deprecated
    public String getId() {
        return getInfo().getIdLong();
        // return info.get().getIdLong();
    }

    public String getExtendedId() {
        return getInfo().getExtendedId();
        // return info.get().getExtendedId();
    }

    public Optional<String> getExtendedIdTry() {
        return getInfo().getId().map(id -> id.getExtendedId());
        // return info.get().getId().map(id -> id.getExtendedId());
    }

    /**
     * ClangNode ids are composed by an address and a number, separated by an underscore (e.g., 0x3bb76f0_0).
     * 
     * <p>
     * This method returns the underscore and the number.
     * 
     * @return
     */
    public String getIdSuffix() {
        String id = getExtendedId();
        int underscoreIndex = id.lastIndexOf('_');

        if (underscoreIndex == -1) {
            throw new RuntimeException("Expected to find an underscore (_) in the id: " + id);
        }

        // Preconditions.checkArgument(id.length() > underscoreIndex + 1,
        // "Expected to have characters after the underscore:" + id);

        return id.substring(underscoreIndex);
    }

    // public Optional<Long> getId() {
    // return getAddress().map(address -> Long.decode(address));
    // }

    public abstract Optional<String> getLocationString();

    public static String jsonContents(ClangNode node) {
        BuilderWithIndentation builder = new BuilderWithIndentation();

        // Always has a name
        builder.addLine("\"name\": \"" + node.getName() + "\",");

        // Address
        if (node.hasId()) {
            builder.addLine("\"address\": \"" + node.getExtendedId() + "\",");
        }
        // node.getAddressTry().ifPresent(string -> builder.addLine("\"address\": \"" + string + "\","));

        // Content
        node.getContentTry().ifPresent(string -> builder.addLine("\"content\": \"" + string + "\","));

        // Location
        node.getLocationString()
                .ifPresent(string -> builder.addLine("\"location_raw\": \"" + JsonWriter.escape(string) + "\","));

        // Root node does not have location
        /*
        if (node.getParent() != null) {
        // Add code if only spans one line
        Location location = node.getLocation();
        if (location.getStartLine() == location.getEndLine()) {
        	builder.addLine("\"code\": \"" + node.getLocationCode() + "\",");
        }
        
        }
        */

        return builder.toString();
    }

    public Optional<ClangRootNode> getClangRootTry() {
        if (!hasParent()) {
            if (this instanceof ClangRootNode) {
                return Optional.of((ClangRootNode) this);
            }

            return Optional.empty();
        }

        return getParent().getClangRootTry();
    }

    public ClangRootNode getClangRoot() {
        return getClangRootTry().orElseThrow(() -> new RuntimeException("No ClangRoot defined"));
    }

    @Override
    public ClangNode getThis() {
        return this;
    }

    /**
     * What part in the original code corresponds to this node.
     * 
     * @return
     */
    public abstract SourceRange getLocation();

    // public abstract String toNodeString(LocationDecoder decoder);

    /**
     * 
     * @return the C/C++ code corresponding to this node (experimental)
     */
    public String getCode() {
        throw new UnsupportedOperationException("Not implemented for class '" + getClass().getSimpleName() + "'");
    }

    /**
     * The tab. Currently it is equivalent to 3 spaces.
     * 
     * @return
     */
    protected String getTab() {
        return "   ";
    }

    /**
     * Get the first child of the given class, with an optional.
     * 
     * @param targetType
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T extends ClangNode> Optional<T> getFirstChildOptional(Class<T> targetType) {
        // It is safe to cast to T, since it returns a node that implements the given class
        return (Optional<T>) getChildren().stream()
                // .filter(node -> node.getClass().equals(targetType.getClass()))
                .filter(node -> targetType.isInstance(node))
                .findFirst();
    }

    /**
     * Get the first child of the given class, or throw an exception if no node of the target type is found.
     * 
     * @param targetType
     * @return
     */
    public <T extends ClangNode> T getFirstChild(Class<T> targetType) {
        return getFirstChildOptional(targetType).orElseThrow(
                () -> {
                    StringJoiner joiner = new StringJoiner(", ");
                    getChildrenStream()
                            .map(child -> child.getClass().getSimpleName())
                            .forEach(name -> joiner.add(name));

                    return new RuntimeException("Could not find a '" + targetType.getSimpleName() + "' inside "
                            + getClass().getSimpleName() + ". Children classes:\n" + joiner.toString());
                });
    }

    /**
     * Get the first child of the given class.
     * 
     * <p>
     * The search is done over all children nodes. Traverse is done in a breath-first manner.
     * 
     * @param targetType
     * @return
     */
    @SuppressWarnings("unchecked")
    public <T extends ClangNode> Optional<T> getFirstChildRecursiveOptional(Class<T> targetType) {
        // It is safe to cast to T, since it returns a node that implements the given class
        Optional<T> foundChild = (Optional<T>) getChildren().stream()
                // .filter(node -> node.getClass().equals(targetType.getClass()))
                .filter(node -> targetType.isInstance(node))
                .findFirst();

        // If found, return it
        if (foundChild.isPresent()) {
            return foundChild;
        }

        // Child not found, search in each of the children
        for (ClangNode child : getChildren()) {
            foundChild = child.getFirstChildRecursiveOptional(targetType);
            if (foundChild.isPresent()) {
                return foundChild;
            }
        }

        // Could not find child of the given class, return empty optional
        return Optional.empty();
    }

    /**
     * Get the first child of the given class, or throw an exception if no node of the target type is found.
     * 
     * <p>
     * The search is done over all children nodes. Traverse is done in a breath-first manner.
     * 
     * <p>
     * Throws an exception if the node of the given class is not found.
     * 
     * @param targetType
     * @return
     */
    public <T extends ClangNode> T getFirstChildRecursive(Class<T> targetType) {
        return getFirstChildRecursiveOptional(targetType).orElse(null);
    }

    /**
     * Get all children that are an instance of the given class.
     * 
     * @param targetType
     * @return
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ClangNode> List<T> getChildren(Class<T> targetType) {
        // It is safe to cast to T, since it returns nodes that implements the given class
        return (List<T>) getChildren().stream()
                .filter(node -> targetType.isInstance(node))
                .collect(Collectors.toList());
    }

    /**
     * Helper method that throws an exception if any of the children are NOT of the given class.
     * 
     * @param targetType
     * @return
     */
    public <T extends ClangNode, T2 extends ClangNode> List<T> getChildrenAll(Class<T> targetType,
            Class<T2> ignoreType) {
        List<T> children = getChildren(targetType);
        List<T2> ignoreChildren = getChildren(ignoreType);

        if (ignoreType.isAssignableFrom(targetType) || targetType.isAssignableFrom(ignoreType)) {
            throw new IllegalArgumentException("targetType and ignoreType must be disjoint");
        }

        if (children.size() != getNumChildren() - ignoreChildren.size()) {
            String message = "Node " + getClass().getSimpleName()
                    + " contains children that are not of class '" + targetType.getSimpleName() + "':\n"
                    + getChildren();

            throw new RuntimeException(message);
        }

        return children;
    }

    @Override
    public ClangNodeIterator getChildrenIterator() {
        return new ClangNodeIterator(this);
    }

    /**
     * As default, returns false.
     * 
     * @return true if this node should not be in the tree after parsing.
     */
    public boolean isTemporary() {
        return false;
    }

    /**
     * Joins the corresponding code of a list of nodes, according to the given strings.
     * 
     * @param delimiter
     * @param prefix
     * @param suffix
     * @param nodes
     * @return
     */
    protected String joinCode(String delimiter, String prefix, String suffix, List<ClangNode> nodes) {
        StringJoiner joiner = new StringJoiner(delimiter, prefix, suffix);

        nodes.forEach(arg -> joiner.add(arg.getCode()));

        return joiner.toString();
    }

    @Override
    public String getNodeName() {
        String originalName = super.getNodeName();

        if (originalName.endsWith("Node")) {
            return originalName.substring(0, originalName.length() - "Node".length());
        }

        return originalName;
    }

    /**
     * Returns the corresponding code, if a leaf node, or an empty string, if it has children.
     */
    @Override
    public String toContentString() {
        if (hasChildren()) {
            return "";
        }
        return getCode();
    }

    /**
     * 
     * @return a string representing the name of the Clang node, and its contents, if any
     */
    public String getDescription() {
        StringBuilder content = new StringBuilder(getName());
        getContentTry().ifPresent(nodeContent -> content.append(" - ").append(nodeContent));

        return content.toString();
    }

    /**
     * 
     * @return
     */
    private ClavaNodeInfo toInfo() {

        if (!hasId()) {
            return ClavaNodeInfo.undefinedInfo(getLocation());
        }

        ClavaId id = parseAddress(new StringParser(getIdRaw()));

        return new ClavaNodeInfo(id, getLocation());
    }

    private ClavaId parseAddress(StringParser parser) {

        // Long id = parser.apply(ClangParseWorkers::parseHex);
        String id = parser.apply(StringParsers::parseWord);
        if (parser.isEmpty()) {
            return new ClavaId(id);
        }

        // Parse relationship
        RelationType type = parser.apply(string -> ClangGenericParsers.parseEnum(string, RelationType.class));

        ClavaId next = parseAddress(parser);

        return new ClavaId(id, type, next);
    }

}

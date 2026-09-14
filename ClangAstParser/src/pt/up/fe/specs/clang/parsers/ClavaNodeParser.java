/**
 * Copyright 2018 SPeCS.
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

package pt.up.fe.specs.clang.parsers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

import org.suikasoft.jOptions.Interfaces.DataStore;
import pt.up.fe.specs.clang.dumper.ClangAstData;
import pt.up.fe.specs.clang.utils.ChildrenAdapter;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.DummyNode;
import pt.up.fe.specs.clava.ast.attr.Attribute;
import pt.up.fe.specs.clava.ast.attr.AlignedExprAttr;
import pt.up.fe.specs.clava.ast.attr.AlignedTypeAttr;
import pt.up.fe.specs.clava.ast.attr.AlignedAttr;
import pt.up.fe.specs.clava.ast.attr.enums.AlignedAttrKind;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.utils.ClassesService;
import pt.up.fe.specs.util.SpecsLogs;

/** Builds Clava nodes from eagerly decoded protobuf records. */
public class ClavaNodeParser {

    private final ClassesService classesService;
    private final Set<String> missingConstructors;
    private ChildrenAdapter childrenAdapter;

    public ClavaNodeParser(ClassesService classesService) {
        this.classesService = classesService;
        this.missingConstructors = new HashSet<>();
        this.childrenAdapter = null;
    }

    public void init(ClangAstData data) {

        if (!data.hasValue(ClangAstData.CONTEXT)) {
            throw new RuntimeException("ClavaNodeParser requires ClavaContext");
        }

        ClavaNodes clavaNodes = new ClavaNodes(data.get(ClangAstData.CONTEXT).get(ClavaContext.FACTORY));
        data.set(ClangAstData.CLAVA_NODES, clavaNodes);

        childrenAdapter = new ChildrenAdapter(data.get(ClangAstData.CONTEXT));
    }

    /**
     * Applies one already-decoded protobuf node.  The wire reader deliberately
     * calls this method only after the node data and its class record have both
     * arrived; all cross-node values remain queued in {@link ClavaNodes} until
     * the complete stream has been consumed.
     */
    public void applyRecord(String nodeId, String classname, ClangAstData data) {
        Map<String, ClavaNode> parsedNodes = data.get(ClangAstData.CLAVA_NODES).getNodes();
        if (parsedNodes.containsKey(nodeId)) {
            throw new IllegalArgumentException("Duplicated Clava node '" + nodeId + "'");
        }

        ClavaNode node = parseNode(nodeId, classname, data);
        parsedNodes.put(nodeId, node);
    }

    private ClavaNode parseNode(String nodeId, String classname, ClangAstData data) {
        boolean debug = data.get(ClangAstData.DEBUG);

        if (classname == null) {
            throw new RuntimeException("No classname for node '" + nodeId + "");
            // if (debug)
            // SpecsLogs.msgInfo("No classname for node '" + nodeId + "");
            // return new UnsupportedNode("<CLASSNAME NOT FOUND>", ClavaData.empty(), Collections.emptyList());
        }

        // DataStore mapped to the node id
        DataStore nodeData = data.get(ClangAstData.NODE_DATA).get(nodeId);

        if (nodeData == null) {
            throw new RuntimeException("No ClavaData/DataStore for node '" + nodeId + "' (classname: " + classname
                    + "), protobuf data dumper is not being called");
        }

        // Get corresponding ClavaNode class

        Class<? extends ClavaNode> clavaNodeClass = getClavaNodeClass(classname, nodeData);

        // Get children ids
        List<String> childrenIds = getChildrenIds(nodeId, classname, data);

        Map<String, ClavaNode> parsedNodes = data.get(ClangAstData.CLAVA_NODES).getNodes();

        List<ClavaNode> children = Collections.emptyList();

        ClavaNode clavaNode = buildChildlessNode(nodeId, children, classname, debug, nodeData, clavaNodeClass);

        // Queue setting the children
        data.getClavaNodes().queueAction(() -> {

            // Get the children nodes
            List<ClavaNode> newChildren = new ArrayList<>(childrenIds.size());
            // for (String childId : childrenIds) {

            for (int i = 0; i < childrenIds.size(); i++) {
                String childId = childrenIds.get(i);
                ClavaNode child = parsedNodes.get(childId);

                // Check if nullptr
                if (child == null && ClavaNodes.isNullId(childId)) {
                    child = data.get(ClangAstData.CLAVA_NODES).nullNode(childId);
                }

                int index = i;
                Objects.requireNonNull(child,
                        () -> "Did not find ClavaNode for child with index '" + index + "' and id '" + childId
                                + "' when parsing " + clavaNodeClass.getSimpleName() + " -> " + nodeData);

                child = processChild(child, clavaNodeClass, data);

                newChildren.add(child);

            }

            clavaNode.setChildren(childrenAdapter.adaptChildren(clavaNode, newChildren));
        });

        return clavaNode;
    }

    private Class<? extends ClavaNode> getClavaNodeClass(String classname, DataStore nodeData) {

        if (classname.equals("AlignedAttr")) {
            return nodeData.get(AlignedAttr.ALIGNED_ATTR_KIND) == AlignedAttrKind.EXPR
                    ? AlignedExprAttr.class
                    : AlignedTypeAttr.class;
        }

        try {
            return classesService.getClass(classname, nodeData);
        } catch (Exception e) {
            // If classname is an attribute, use generic Attribute class
            if (classname.endsWith("Attr")) {
                // Add custom mapping to avoid exception next time this classname is used
                classesService.getCustomClassMap().add(classname, data -> Attribute.class);

                return Attribute.class;
            }

            throw new RuntimeException(e);
        }

    }

    private ClavaNode processChild(ClavaNode child, Class<? extends ClavaNode> clavaNodeClass, ClangAstData data) {
        if (clavaNodeClass.equals(CompoundStmt.class)) {
            // If child is an expression, wrap a Stmt around
            if (child instanceof Expr) {
                return data.getFactory().exprStmt((Expr) child);
            }
        }

        return child;
    }

    private List<String> getChildrenIds(String nodeId, String classname, ClangAstData data) {
        List<String> childrenIds = data.get(ClangAstData.VISITED_CHILDREN).get(nodeId);

        if (childrenIds == null) {
            SpecsLogs.msgInfo("No children for node '" + nodeId + "' (" + classname + ")");

            childrenIds = Collections.emptyList();
        }
        return childrenIds;
    }

    private ClavaNode buildChildlessNode(String nodeId, List<ClavaNode> children, String classname, boolean debug,
            DataStore nodeData,
            Class<? extends ClavaNode> clavaNodeClass) {
        // Get constructor based on DataStore
        BiFunction<DataStore, List<? extends ClavaNode>, ClavaNode> dataStoreBuilder = classesService
                .getClavaNodeBuilder(clavaNodeClass);

        if (dataStoreBuilder != null) {
            // Build node based on data and children
            // return dataStoreBuilder.apply(nodeData, children);
            return dataStoreBuilder.apply(nodeData, children);
        }

        if (!missingConstructors.contains(classname)) {

            // throw new RuntimeException(
            // "No builder for node '" + nodeId + "', missing constructor 'new " + classname + "("
            // + DataStore.class.getSimpleName()
            // + " data, Collection<? extends ClavaNode> children)'");

            missingConstructors.add(classname);
            if (debug) {
                SpecsLogs
                        .msgInfo("No builder for node '" + nodeId + "', missing constructor 'new " + classname + "("
                                + DataStore.class.getSimpleName()
                                + " data, Collection<? extends ClavaNode> children)'");
            }
        }

        // return DummyNode.newInstance(clavaNodeClass, nodeData, children, false);
        return DummyNode.newInstance(clavaNodeClass, nodeData, children, false);
    }

    public void close(ClangAstData data) {
        data.get(ClangAstData.CLAVA_NODES).getQueuedActions().stream()
                .forEach(Runnable::run);

    }

}

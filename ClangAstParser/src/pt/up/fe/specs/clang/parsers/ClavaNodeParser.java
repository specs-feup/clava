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
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clang.parsers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.streamparser.LineStreamWorker;

import pt.up.fe.specs.clang.dumper.ClangAstData;
import pt.up.fe.specs.clang.utils.ChildrenAdapter;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.DummyNode;
import pt.up.fe.specs.clava.ast.attr.Attribute;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.utils.ClassesService;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.utilities.LineStream;

public class ClavaNodeParser implements LineStreamWorker<ClangAstData> {

    /// DATAKEYS BEGIN

    // public final static DataKey<Set<String>> NODES_CURRENTLY_BEING_PARSED = KeyFactory
    // .generic("nodesCurrentlyBeingParsed", (Set<String>) new HashSet<String>())
    // .setDefault(() -> new HashSet<>());

    /// DATAKEYS END

    private static final String PARSER_ID = "<Id to Class Map>";

    private final ClassesService classesService;
    private final Set<String> missingConstructors;
    private ChildrenAdapter childrenAdapter;

    public ClavaNodeParser(ClassesService classesService) {
        this.classesService = classesService;
        this.missingConstructors = new HashSet<>();
        this.childrenAdapter = null;
    }

    @Override
    public String getId() {
        return PARSER_ID;
    }

    @Override
    public void init(ClangAstData data) {

        if (!data.hasValue(ClangAstData.CONTEXT)) {
            throw new RuntimeException("ClavaNodeParser requires ClavaContext");
        }

        // data.add(ClangParserKeys.CLAVA_NODES, new HashMap<>());

        // ClavaNodes clavaNodes = new ClavaNodes(data.get(ClangParserData.CONTEXT).get(ClavaContext.FACTORY),
        // data.get(ClangParserData.SKIPPED_NODES_MAP));
        ClavaNodes clavaNodes = new ClavaNodes(data.get(ClangAstData.CONTEXT).get(ClavaContext.FACTORY));
        data.set(ClangAstData.CLAVA_NODES, clavaNodes);
        // data.add(NODES_CURRENTLY_BEING_PARSED, new HashSet<>());

        childrenAdapter = new ChildrenAdapter(data.get(ClangAstData.CONTEXT));
    }

    @Override
    public void apply(LineStream lineStream, ClangAstData data) {
        // Get nodeId and classname
        String nodeId = lineStream.nextLine();

        /*
        if (data.get(ClangParserData.NODES_CURRENTLY_BEING_PARSED).contains(nodeId)) {
            throw new RuntimeException("Found circular dependence when parsing node '" + nodeId + "'");
        } else {
            data.get(ClangParserData.NODES_CURRENTLY_BEING_PARSED).add(nodeId);
            System.out.println("NODES BEING PARSED:" + data.get(ClangParserData.NODES_CURRENTLY_BEING_PARSED));
        }
        */
        // System.out.println("PARSING NODE " + nodeId);

        String classname = lineStream.nextLine();
        // System.out.println("CLASS NAMES:" + classname);
        Map<String, ClavaNode> parsedNodes = data.get(ClangAstData.CLAVA_NODES).getNodes();

        // Check if node was already parsed
        if (parsedNodes.containsKey(nodeId)) {
            return;
        }

        ClavaNode node = parseNode(nodeId, classname, data, lineStream);

        // If UnsupportedNode, transform to DummyNode
        // node = transformUnsupportedNode(node);

        // Store node
        parsedNodes.put(nodeId, node);
        // data.get(ClangParserData.NODES_CURRENTLY_BEING_PARSED).remove(nodeId);
    }

    /*
    private ClavaNode transformUnsupportedNode(ClavaNode node) {
        if (!(node instanceof UnsupportedNode)) {
            return node;
        }
    
        UnsupportedNode unsupportedNode = (UnsupportedNode) node;
    
        // Determine DummyNode type based on Data
        ClavaData data = node.getData();
    
        if (data instanceof TypeDataV2) {
            DummyTypeData dummyData = new DummyTypeData(unsupportedNode.getClassname(), (TypeDataV2) data);
            return new DummyType(dummyData, unsupportedNode.getChildren());
        }
    
        if (data instanceof DeclDataV2) {
            DummyDeclData dummyData = new DummyDeclData(unsupportedNode.getClassname(), (DeclDataV2) data);
            return new DummyDecl(dummyData, unsupportedNode.getChildren());
        }
    
        if (data instanceof ExprDataV2) {
            DummyExprData dummyData = new DummyExprData(unsupportedNode.getClassname(), (ExprDataV2) data);
            return new DummyExpr(dummyData, unsupportedNode.getChildren());
        }
    
        if (data instanceof StmtData) {
            DummyStmtData dummyData = new DummyStmtData(unsupportedNode.getClassname(), (StmtData) data);
            return new DummyStmt(dummyData, unsupportedNode.getChildren());
        }
    
        if (data instanceof AttributeData) {
            DummyAttributeData dummyData = new DummyAttributeData(unsupportedNode.getClassname(), (AttributeData) data);
            return new DummyAttr(dummyData, unsupportedNode.getChildren());
        }
    
        throw new RuntimeException("ClavaData class not supported:" + data.getClass());
    }
    */
    private ClavaNode parseNode(String nodeId, String classname, ClangAstData data, LineStream lineStream) {
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
                    + "), data dumper is not being called (linestream index '" + lineStream.getLastLineIndex() + "')");
            // if (debug)
            // SpecsLogs.msgInfo("No ClavaData for node '" + nodeId + "' (classname: " + classname
            // + "), data dumper is not being called");
            // return new UnsupportedNode(classname, ClavaData.empty(), Collections.emptyList());
        }

        // Get corresponding ClavaNode class

        Class<? extends ClavaNode> clavaNodeClass = getClavaNodeClass(classname, nodeData);

        // Get children ids
        List<String> childrenIds = getChildrenIds(nodeId, classname, data);

        Map<String, ClavaNode> parsedNodes = data.get(ClangAstData.CLAVA_NODES).getNodes();

        List<ClavaNode> children = Collections.emptyList();
        /*
        // Get the children nodes
        List<ClavaNode> children = new ArrayList<>(childrenIds.size());
        // for (String childId : childrenIds) {
        
        for (int i = 0; i < childrenIds.size(); i++) {
            String childId = childrenIds.get(i);
            ClavaNode child = parsedNodes.get(childId);
        
            // Check if nullptr
            if (child == null && ClavaNodes.isNullId(childId)) {
                child = data.get(ClangParserData.CLAVA_NODES).nullNode(childId);
            }
        
            int index = i;
            SpecsCheck.checkNotNull(child,
                    () -> "Did not find ClavaNode for child with index '" + index + "' and id '" + childId
                            + "' when parsing "
                            + clavaNodeClass.getSimpleName() + " -> " + nodeData);
        
            children.add(child);
        }
        */

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

                // Check if skipped node
                // if (child == null) {
                // String nullptrId = data.get(ClangParserData.SKIPPED_NODES_MAP).get(childId);
                // if (nullptrId != null) {
                // child = data.get(ClangParserData.CLAVA_NODES).nullNode(nullptrId);
                // }
                // }

                int index = i;
                SpecsCheck.checkNotNull(child,
                        () -> "Did not find ClavaNode for child with index '" + index + "' and id '" + childId
                                + "' when parsing "
                                + clavaNodeClass.getSimpleName() + " -> " + nodeData);

                child = processChild(child, clavaNodeClass, data);

                newChildren.add(child);

                // if (child instanceof UnaryOperator) {
                // System.out.println(
                // "UNARY: '" + child.get(ClavaNode.ID) + " -> " + child);
                // System.out.println("FUTURE PARENT:" + clavaNode);
                //
                // }
                // //
                // if (child.hasParent()) {
                // System.out.println(
                // "CHILD '" + child.get(ClavaNode.ID) + "' ALREADY HAS PARENT:" + child.getParent().toTree());
                // }

            }

            // clavaNode.setChildren(newChildren);
            // ClavaNode.SKIP_EXCEPTION = true;
            clavaNode.setChildren(childrenAdapter.adaptChildren(clavaNode, newChildren));
            // ClavaNode.SKIP_EXCEPTION = false;
        });

        return clavaNode;
    }

    private Class<? extends ClavaNode> getClavaNodeClass(String classname, DataStore nodeData) {

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

    @Override
    public void close(ClangAstData data) {
        data.get(ClangAstData.CLAVA_NODES).getQueuedActions().stream()
                .forEach(Runnable::run);

        // for (var action : data.get(ClangParserData.CLAVA_NODES).getQueuedActions()) {
        // try {
        // action.run();
        // } catch (Exception e) {
        // throw new RunnerException("Could not complete action for node with data " + data);
        // }
        // }

        // ClavaLog.metrics("Parsed ClavaNodes: " + data.get(ClangParserData.CLAVA_NODES).getNodes().size());
    }

}

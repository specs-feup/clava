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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.streamparser.LineStreamWorker;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.streamparserv2.ClassesService;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaData;
import pt.up.fe.specs.clava.ast.DummyNode;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.utilities.LineStream;

public class ClavaNodeParser implements LineStreamWorker {

    private static final String PARSER_ID = "<Id to Class Map>";

    private final ClassesService classesService;
    private final Set<String> missingConstructors;

    public ClavaNodeParser(ClassesService classesService) {
        this.classesService = classesService;
        this.missingConstructors = new HashSet<>();
    }

    @Override
    public String getId() {
        return PARSER_ID;
    }

    @Override
    public void init(DataStore data) {
        data.add(ClangParserKeys.CLAVA_NODES, new HashMap<>());
    }

    @Override
    public void apply(LineStream lineStream, DataStore data) {
        // Get nodeId and classname
        String nodeId = lineStream.nextLine();
        String classname = lineStream.nextLine();

        Map<String, ClavaNode> parsedNodes = data.get(ClangParserKeys.CLAVA_NODES);

        // Check if node was already parsed
        if (parsedNodes.containsKey(nodeId)) {
            return;
        }

        ClavaNode node = parseNode(nodeId, classname, data);

        // If UnsupportedNode, transform to DummyNode
        // node = transformUnsupportedNode(node);

        // Store node
        parsedNodes.put(nodeId, node);
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
    private ClavaNode parseNode(String nodeId, String classname, DataStore data) {
        boolean debug = data.get(ClangParserKeys.DEBUG);

        if (classname == null) {
            throw new RuntimeException("No classname for node '" + nodeId + "");
            // if (debug)
            // SpecsLogs.msgInfo("No classname for node '" + nodeId + "");
            // return new UnsupportedNode("<CLASSNAME NOT FOUND>", ClavaData.empty(), Collections.emptyList());
        }

        // Get ClavaData mapped to the node id
        ClavaData clavaData = data.get(ClangParserKeys.CLAVA_DATA).get(nodeId);

        if (clavaData == null) {
            throw new RuntimeException("No ClavaData for node '" + nodeId + "' (classname: " + classname
                    + "), data dumper is not being called");
            // if (debug)
            // SpecsLogs.msgInfo("No ClavaData for node '" + nodeId + "' (classname: " + classname
            // + "), data dumper is not being called");
            // return new UnsupportedNode(classname, ClavaData.empty(), Collections.emptyList());
        }

        // Get corresponding ClavaNode class
        Class<? extends ClavaNode> clavaNodeClass = classesService.getClass(classname, clavaData.getData());

        // Get children ids
        List<String> childrenIds = data.get(ClangParserKeys.VISITED_CHILDREN).get(nodeId);

        if (childrenIds == null) {
            SpecsLogs.msgInfo("No children for node '" + nodeId + "' (" + classname + ")");
            // if (debug) {
            // SpecsLogs.msgInfo("No children for node '" + nodeId + "' (" + classname + ")");
            // }

            childrenIds = Collections.emptyList();
            // return DummyNode.newInstance(classname, clavaData, Collections.emptyList());
            // return new UnsupportedNode(classname, clavaData, Collections.emptyList());
        }

        Map<String, ClavaNode> parsedNodes = data.get(ClangParserKeys.CLAVA_NODES);

        // Get the children nodes
        List<ClavaNode> children = new ArrayList<>(childrenIds.size());
        for (String childId : childrenIds) {
            ClavaNode child = parsedNodes.get(childId);

            // Check if nullptr
            if (child == null && ClavaNodes.isNullId(childId)) {
                child = ClavaNodes.nullNode(childId);
            }

            Preconditions.checkNotNull(child, "Did not find ClavaNode for child with id '" + childId + "'");
            children.add(child);
        }

        // Get constructor based on DataStore
        BiFunction<DataStore, List<ClavaNode>, ClavaNode> dataStoreBuilder = classesService
                .getDataStoreBuilder(clavaNodeClass);

        if (dataStoreBuilder != null) {
            // Build node based on data and children
            return dataStoreBuilder.apply(clavaData.getData(), children);
        }

        // Get ClavaNode constructor
        BiFunction<ClavaData, List<ClavaNode>, ClavaNode> builder = classesService.getBuilder(clavaNodeClass,
                clavaData.getClass());

        if (builder == null) {
            if (!missingConstructors.contains(classname)) {
                missingConstructors.add(classname);
                if (debug)
                    SpecsLogs
                            .msgInfo("No builder for node '" + nodeId + "', missing constructor 'new " + classname + "("
                                    + clavaData.getClass().getSimpleName()
                                    + " data, Collection<? extends ClavaNode> children)'");
            }

            return DummyNode.newInstance(clavaNodeClass, clavaData.getData(), children);
            // return new UnsupportedNode(classname, clavaData, children);
        }

        // Build node based on data and children
        return builder.apply(clavaData, children);
    }

}

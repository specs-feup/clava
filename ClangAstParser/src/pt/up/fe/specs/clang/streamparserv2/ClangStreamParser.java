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

package pt.up.fe.specs.clang.streamparserv2;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clang.parsers.ClavaDataParser;
import pt.up.fe.specs.clang.parsers.IdToClassnameParser;
import pt.up.fe.specs.clang.parsers.TopLevelNodesParser;
import pt.up.fe.specs.clang.parsers.VisitedChildrenParser;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.data2.ClavaData;
import pt.up.fe.specs.clava.ast.extra.UnsupportedNode;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.collections.MultiMap;

/**
 * Creates a Clava tree from information dumper by ClangAstDumper.
 * 
 * @author JoaoBispo
 *
 */
public class ClangStreamParser {
    private final DataStore data;

    private final ClassesService classesService;

    public ClangStreamParser(DataStore data) {
        this.data = data;

        classesService = new ClassesService(new HashMap<>());
    }

    public void parse() {
        // Get top-level nodes
        Set<String> topLevelNodes = data.get(TopLevelNodesParser.getDataKey());
        // System.out.println("TOP LEVEL NODES:" + topLevelNodes);
        // Separate into translation units?

        MultiMap<String, ClavaNode> app = new MultiMap<>();
        for (String topLevelId : topLevelNodes) {
            ClavaNode clavaNode = parse(topLevelId);

            // Determine key base on id
            int lastIndexOfUnderscore = topLevelId.lastIndexOf('_');
            if (lastIndexOfUnderscore == -1) {
                throw new RuntimeException("Expected to find at least one underscore: " + topLevelId);
            }

            String key = topLevelId.substring(lastIndexOfUnderscore + 1);
            app.put(key, clavaNode);
        }

        System.out.println("PARSED NODES:\n" + app);

    }

    private ClavaNode parse(String nodeId) {

        String classname = data.get(IdToClassnameParser.getDataKey()).get(nodeId);
        // System.out.println("CLASSNAME:" + classname);

        if (classname == null) {
            SpecsLogs.msgInfo("No classname for node '" + nodeId + "");
            return new UnsupportedNode("<CLASSNAME NOT FOUND>", ClavaData.empty(), Collections.emptyList());
        }

        Class<? extends ClavaNode> clavaNodeClass = classesService.getClass(classname);

        // Map classname to ClavaData class
        Class<? extends ClavaData> clavaDataClass = ClavaNodeToData.getClavaDataClass(clavaNodeClass);

        if (clavaDataClass == null) {
            // SpecsLogs.msgInfo("No ClavaData class for node '" + nodeId + "'. Add mapping for class '' in Java class
            // '"
            // + ClavaNodeToData.class.getSimpleName() + "'");
            SpecsLogs.msgInfo("No ClavaData class (specific or base) for node '" + nodeId + "'. Add mapping for class '"
                    + classname + "' in Java class '"
                    + ClavaNodeToData.class.getSimpleName() + "'");
            return new UnsupportedNode(classname, ClavaData.empty(), Collections.emptyList());
        }

        // DataKey<Map<String, ClavaData>> clavaDataKey = ClavaDataParser.getDataKey(clavaDataClass);
        if (!data.hasValue(ClavaDataParser.getDataKey(clavaDataClass))) {
            SpecsLogs.msgInfo("No parsed information for class '" + classname + "', missing entry in "
                    + ClavaDataParser.class.getSimpleName() + "? (node '" + nodeId
                    + ", expected ClavaData '" + clavaDataClass.getSimpleName() + "')");
            return new UnsupportedNode(classname, ClavaData.empty(), Collections.emptyList());
        }

        ClavaData clavaData = data.get(ClavaDataParser.getDataKey(clavaDataClass)).get(nodeId);

        if (clavaData == null) {
            SpecsLogs.msgInfo("No ClavaData for node '" + nodeId + "' (" + classname + "). ");
            return new UnsupportedNode(classname, ClavaData.empty(), Collections.emptyList());
        }

        // Get children
        List<String> childrenIds = data.get(VisitedChildrenParser.getDataKey()).get(nodeId);

        if (childrenIds == null) {
            SpecsLogs.msgInfo("No children for node '" + nodeId + "' (" + classname + ")");
            return new UnsupportedNode(classname, clavaData, Collections.emptyList());
        }

        // Parse each children
        List<ClavaNode> children = childrenIds.stream()
                .map(childId -> parse(childId))
                .collect(Collectors.toList());
        // System.out.println("CHILDREN:" + children);

        BiFunction<ClavaData, List<ClavaNode>, ClavaNode> builder = classesService.getBuilder(clavaNodeClass,
                clavaDataClass);
        if (builder == null) {
            SpecsLogs.msgInfo("No builder for node '" + nodeId + "' (" + classname + ")");
            return new UnsupportedNode(classname, clavaData, children);
        }

        // Build node based on data and children (map with ClavaNode class -> builder?)
        return builder.apply(clavaData, children);
    }

}

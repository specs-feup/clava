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

package pt.up.fe.specs.clang;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

/**
 * Creates a Clava tree from information dumper by ClangAstDumper.
 * 
 * @author JoaoBispo
 *
 */
public class ClangStreamParser {
    private final DataStore data;
    private final Map<String, Class<ClavaData>> clavaDataMap;
    private final Map<String, BiFunction<ClavaData, List<ClavaNode>, ClavaNode>> clavaNodeBuilders;

    public ClangStreamParser(DataStore data) {
        this.data = data;
        clavaDataMap = buildClavaDataMap();
        clavaNodeBuilders = buildClavaNodeBuilders();
    }

    private Map<String, Class<ClavaData>> buildClavaDataMap() {
        // TODO Auto-generated method stub
        return new HashMap<>();
    }

    private Map<String, BiFunction<ClavaData, List<ClavaNode>, ClavaNode>> buildClavaNodeBuilders() {
        // TODO Auto-generated method stub
        return new HashMap<>();
    }

    public void parse() {
        // Get top-level nodes
        Set<String> topLevelNodes = data.get(TopLevelNodesParser.getDataKey());

        // Separate into translation units?

        for (String topLevelId : topLevelNodes) {
            parse(topLevelId);

        }
        // System.out.println("TOP LEVEL NODES:" + topLevelNodes);
    }

    private ClavaNode parse(String nodeId) {

        String classname = data.get(IdToClassnameParser.getDataKey()).get(nodeId);
        System.out.println("CLASSNAME:" + classname);

        // Map classname to ClavaData class
        Class<ClavaData> clavaDataClass = getClavaDataClass(classname);
        if (clavaDataClass == null) {
            System.out.println("No ClavaData class for node '" + nodeId + "' (" + classname + ")");
            return null;
        }

        ClavaData clavaData = data.get(ClavaDataParser.getDataKey(clavaDataClass)).get(nodeId);

        if (clavaData == null) {
            System.out.println("No ClavaData for node '" + nodeId + "' (" + classname + ")");
            return null;
        }

        // Get children
        List<String> childrenIds = data.get(VisitedChildrenParser.getDataKey()).get(nodeId);

        if (childrenIds == null) {
            System.out.println("No children for node '" + nodeId + "' (" + classname + ")");
            return null;
        }

        // Parse each children
        List<ClavaNode> children = childrenIds.stream()
                .map(childId -> parse(childId))
                .collect(Collectors.toList());
        System.out.println("CHILDREN:" + children);

        BiFunction<ClavaData, List<ClavaNode>, ClavaNode> builder = getBuilder(classname);
        if (builder == null) {
            System.out.println("No builder for node '" + nodeId + "' (" + classname + ")");
            return null;
        }

        // Build node based on data and children (map with ClavaNode class -> builder?)
        return builder.apply(clavaData, children);
    }

    private Class<ClavaData> getClavaDataClass(String classname) {
        Class<ClavaData> clavaDataClass = clavaDataMap.get(classname);
        if (clavaDataClass == null) {
            return null;
        }

        return clavaDataClass;
    }

    private BiFunction<ClavaData, List<ClavaNode>, ClavaNode> getBuilder(String classname) {
        BiFunction<ClavaData, List<ClavaNode>, ClavaNode> builder = clavaNodeBuilders.get(classname);
        if (builder == null) {
            return null;
        }

        return builder;
    }
}

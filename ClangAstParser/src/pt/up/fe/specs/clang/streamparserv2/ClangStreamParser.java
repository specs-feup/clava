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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clang.parsers.ClavaDataParser;
import pt.up.fe.specs.clang.parsers.IdToClassnameParser;
import pt.up.fe.specs.clang.parsers.TopLevelNodesParser;
import pt.up.fe.specs.clang.parsers.TopLevelTypesParser;
import pt.up.fe.specs.clang.parsers.VisitedChildrenParser;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.ClavaData;
import pt.up.fe.specs.clava.ast.ClavaDataPostProcessing;
import pt.up.fe.specs.clava.ast.ClavaDataUtils;
import pt.up.fe.specs.clava.ast.ClavaNodeToData;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.DummyDecl;
import pt.up.fe.specs.clava.ast.decl.data2.DeclDataV2;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.UnsupportedNode;
import pt.up.fe.specs.util.SpecsIo;
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

    private final Map<String, ClavaNode> parsedNodes;
    private final ClassesService classesService;

    public ClangStreamParser(DataStore data) {
        this.data = data;

        classesService = new ClassesService(new HashMap<>());
        this.parsedNodes = new HashMap<>();
    }

    public void parse() {
        // Get top-level nodes
        Set<String> topLevelDecls = data.get(TopLevelNodesParser.getDataKey());
        Set<String> topLevelTypes = data.get(TopLevelTypesParser.getDataKey());
        // System.out.println("TOP LEVEL NODES:" + topLevelNodes);
        // Separate into translation units?

        List<String> allTopLevelNodes = new ArrayList<>(topLevelDecls.size() + topLevelTypes.size());
        allTopLevelNodes.addAll(topLevelDecls);
        allTopLevelNodes.addAll(topLevelTypes);
        // System.out.println("TOP LEVLE TYPES:" + topLevelTypes);
        MultiMap<String, ClavaNode> app = new MultiMap<>();
        // for (String topLevelId : topLevelNodes) {
        for (String topLevelId : allTopLevelNodes) {
            ClavaNode clavaNode = parse(topLevelId);

            // Determine key base on id
            int lastIndexOfUnderscore = topLevelId.lastIndexOf('_');
            if (lastIndexOfUnderscore == -1) {
                throw new RuntimeException("Expected to find at least one underscore: " + topLevelId);
            }

            String key = topLevelId.substring(lastIndexOfUnderscore + 1);
            app.put(key, clavaNode);
        }

        // After all ClavaNodes are created, apply post-processing
        ClavaDataPostProcessing postData = new ClavaDataPostProcessing(parsedNodes);
        parsedNodes.values().stream()
                .forEach(node -> ClavaDataUtils.applyPostProcessing(node.getData(), postData));

        List<ClavaNode> topDecls = topLevelDecls.stream()
                .map(id -> parsedNodes.get(id))
                .collect(Collectors.toList());

        // Create App node
        createApp(topDecls);

        // System.out.println("PARSED NODES:\n" + app);

    }

    private ClavaNode parse(String nodeId) {
        // Check if node was already parsed
        if (parsedNodes.containsKey(nodeId)) {
            return parsedNodes.get(nodeId);
        }

        // Parse node and store
        ClavaNode parsedNode = parseWorker(nodeId);
        parsedNodes.put(nodeId, parsedNode);

        return parsedNode;
    }

    private ClavaNode parseWorker(String nodeId) {

        // Get classname
        String classname = data.get(IdToClassnameParser.getDataKey()).get(nodeId);

        if (classname == null) {
            SpecsLogs.msgInfo("No classname for node '" + nodeId + "");
            return new UnsupportedNode("<CLASSNAME NOT FOUND>", ClavaData.empty(), Collections.emptyList());
        }

        // Get corresponding ClavaNode class
        Class<? extends ClavaNode> clavaNodeClass = classesService.getClass(classname);

        // Map classname to ClavaData class
        Class<? extends ClavaData> clavaDataClass = ClavaNodeToData.getClavaDataClass(clavaNodeClass);

        if (clavaDataClass == null) {
            SpecsLogs.msgInfo("No ClavaData class (specific or base) for node '" + nodeId + "'. Add mapping for class '"
                    + classname + "' in Java class '"
                    + ClavaNodeToData.class.getSimpleName() + "'");
            return new UnsupportedNode(classname, ClavaData.empty(), Collections.emptyList());
        }

        if (!data.hasValue(ClavaDataParser.getDataKey(clavaDataClass))) {
            SpecsLogs.msgInfo("No parsed information for class '" + classname + "', missing entry in "
                    + ClavaDataParser.class.getSimpleName() + "? (node '" + nodeId
                    + ", expected ClavaData '" + clavaDataClass.getSimpleName() + "')");
            return new UnsupportedNode(classname, ClavaData.empty(), Collections.emptyList());
        }

        // Get ClavaData
        ClavaData clavaData = data.get(ClavaDataParser.getDataKey(clavaDataClass)).get(nodeId);

        if (clavaData == null) {
            SpecsLogs.msgInfo("No ClavaData for node '" + nodeId + "' (classname: " + classname + ", ClavaData Class: "
                    + clavaDataClass.getSimpleName() + "). ");
            return new UnsupportedNode(classname, ClavaData.empty(), Collections.emptyList());
        }

        // Get children ids
        List<String> childrenIds = data.get(VisitedChildrenParser.getDataKey()).get(nodeId);

        if (childrenIds == null) {
            SpecsLogs.msgInfo("No children for node '" + nodeId + "' (" + classname + ")");
            return new UnsupportedNode(classname, clavaData, Collections.emptyList());
        }

        // Parse each children
        List<ClavaNode> children = childrenIds.stream()
                .map(childId -> parse(childId))
                .collect(Collectors.toList());

        // Get ClavaNode constructor
        BiFunction<ClavaData, List<ClavaNode>, ClavaNode> builder = classesService.getBuilder(clavaNodeClass,
                clavaDataClass);

        if (builder == null) {
            SpecsLogs.msgInfo("No builder for node '" + nodeId + "' (" + classname + ")");
            return new UnsupportedNode(classname, clavaData, children);
        }

        // Build node based on data and children
        return builder.apply(clavaData, children);
    }

    private App createApp(Collection<? extends ClavaNode> topLevelDecls) {
        System.out.println("TOP LEVEL NODES:" + topLevelDecls);
        // Each node belongs to a file, maintain a map of what nodes belong to each file,
        // in the end create the file nodes and add them to the App node

        // Using LinkedHashMap to maintain order of keys
        MultiMap<String, Decl> declarations = new MultiMap<>(() -> new LinkedHashMap<>());

        // There can be repeated top level declarations, with different values for the addresses.
        // Because the parser uses the addresses as ids, it can be problematic if declarations with mismatched addresses
        // appear in the final AST tree.
        //
        // To solve this, use the location of the node to remove repetitions,
        // and create a map between repeated ids and normalized ids

        NormalizedNodes normalizedNodes = NormalizedNodes.newInstance(topLevelDecls);

        for (ClavaNode clavaNode : normalizedNodes.getUniqueNodes()) {

            // Normalize node source path
            String filepath = clavaNode.getLocation().getFilepath();
            if (filepath == null) {
                SpecsLogs.msgWarn("Filepath null, check if ok. Skipping node:\n" + clavaNode);
                continue;
            }

            String canonicalPath = SpecsIo.getCanonicalPath(new File(filepath));

            // If UnsupportedNode, transform to DummyDecl
            if (clavaNode instanceof UnsupportedNode) {
                UnsupportedNode unsupportedNode = (UnsupportedNode) clavaNode;
                clavaNode = new DummyDecl(unsupportedNode.getClassname(), (DeclDataV2) unsupportedNode.getData(),
                        unsupportedNode.getChildren());
                // clavaNode = ClavaNodeFactory.dummyDecl(clavaNode);
            }

            if (!(clavaNode instanceof Decl)) {
                throw new RuntimeException(
                        "Expecting a DeclNode, found a '" + clavaNode.getClass().getSimpleName() + "'");
            }

            Decl decl = (Decl) clavaNode;

            declarations.put(canonicalPath, decl);
        }

        System.out.println("DECLS:" + declarations);

        /*
        
        
        
        
        
        
        // For each enty in MultiMap, create a Translation Unit
        ClangIncludes includes = node.getClangRoot().getIncludes();
        
        List<TranslationUnit> tUnits = new ArrayList<>();
        for (String path : declarations.keySet()) {
            // Set<Decl> decls = new LinkedHashSet<>();
            List<Decl> decls = new ArrayList<>();
        
            // Build filename
            String filename = new File(path).getName();
            int endIndex = path.length() - filename.length();
            String filenamePath = path.substring(0, endIndex);
        
            // Declaration nodes of the translation unit
            List<Decl> declNodes = declarations.get(path);
        
        
            // Remove ParmVarDecl nodes
            declNodes = declNodes.stream()
                    .filter(decl -> !(decl instanceof ParmVarDecl))
                    .collect(Collectors.toList());
        
            // Add includes
            List<Include> includeList = includes.getIncludes(new File(path));
        
            List<Include> uniqueIncludes = SpecsCollections.filter(includeList, include -> include.toString());
        
            // Set<String> addedIncludes = new HashSet<>();
        
            // Only add includes that are not in the line number range of the declarations
            if (!uniqueIncludes.isEmpty()) {
                Set<Integer> lineNumbers = getLineNumbers(declNodes);
        
                for (Include include : uniqueIncludes) {
        
                    // Only add include if line number of the include is not contained in declaration numbers
                    if (lineNumbers.contains(include.getLine())) {
                        continue;
                    }
        
                    // Check if include was not already added
                    // if (addedIncludes.contains(include.toString())) {
                    // continue;
                    // }
                    //
                    // addedIncludes.add(include.toString());
                    decls.add(ClavaNodeFactory.include(include, path));
                }
            }
        
            // Add declarations
            decls.addAll(declNodes);
        
            TranslationUnit tUnit = ClavaNodeFactory.translationUnit(filename, filenamePath, decls);
        
            // Clean translation unit
            ClavaPostProcessing.applyPostPasses(tUnit);
        
            tUnits.add(tUnit);
        }
        
        // Create App
        
        App app = ClavaNodeFactory.app(tUnits);
        
        app.setIdsAlias(normalizedClangNodes.getRepeatedIdsMap());
        
        */
        return null;
    }

}

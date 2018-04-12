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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clang.clavaparser.ClavaPostProcessing;
import pt.up.fe.specs.clang.parsers.ClavaDataParser;
import pt.up.fe.specs.clang.parsers.IdToClassnameParser;
import pt.up.fe.specs.clang.parsers.IncludesParser;
import pt.up.fe.specs.clang.parsers.TopLevelNodesParser;
import pt.up.fe.specs.clang.parsers.TopLevelTypesParser;
import pt.up.fe.specs.clang.parsers.VisitedChildrenParser;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.Include;
import pt.up.fe.specs.clava.ast.ClavaData;
import pt.up.fe.specs.clava.ast.ClavaDataPostProcessing;
import pt.up.fe.specs.clava.ast.ClavaDataUtils;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.DummyDecl;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.decl.data2.DeclDataV2;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.extra.UnsupportedNode;
import pt.up.fe.specs.util.SpecsCollections;
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

    public App parse() {
        // Get top-level nodes
        Set<String> topLevelDecls = data.get(TopLevelNodesParser.getDataKey());
        // MultiMap<String, String> topLevelDecls = data.get(TopLevelNodesParser.getDataKey());
        Set<String> topLevelTypes = data.get(TopLevelTypesParser.getDataKey());
        // Separate into translation units?

        // Parse top-level decls
        // List<ClavaNode> topLevelDeclNodes = topLevelDecls.flatValues().stream()
        // .map(this::parse)
        // .collect(Collectors.toList());
        List<ClavaNode> topLevelDeclNodes = new ArrayList<>();
        // for (String topLevelDeclId : topLevelDecls.flatValues()) {
        for (String topLevelDeclId : topLevelDecls) {
            ClavaNode parsedNode = parse(topLevelDeclId);

            // Check
            topLevelDeclNodes.add(parsedNode);
        }

        // Parse top-level types
        topLevelTypes.stream().forEach(this::parse);

        // After all ClavaNodes are created, apply post-processing
        ClavaDataPostProcessing postData = new ClavaDataPostProcessing(parsedNodes);
        parsedNodes.values().stream()
                .forEach(node -> ClavaDataUtils.applyPostProcessing(node.getData(), postData));

        // Create App node
        App app = createApp(topLevelDeclNodes, data.get(IncludesParser.getDataKey()));

        // Set app in Type nodes
        /*
        for (String topLevelTypeId : topLevelTypes) {
            ClavaNode topLevelType = parsedNodes.get(topLevelTypeId);
            Preconditions.checkNotNull(topLevelType);
        
            topLevelType.getDescendantsAndSelfStream()
                    .filter(Type.class::isInstance)
                    .map(Type.class::cast)
                    .forEach(type -> type.setApp(app));
        }
        */

        return app;

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

        // Get ClavaData mapped to the node id
        ClavaData clavaData = data.get(ClavaDataParser.getDataKey()).get(nodeId);

        if (clavaData == null) {
            SpecsLogs.msgInfo("No ClavaData for node '" + nodeId + "' (classname: " + classname
                    + "), data dumper is not being called");
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
                clavaData.getClass());

        if (builder == null) {
            SpecsLogs.msgInfo("No builder for node '" + nodeId + "', missing constructor 'new " + classname + "("
                    + clavaData.getClass().getSimpleName() + " data, Collection<? extends ClavaNode> children)'");
            return new UnsupportedNode(classname, clavaData, children);
        }

        // Build node based on data and children
        return builder.apply(clavaData, children);
    }

    /*
    private App createAppV2(MultiMap<String, String> topLevelDecls, List<Include> includes) {
        // System.out.println("TOP LEVEL DECLS:" + topLevelDecls);
        // Each id represents a translation unit
    
        //
        // return null;
    
        // There can be repeated top level declarations, e.g., if they come from the same include.
        //
        // To solve this, use the location of the node to remove repetitions,
        // and create a map between repeated ids and normalized ids
    
        List<ClavaNode> topLevelDeclNodes = topLevelDecls.valuesFlat().stream()
                .map(id -> parsedNodes.get(id))
                .collect(Collectors.toList());
    
        NormalizedNodes normalizedNodes = NormalizedNodes.newInstance(topLevelDeclNodes);
    
        // Using LinkedHashMap to maintain order of keys
        MultiMap<String, Decl> fileDeclMap = new MultiMap<>(() -> new LinkedHashMap<>());
    
        for (ClavaNode clavaNode : normalizedNodes.getUniqueNodes()) {
    
            // Normalize node source path
            String filepath = clavaNode.getLocation().getFilepath();
    
            // If no filepath, determine using
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
    
            fileDeclMap.put(canonicalPath, decl);
        }
    
        // Create includes map
        MultiMap<String, Include> includesMap = new MultiMap<>();
        includes.stream()
                .forEach(include -> includesMap.put(SpecsIo.getCanonicalPath(include.getSourceFile()), include));
    
        // For each enty in MultiMap, create a Translation Unit
        List<TranslationUnit> tUnits = new ArrayList<>();
        for (String path : fileDeclMap.keySet()) {
            // Set<Decl> decls = new LinkedHashSet<>();
            List<Decl> decls = new ArrayList<>();
    
            // Build filename
            String filename = new File(path).getName();
            int endIndex = path.length() - filename.length();
            String filenamePath = path.substring(0, endIndex);
    
            // Declaration nodes of the translation unit
            List<Decl> declNodes = fileDeclMap.get(path);
    
            // Remove ParmVarDecl nodes
            declNodes = declNodes.stream()
                    .filter(decl -> !(decl instanceof ParmVarDecl))
                    .collect(Collectors.toList());
    
            // Get corresponding includes
            File declFile = new File(path);
            List<Include> sourceIncludes = includesMap.get(SpecsIo.getCanonicalPath(declFile));
    
            if (sourceIncludes == null) {
                throw new RuntimeException("Could not find includes for source file '" + declFile + "'");
            }
    
            List<Include> uniqueIncludes = SpecsCollections.filter(sourceIncludes, include -> include.toString());
    
            // Only add includes that are not in the line number range of the declarations
            if (!uniqueIncludes.isEmpty()) {
                Set<Integer> lineNumbers = getLineNumbers(declNodes);
    
                for (Include include : uniqueIncludes) {
    
                    // Only add include if line number of the include is not contained in declaration numbers
                    if (lineNumbers.contains(include.getLine())) {
                        continue;
                    }
    
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
    
        App app = ClavaNodeFactory.app(tUnits);
    
        app.setIdsAlias(normalizedNodes.getRepeatedIdsMap());
    
        return app;
    
    }
    */

    private App createApp(Collection<? extends ClavaNode> topLevelDecls, List<Include> includes) {
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

        // Create includes map
        MultiMap<String, Include> includesMap = new MultiMap<>();
        includes.stream()
                .forEach(include -> includesMap.put(SpecsIo.getCanonicalPath(include.getSourceFile()), include));

        // For each enty in MultiMap, create a Translation Unit
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

            // Get corresponding includes
            File declFile = new File(path);
            List<Include> sourceIncludes = includesMap.get(SpecsIo.getCanonicalPath(declFile));

            if (sourceIncludes == null) {
                throw new RuntimeException("Could not find includes for source file '" + declFile + "'");
            }

            List<Include> uniqueIncludes = SpecsCollections.filter(sourceIncludes, include -> include.toString());

            // Only add includes that are not in the line number range of the declarations
            if (!uniqueIncludes.isEmpty()) {
                Set<Integer> lineNumbers = getLineNumbers(declNodes);

                for (Include include : uniqueIncludes) {

                    // Only add include if line number of the include is not contained in declaration numbers
                    if (lineNumbers.contains(include.getLine())) {
                        continue;
                    }

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

        App app = ClavaNodeFactory.app(tUnits);

        app.setIdsAlias(normalizedNodes.getRepeatedIdsMap());

        return app;
    }

    private static <N extends ClavaNode> Set<Integer> getLineNumbers(List<N> nodes) {
        Set<Integer> lineNumbers = new HashSet<>();

        for (ClavaNode node : nodes) {
            int startLine = node.getLocation().getStartLine();
            int endLine = node.getLocation().getEndLine();

            IntStream.range(startLine, endLine + 1).forEach(index -> lineNumbers.add(index));
        }

        return lineNumbers;
    }

}

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

package pt.up.fe.specs.clang.clavaparser.extra;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.ast.genericnode.ClangRootNode;
import pt.up.fe.specs.clang.clavaparser.AClangNodeParser;
import pt.up.fe.specs.clang.clavaparser.ClangConverterTable;
import pt.up.fe.specs.clang.includes.ClangIncludes;
import pt.up.fe.specs.clang.parsers.ClangParserData;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.Include;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.LegacyToDataStore;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.extra.Undefined;
import pt.up.fe.specs.clava.ast.extra.data.Language;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.collections.MultiMap;
import pt.up.fe.specs.util.stringparser.StringParser;

public class RootParser extends AClangNodeParser<App> {

    public RootParser(ClangConverterTable converter) {
        super(converter, false);
    }

    @Override
    public App parse(ClangNode node) {
        return (App) super.parse(node);
    }

    /**
     * Only accepts nodes of type ClangRootNode.
     */
    @Override
    public App parse(ClangNode node, StringParser parser) {

        if (!(node instanceof ClangRootNode)) {
            throw new RuntimeException(
                    "This method only works with Clang nodes of type '" + ClangRootNode.getRootName() + "'");
        }

        // Parse all children, include them in a Clava App node
        //
        // Each child of the root node belongs to a file, maintain a map of what nodes belong to each file, in the end
        // create the file nodes and add them to a root node

        // Using LinkedHashMap to maintain order of keys
        MultiMap<String, Decl> declarations = new MultiMap<>(() -> new LinkedHashMap<>());

        // There can be repeated top level declarations, with different values for the addresses.
        // Because the parser uses the addresses as ids, it can be problematic if declarations with mismatched addresses
        // appear in the final AST tree.
        //
        // To solve this, use the location of the node to remove repetitions,
        // and create a map between repeated ids and normalized ids

        NormalizedClangNodes normalizedClangNodes = normalizeClangNodes((ClangRootNode) node);

        // System.out.println("REPEATED IDS MAP:\n" + normalizedClangNodes.getRepeatedIdsMap());
        // List<ClangNode> uniqueChildren = SpecsCollections.filter(node.getChildren(),
        // child -> child.getLocation().toString());

        // return elements.stream()
        // // Add to set. If already in set, will return false and filter the node
        // .filter(element -> seenElements.add(mapFunction.apply(element)))
        // // .filter(element -> {
        // // mapFunction.apply(element);
        // // return true;
        // // })
        // .collect(Collectors.toList());

        for (ClangNode child : normalizedClangNodes.getUniqueNodes()) {
            // Normalize node source path
            String filepath = child.getLocation().getFilepath();
            if (filepath == null) {
                // throw new RuntimeException("Filepath null, check if ok");
                SpecsLogs.msgWarn("Filepath null, check if ok. Skipping node:\n" + child.getContent());
                continue;
            }

            String canonicalPath = SpecsIo.getCanonicalPath(new File(filepath));

            // Parse node
            ClavaNode clavaNode = getConverter().parse(child);

            // If Undefined, transform to DummyDecl
            if (clavaNode instanceof Undefined) {
                clavaNode = LegacyToDataStore.getFactory().dummyDecl(clavaNode);
            }

            if (!(clavaNode instanceof Decl)) {
                throw new RuntimeException(
                        "Expecting a DeclNode, found a '" + clavaNode.getClass().getSimpleName() + "'");
            }

            Decl decl = (Decl) clavaNode;

            declarations.put(canonicalPath, decl);
        }

        double newNodesFraction = (double) getConverter().getNewParsedNodes() / getConverter().getTotalParsedNodes();
        SpecsLogs.msgInfo("New nodes ratio: " + SpecsStrings.toPercentage(newNodesFraction) + " (from a total of "
                + getConverter().getTotalParsedNodes() + " parsed nodes)");

        // For each enty in MultiMap, create a Translation Unit
        ClangIncludes includes = node.getClangRoot().getIncludes();

        List<TranslationUnit> tUnits = new ArrayList<>();
        for (String path : declarations.keySet()) {
            // Set<Decl> decls = new LinkedHashSet<>();
            List<Decl> decls = new ArrayList<>();

            // Build filename
            File sourcePath = new File(path);
            // String filename = sourcePath.getName();
            // int endIndex = path.length() - filename.length();
            // String filenamePath = path.substring(0, endIndex);

            // Declaration nodes of the translation unit
            List<Decl> declNodes = declarations.get(path);

            // Remove ParmVarDecl nodes
            declNodes = declNodes.stream()
                    .filter(decl -> !(decl instanceof ParmVarDecl))
                    .collect(Collectors.toList());

            // Add includes
            List<Include> includeList = includes.getIncludes(new File(path));

            List<Include> uniqueIncludes = SpecsCollections.filter(includeList, include -> include.toString());

            // Add includes
            uniqueIncludes.stream()
                    .map(include -> ClavaNodeFactory.include(include, path))
                    .forEach(decls::add);
            // Add declarations
            decls.addAll(declNodes);

            // TranslationUnit tUnit = ClavaNodeFactory.translationUnit(filename, filenamePath, decls);
            // TranslationUnit tUnit = ClavaNodeFactory.translationUnit(sourcePath, decls);
            TranslationUnit tUnit = LegacyToDataStore.getFactory().translationUnit(sourcePath, decls);

            Language language = getStdErr().get(ClangParserData.FILE_LANGUAGE_DATA)
                    // .get(new File(filenamePath, filename));
                    .get(sourcePath);
            if (language != null) {
                tUnit.setLanguage(language);
            }
            // Clean translation unit
            // ClavaPostProcessing.applyPostPasses(tUnit);

            tUnits.add(tUnit);
        }

        // Create App
        // App app = ClavaNodeFactory.app(tUnits);
        App app = getConfig()
                .get(ClavaNode.CONTEXT)
                .get(ClavaContext.FACTORY)
                .app(tUnits);

        app.setIdsAlias(normalizedClangNodes.getRepeatedIdsMap());

        return app;
    }

    private NormalizedClangNodes normalizeClangNodes(ClangRootNode rootNode) {
        Map<String, String> fileLocationToId = new HashMap<>();

        List<ClangNode> uniqueNodes = new ArrayList<>();
        Map<String, String> repeatedIdsMap = new HashMap<>();
        // System.out.println("CONFIG:" + rootNode.getConfig());
        rootNode.getChildrenStream().forEach(node -> {
            // System.out.println("NODE ID:" + node.getExtendedId());
            // Convert node to a string based on location
            String locationString = node.getLocation().toString();
            // System.out.println("LOCATION:" + locationString);
            String normalizedId = fileLocationToId.get(locationString);
            // System.out.println("normalizedId:" + normalizedId);

            // If location is not mapped to a normalized node yet, add to map;
            if (normalizedId == null) {
                // fileLocationToId.put(locationString, node.getExtendedIdTry().orElse(DUMMY_ID));
                fileLocationToId.put(locationString, node.getExtendedId());
                uniqueNodes.add(node);
                return;
            }

            // // Using object comparison, it should be safe since we are using the same static object
            // if (normalizedId == DUMMY_ID) {
            // // Most likely a type
            // SpecsLogs.msgInfo("Repeated node without id: " + node);
            // SpecsLogs.msgInfo("Location: " + node.getLocation().toString());
            // SpecsLogs.msgInfo("ID: " + node.getExtendedId());
            // }

            // Otherwise, map node to already normalized node in the table
            repeatedIdsMap.put(node.getExtendedId(), normalizedId);
        });

        return new NormalizedClangNodes(uniqueNodes, repeatedIdsMap);
    }

    // private static <N extends ClavaNode> Set<Integer> getLineNumbers(List<N> nodes) {
    // Set<Integer> lineNumbers = new HashSet<>();
    //
    // for (ClavaNode node : nodes) {
    // int startLine = node.getLocation().getStartLine();
    // int endLine = node.getLocation().getEndLine();
    //
    // IntStream.range(startLine, endLine + 1).forEach(index -> lineNumbers.add(index));
    // }
    //
    // return lineNumbers;
    // }

    public Set<String> getUndefinedNodes() {
        return ClangConverterTable.getUndefinedNodes();
    }

    public TranslationUnit parseTranslationUnit(ClangRootNode clangAst, File sourceFile) {
        // Create StringParser from node content
        StringParser parser = new StringParser(clangAst.getContentTry().orElse("").trim());

        // Store original string of parser, for debugging
        String originalContent = parser.toString();

        // Parse node
        TranslationUnit clavaNode = parseTranslationUnit(clangAst, parser, sourceFile);

        if (!parser.toString().trim().isEmpty()) {
            throw new RuntimeException("ClangNodeParser '" + getClass().getSimpleName()
                    + "' did not consume StringParser completely, current string is '" + parser
                    + "'.\nOriginal string was:" + originalContent + "\nLocation:" + clangAst.getLocation());
        }

        // Mark node
        if (isLegacyParser()) {
            clavaNode.setIsLegacyNode(true);
        }

        return clavaNode;
    }

    /**
     * Only accepts nodes of type ClangRootNode.
     */
    public TranslationUnit parseTranslationUnit(ClangRootNode node, StringParser parser, File sourceFile) {

        // For each enty in MultiMap, create a Translation Unit
        ClangIncludes includes = node.getClangRoot().getIncludes();

        List<Decl> tuDecls = new ArrayList<>();
        // File canonicalSourceFile = SpecsIo.getCanonicalFile(sourceFile);

        // Add includes
        List<Include> includeList = includes.getIncludes(sourceFile);

        List<Include> uniqueIncludes = SpecsCollections.filter(includeList, include -> include.toString());

        // Set<String> addedIncludes = new HashSet<>();

        // Add includes
        uniqueIncludes.stream()
                .map(include -> ClavaNodeFactory.include(include, null)) // PATH?
                .forEach(tuDecls::add);

        // If either the file in start location or end location is the current file, then it belongs to the translation
        // unit
        for (ClangNode child : node.getChildren()) {
            if (!sourceFile.equals(child.getLocation().getStartFile())
                    && !sourceFile.equals(child.getLocation().getEndFile())) {
                // System.out.println("Discarding node " + child.getLocation());

                continue;
            }

            // Parse node
            ClavaNode clavaNode = getConverter().parse(child);

            // If Undefined, transform to DummyDecl
            if (clavaNode instanceof Undefined) {
                clavaNode = LegacyToDataStore.getFactory().dummyDecl(clavaNode);
            }

            if (!(clavaNode instanceof Decl)) {
                throw new RuntimeException(
                        "Expecting a DeclNode, found a '" + clavaNode.getClass().getSimpleName() + "'");
            }

            Decl decl = (Decl) clavaNode;
            tuDecls.add(decl);
        }

        // LegacyToDataStore.getFactory().
        // return new TranslationUnit(sourceFile.getName(), sourceFile.getParent(), tuDecls);
        // return new TranslationUnit(sourceFile, tuDecls);
        return LegacyToDataStore.getFactory().translationUnit(sourceFile, tuDecls);
        /*
        
        // Parse all children, include them in a Clava App node
        //
        // Each child of the root node belongs to a file, maintain a map of what nodes belong to each file, in the end
        // create the file nodes and add them to a root node
        
        // Using LinkedHashMap to maintain order of keys
        MultiMap<String, Decl> declarations = new MultiMap<>(() -> new LinkedHashMap<>());
        
        // There can be repeated top level declarations, with different values for the addresses.
        // Because the parser uses the addresses as ids, it can be problematic if declarations with mismatched addresses
        // appear in the final AST tree.
        //
        // To solve this, use the location of the node to remove repetitions,
        // and create a map between repeated ids and normalized ids
        
        NormalizedClangNodes normalizedClangNodes = normalizeClangNodes((ClangRootNode) node);
        
        for (ClangNode child : normalizedClangNodes.getUniqueNodes()) {
            // Normalize node source path
            String filepath = child.getLocation().getFilepath();
            if (filepath == null) {
                // throw new RuntimeException("Filepath null, check if ok");
                SpecsLogs.msgWarn("Filepath null, check if ok. Skipping node:\n" + child.getContent());
                continue;
            }
        
            String canonicalPath = SpecsIo.getCanonicalPath(new File(filepath));
        
            // Parse node
            ClavaNode clavaNode = getConverter().parse(child);
        
            // If Undefined, transform to DummyDecl
            if (clavaNode instanceof Undefined) {
                clavaNode = LegacyToDataStore.getFactory().dummyDecl(clavaNode);
            }
        
            if (!(clavaNode instanceof Decl)) {
                throw new RuntimeException(
                        "Expecting a DeclNode, found a '" + clavaNode.getClass().getSimpleName() + "'");
            }
        
            Decl decl = (Decl) clavaNode;
        
            declarations.put(canonicalPath, decl);
        }
        
        double newNodesFraction = (double) getConverter().getNewParsedNodes() / getConverter().getTotalParsedNodes();
        SpecsLogs.msgInfo("New nodes ratio: " + SpecsStrings.toPercentage(newNodesFraction) + " (from a total of "
                + getConverter().getTotalParsedNodes() + " parsed nodes)");
        
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
        
            // Add includes
            uniqueIncludes.stream()
                    .map(include -> ClavaNodeFactory.include(include, path))
                    .forEach(decls::add);
        
            // Add declarations
            decls.addAll(declNodes);
        
            TranslationUnit tUnit = ClavaNodeFactory.translationUnit(filename, filenamePath, decls);
        
            Language language = getStdErr().get(ClangParserKeys.FILE_LANGUAGE_DATA)
                    .get(new File(filenamePath, filename));
            if (language != null) {
                tUnit.setLanguage(language);
            }
            // Clean translation unit
            // ClavaPostProcessing.applyPostPasses(tUnit);
        
            tUnits.add(tUnit);
        }
        
        // Create App
        // App app = ClavaNodeFactory.app(tUnits);
        App app = getConfig()
                .get(ClavaNode.CONTEXT)
                .get(ClavaContext.FACTORY)
                .app(tUnits);
        
        app.setIdsAlias(normalizedClangNodes.getRepeatedIdsMap());
        
        return app;
        */
    }
}

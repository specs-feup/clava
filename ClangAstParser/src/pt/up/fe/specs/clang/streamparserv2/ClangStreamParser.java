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
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.parsers.ClangParserData;
import pt.up.fe.specs.clang.parsers.ClavaNodes;
import pt.up.fe.specs.clang.transforms.CreateDeclStmts;
import pt.up.fe.specs.clang.transforms.DeleteTemplateSpecializations;
import pt.up.fe.specs.clang.transforms.DenanonymizeDecls;
import pt.up.fe.specs.clang.transforms.MoveImplicitCasts;
import pt.up.fe.specs.clang.transforms.RemoveExtraNodes;
import pt.up.fe.specs.clang.transforms.TreeTransformer;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaRule;
import pt.up.fe.specs.clava.Include;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.extra.data.Language;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.context.ClavaFactory;
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
    private final static Collection<ClavaRule> POST_PARSING_RULES = Arrays.asList(
            new DenanonymizeDecls(),

            new DeleteTemplateSpecializations(),
            new RemoveExtraNodes(),
            // new RemoveClangComments(),
            new CreateDeclStmts(),
            new MoveImplicitCasts()

    // new AdaptBoolTypes(),
    // new AdaptBoolCasts(),
    // new RemoveBoolOperatorCalls(),
    // new ReplaceClangLabelStmt(),
    // new RemoveDefaultInitializers(),
    // new RemoveImplicitConstructors(),
    // new RecoverStdMacros(),
    );

    private final ClangParserData data;

    public ClangStreamParser(ClangParserData data, boolean debug) {
        this.data = data;
    }

    private ClavaFactory getFactory() {
        return data.get(ClavaNode.CONTEXT).get(ClavaContext.FACTORY);
    }

    public static Collection<ClavaRule> getPostParsingRules() {
        return POST_PARSING_RULES;
    }

    public App parse() {
        // Get top-level nodes
        Set<String> topLevelDecls = data.get(ClangParserData.TOP_LEVEL_DECL_IDS);
        // MultiMap<String, String> topLevelDecls = data.get(TopLevelNodesParser.getDataKey());
        Set<String> topLevelTypes = data.get(ClangParserData.TOP_LEVEL_TYPE_IDS);
        Set<String> topLevelAttributes = data.get(ClangParserData.TOP_LEVEL_ATTR_IDS);

        // Set<String> allDecls = data.get(ClangParserKeys.ALL_DECLS_IDS);
        // System.out.println("ALL DECLS:" + allDecls);
        // Separate into translation units?

        // Parse top-level decls
        // List<ClavaNode> topLevelDeclNodes = topLevelDecls.flatValues().stream()
        // .map(this::parse)
        // .collect(Collectors.toList());
        List<ClavaNode> topLevelDeclNodes = new ArrayList<>();
        // for (String topLevelDeclId : topLevelDecls.flatValues()) {
        for (String topLevelDeclId : topLevelDecls) {
            ClavaNode parsedNode = data.get(ClangParserData.CLAVA_NODES).get(topLevelDeclId);
            Preconditions.checkNotNull(parsedNode, "No node for decl '" + topLevelDeclId + "'");
            // Check
            topLevelDeclNodes.add(parsedNode);
        }

        // Parse top-level types
        for (String topLevelTypeId : topLevelTypes) {
            if (ClavaNodes.isNullId(topLevelTypeId)) {
                continue;
            }
            ClavaNode parsedNode = data.get(ClangParserData.CLAVA_NODES).get(topLevelTypeId);
            Preconditions.checkNotNull(parsedNode, "No node for type '" + topLevelTypeId + "'");

        }

        // Parse top-level attributes
        for (String topLevelAttributeId : topLevelAttributes) {
            ClavaNode parsedNode = data.get(ClangParserData.CLAVA_NODES).get(topLevelAttributeId);
            Preconditions.checkNotNull(parsedNode, "No node for attribute '" + topLevelAttributeId + "'");
        }

        // topLevelTypes.stream().forEach(this::parse);

        // Parse top-level attributes
        // topLevelAttributes.stream().forEach(this::parse);

        // After all ClavaNodes are created, apply post-processing
        // Map<String, ClavaNode> parsedNodes = data.get(ClangParserKeys.CLAVA_NODES);
        // ClavaDataPostProcessing postData = new ClavaDataPostProcessing(parsedNodes);
        // parsedNodes.values().stream()
        // .forEach(node -> ClavaDataUtils.applyPostProcessing(node.getData(), postData));

        // Create App node
        App app = createApp(topLevelDeclNodes, data.get(ClangParserData.INCLUDES));

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

        // TODO: Currently disabled, to avoid conflicts with legacy parser
        // Add text elements (comments, pragmas) to the tree
        // new TextParser(app.getContext()).addElements(app);

        // Applies several passes to make the tree resemble more the original code, e.g., remove implicit nodes from
        // original clang tree
        new TreeTransformer(POST_PARSING_RULES).transform(app);

        return app;

    }

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
            // if (clavaNode instanceof UnsupportedNode) {
            // // throw new RuntimeException("ADASDASD");
            // UnsupportedNode unsupportedNode = (UnsupportedNode) clavaNode;
            // DummyDeclData dummyData = new DummyDeclData(unsupportedNode.getClassname(),
            // (DeclDataV2) unsupportedNode.getData());
            //
            // clavaNode = new DummyDecl(dummyData, unsupportedNode.getChildren());
            // }

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
            File sourcePath = new File(path);
            // String filename = new File(path).getName();
            // int endIndex = path.length() - filename.length();
            // String filenamePath = path.substring(0, endIndex);

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

            // Add includes
            uniqueIncludes.stream()
                    // .map(include -> ClavaNodeFactory.include(include, path))
                    .map(include -> getFactory().includeDecl(include, path))
                    .forEach(decls::add);

            // Add declarations
            decls.addAll(declNodes);

            // TranslationUnit tUnit = ClavaNodeFactory.translationUnit(filename, filenamePath, decls);
            // TranslationUnit tUnit = ClavaNodeFactory.translationUnit(sourcePath, decls);
            TranslationUnit tUnit = getFactory().translationUnit(sourcePath, decls);

            // Language language = data.get(ClangParserKeys.FILE_LANGUAGE_DATA).get(new File(filenamePath, filename));
            Language language = data.get(ClangParserData.FILE_LANGUAGE_DATA).get(sourcePath);
            if (language != null) {
                tUnit.setLanguage(language);
            }
            // Clean translation unit
            // ClavaPostProcessing.applyPostPasses(tUnit);

            tUnits.add(tUnit);
        }

        // App app = ClavaNodeFactory.app(tUnits);
        // App app = data.get(ClavaNode.CONTEXT).get(ClavaContext.FACTORY).app(tUnits);
        App app = getFactory().app(tUnits);

        app.setIdsAlias(normalizedNodes.getRepeatedIdsMap());

        return app;
    }

    public TranslationUnit parseTu(File sourceFile) {
        // Get top-level nodes
        Set<String> topLevelDecls = data.get(ClangParserData.TOP_LEVEL_DECL_IDS);
        Set<String> topLevelTypes = data.get(ClangParserData.TOP_LEVEL_TYPE_IDS);
        Set<String> topLevelAttributes = data.get(ClangParserData.TOP_LEVEL_ATTR_IDS);

        // Parse top-level decls

        List<ClavaNode> topLevelDeclNodes = new ArrayList<>();
        // for (String topLevelDeclId : topLevelDecls.flatValues()) {
        for (String topLevelDeclId : topLevelDecls) {
            ClavaNode parsedNode = data.get(ClangParserData.CLAVA_NODES).get(topLevelDeclId);
            Preconditions.checkNotNull(parsedNode, "No node for decl '" + topLevelDeclId + "'");
            // Check
            topLevelDeclNodes.add(parsedNode);
        }

        // Parse top-level types
        for (String topLevelTypeId : topLevelTypes) {
            if (ClavaNodes.isNullId(topLevelTypeId)) {
                continue;
            }
            ClavaNode parsedNode = data.get(ClangParserData.CLAVA_NODES).get(topLevelTypeId);
            Preconditions.checkNotNull(parsedNode, "No node for type '" + topLevelTypeId + "'");

        }

        // Parse top-level attributes
        for (String topLevelAttributeId : topLevelAttributes) {
            ClavaNode parsedNode = data.get(ClangParserData.CLAVA_NODES).get(topLevelAttributeId);
            Preconditions.checkNotNull(parsedNode, "No node for attribute '" + topLevelAttributeId + "'");
        }

        // Create TU node
        return createTu(sourceFile, topLevelDeclNodes, data.get(ClangParserData.INCLUDES));

    }

    private TranslationUnit createTu(File sourceFile, Collection<? extends ClavaNode> topLevelDecls,
            List<Include> includes) {
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
        // if (declarations.size() > 1) {
        // // Just to check, for now
        // throw new RuntimeException("Declarations size is not one, check:" + declarations.keySet());
        // // ClavaLog.warning("DECLA");
        // }

        // System.out.println("DECLARATION KEYS:");
        // System.out.println(declarations.keySet());
        //
        // System.out.println("DECLARATIONS:");
        // System.out.println(declarations);

        String path = sourceFile.getAbsolutePath();
        if (declarations.size() > 0 && !declarations.containsKey(path)) {
            // Just to check, for now
            throw new RuntimeException("Expeted declarations to have key '" + path + "':" + declarations);
        }

        // Declaration nodes of the translation unit
        List<Decl> declNodes = declarations.get(path);

        // Set<Decl> decls = new LinkedHashSet<>();
        List<Decl> decls = new ArrayList<>();

        // Build filename
        // File sourcePath = new File(path);

        // Declaration nodes of the translation unit
        // List<Decl> declNodes = declarations.get(path);

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

        // Add includes
        uniqueIncludes.stream()
                // .map(include -> ClavaNodeFactory.include(include, path))
                .map(include -> getFactory().includeDecl(include, path))
                .forEach(decls::add);

        // Add declarations
        decls.addAll(declNodes);

        // TranslationUnit tUnit = ClavaNodeFactory.translationUnit(filename, filenamePath, decls);
        // TranslationUnit tUnit = ClavaNodeFactory.translationUnit(sourcePath, decls);
        TranslationUnit tUnit = getFactory().translationUnit(sourceFile, decls);

        // Language language = data.get(ClangParserKeys.FILE_LANGUAGE_DATA).get(new File(filenamePath, filename));
        Language language = data.get(ClangParserData.FILE_LANGUAGE_DATA).get(sourceFile);
        if (language != null) {
            tUnit.setLanguage(language);
        }
        // Clean translation unit
        // ClavaPostProcessing.applyPostPasses(tUnit);

        // tUnits.add(tUnit);

        return tUnit;
    }

}

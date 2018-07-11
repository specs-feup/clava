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

package pt.up.fe.specs.clang.codeparser;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clang.ClangAstParser;
import pt.up.fe.specs.clang.ast.genericnode.ClangRootNode;
import pt.up.fe.specs.clang.clavaparser.ClavaParser;
import pt.up.fe.specs.clang.streamparserv2.ClangDumperParser;
import pt.up.fe.specs.clang.textparser.TextParser;
import pt.up.fe.specs.clang.transforms.TreeTransformer;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.ast.LegacyToDataStore;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.clava.utils.SourceType;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;

/**
 * TODO: Make this class extend ACodeParser
 * 
 * @author JoaoBispo
 *
 */
public class TraditionalCodeParser {

    private static final boolean ONLY_NEW_PARSE_METHOD = false;
    private static final boolean ONLY_PARALLEL_PARSING = false;

    private boolean showClangDump;
    private boolean showClangAst;
    private boolean showClavaAst;
    private boolean showCode;
    private boolean useCustomResources;
    // private boolean disableNewParsingMethod;

    public TraditionalCodeParser() {
        showClangDump = false;
        showClangAst = false;
        showClavaAst = false;
        showCode = false;
        useCustomResources = false;
        // disableNewParsingMethod = false;
    }

    public TraditionalCodeParser setShowClangDump(boolean showClangDump) {
        this.showClangDump = showClangDump;
        return this;
    }

    public TraditionalCodeParser setShowClangAst(boolean showClangAst) {
        this.showClangAst = showClangAst;
        return this;
    }

    public TraditionalCodeParser setShowClavaAst(boolean showClavaAst) {
        this.showClavaAst = showClavaAst;
        return this;
    }

    public TraditionalCodeParser setShowCode(boolean showCode) {
        this.showCode = showCode;
        return this;
    }

    // public CodeParser setDisableNewParsingMethod(boolean disableNewParsingMethod) {
    // this.disableNewParsingMethod = disableNewParsingMethod;
    // return this;
    // }

    public TraditionalCodeParser setUseCustomResources(boolean useCustomResources) {
        this.useCustomResources = useCustomResources;
        return this;
    }

    public App parse(List<File> sources, List<String> compilerOptions) {

        if (ONLY_PARALLEL_PARSING) {
            return new ParallelCodeParser()
                    .set(CodeParser.SHOW_CLANG_AST, showClangAst)
                    .set(CodeParser.SHOW_CLANG_DUMP, showClangDump)
                    .set(CodeParser.SHOW_CLAVA_AST, showClavaAst)
                    .set(CodeParser.SHOW_CODE, showCode)
                    .set(CodeParser.USE_CUSTOM_RESOURCES, useCustomResources)
                    .parse(sources, compilerOptions);
        }

        if (ONLY_NEW_PARSE_METHOD) {
            return parseNewMethod(sources, compilerOptions);
        }

        // Collect implementation files
        Map<String, File> allFiles = SpecsIo.getFileMap(sources, SourceType.getPermittedExtensions());

        List<String> implementationFiles = allFiles.keySet().stream()
                // TEST: HEADER + IMPL
                .filter(SourceType.IMPLEMENTATION::hasExtension)
                .collect(Collectors.toList());

        // Parse files
        // ClangRootNode ast = new ClangAstParser(showClangDump, useCustomResources, disableNewParsingMethod).parse(
        ClangRootNode ast = new ClangAstParser(showClangDump, useCustomResources).parse(
                implementationFiles,
                compilerOptions);

        if (showClangDump) {
            SpecsLogs.msgInfo("Clang Dump:\n" + SpecsIo.read(new File(ClangAstParser.getClangDumpFilename())));
        }

        if (showClangAst) {
            SpecsLogs.msgInfo("Clang AST:\n" + ast);
        }

        // Parse dump information
        try (ClavaParser clavaParser = new ClavaParser(ast)) {
            App clavaAst = clavaParser.parse();
            clavaAst.setSourcesFromStrings(allFiles);
            clavaAst.addConfig(ast.getConfig());

            if (showClavaAst) {
                SpecsLogs.msgInfo("CLAVA AST:\n" + clavaAst.toTree());
            }

            if (showCode) {
                SpecsLogs.msgInfo("Code:\n" + clavaAst.getCode());
            }

            return clavaAst;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private App parseNewMethod(List<File> sources, List<String> compilerOptions) {

        // Collect implementation files
        Map<String, File> allFiles = SpecsIo.getFileMap(sources, SourceType.getPermittedExtensions());

        List<String> implementationFiles = allFiles.keySet().stream()
                .filter(SourceType.IMPLEMENTATION::hasExtension)
                .collect(Collectors.toList());

        // Parse files
        App app = new ClangDumperParser(showClangDump, useCustomResources).parse(implementationFiles,
                compilerOptions);

        if (showClangDump) {
            SpecsLogs.msgInfo("Clang Dump not supported in new parse method");
        }

        if (showClangAst) {
            SpecsLogs.msgInfo("Clang AST not supported in new parse method");
        }

        if (showClavaAst) {
            SpecsLogs.msgInfo("CLAVA AST:\n" + app.toTree());
        }

        if (showCode) {
            SpecsLogs.msgInfo("Code:\n" + app.getCode());
        }

        return app;

    }

    /**
     * 
     * @param sources
     * @param compilerOptions
     * @return
     */
    public App parseParallel(List<File> sources, List<String> compilerOptions) {

        List<File> allSourceFolders = getInputSourceFolders(sources, compilerOptions);
        Map<String, File> allSources = SpecsIo.getFileMap(allSourceFolders, SourceType.getPermittedExtensions());
        // System.out.println("ALL SOURCE FOLDERS:" + allSourceFolders);
        // System.out.println("ALL SOURCES:" + allSources);

        // Map<String, TranslationUnit> tunits = new HashMap<>();
        List<TranslationUnit> tUnits = new ArrayList<>();
        // Parse files, individually
        int id = 1;
        for (Entry<String, File> sourceFile : allSources.entrySet()) {
            DataStore options = ClavaOptions.toDataStore(compilerOptions);

            // Adapt compiler options according to the file
            adaptOptions(options, new File(sourceFile.getKey()));

            ClangRootNode ast = new ClangAstParser(showClangDump, useCustomResources).parse(
                    Arrays.asList(sourceFile.getKey()), options, id);

            // Increment id
            id++;

            if (showClangDump) {
                SpecsLogs.msgInfo("Clang Dump:\n" + SpecsIo.read(new File(ClangAstParser.getClangDumpFilename())));
            }

            if (showClangAst) {
                SpecsLogs.msgInfo("Clang AST:\n" + ast);
            }

            // System.out.println("sourceFile:" + sourceFile.getKey());
            // System.out.println("AST:" + ast);

            // Parse dump information
            try (ClavaParser clavaParser = new ClavaParser(ast)) {
                TranslationUnit tunit = clavaParser.parseTranslationUnit(new File(sourceFile.getKey()));
                // tunits.put(sourceFile.getKey(), tunit);
                tUnits.add(tunit);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }

        App app = LegacyToDataStore.getFactory().app(tUnits);

        app.setSourcesFromStrings(allSources);
        app.addConfig(ClavaOptions.toDataStore(compilerOptions));

        // Add text elements (comments, pragmas) to the tree
        new TextParser(app.getContext()).addElements(app);

        // Applies several passes to make the tree resemble more the original code, e.g., remove implicit nodes from
        // original clang tree
        new TreeTransformer(ClavaParser.getPostParsingRules()).transform(app);

        // Perform second pass over types
        // processTypesSecondPass();

        // Applies several passes to make the tree resemble more the original code, e.g., remove implicit nodes from
        // original clang tree
        // if (sourceTree) {
        // processSourceTree(app);
        // }

        SpecsLogs.msgInfo("--- AST parsing report ---");
        // checkUndefinedNodes(app);

        return app;

        // Create App
        // App app = ClavaNodeFactory.app(tUnits);
        // App app = getConfig()
        // .get(ClavaNode.CONTEXT)
        // .get(ClavaContext.FACTORY)
        // .app(tUnits);
        // setAliasIds
        //
        // App clavaAst = clavaParser.parse();
        // clavaAst.setSourcesFromStrings(allFiles);
        // clavaAst.addConfig(ast.getConfig());

        // if (showClavaAst) {
        // SpecsLogs.msgInfo("CLAVA AST:\n" + clavaAst.toTree());
        // }
        //
        // if (showCode) {
        // SpecsLogs.msgInfo("Code:\n" + clavaAst.getCode());
        // }
        //
        // return clavaAst;
        /*
        
        
        
        if (showClangDump) {
            SpecsLogs.msgInfo("Clang Dump:\n" + SpecsIo.read(new File(ClangAstParser.getClangDumpFilename())));
        }
        
        if (showClangAst) {
            SpecsLogs.msgInfo("Clang AST:\n" + ast);
        }
        
        // Parse dump information
        try (ClavaParser clavaParser = new ClavaParser(ast)) {
            App clavaAst = clavaParser.parse();
            clavaAst.setSourcesFromStrings(allFiles);
            clavaAst.addConfig(ast.getConfig());
        
            if (showClavaAst) {
                SpecsLogs.msgInfo("CLAVA AST:\n" + clavaAst.toTree());
            }
        
            if (showCode) {
                SpecsLogs.msgInfo("Code:\n" + clavaAst.getCode());
            }
        
            return clavaAst;
        
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        */

    }

    private void adaptOptions(DataStore options, File source) {
        // Check if the standard is compatible with the file type
        Standard standard = options.getTry(ClavaOptions.STANDARD).orElse(null);

        // Remove standard if extensions do not match
        if (standard != null) {
            if ((SourceType.isCExtension(SpecsIo.getExtension(source)) && standard.isCxx())
                    || SourceType.isCxxExtension(SpecsIo.getExtension(source)) && !standard.isCxx()) {

                options.remove(ClavaOptions.STANDARD);
            }
        }

    }

    /**
     * Collects all source folders, taking into account given sources to compile, and include folders in flags.
     * 
     * @param sources
     * @param parserOptions
     * @return
     */
    private List<File> getInputSourceFolders(List<File> sources, List<String> parserOptions) {

        Set<File> sourceFolders = new HashSet<>();

        // Add folders of sources
        for (File source : sources) {
            if (source.isDirectory()) {
                sourceFolders.add(SpecsIo.getCanonicalFile(source));
                continue;
            }

            if (source.isFile()) {
                sourceFolders.add(SpecsIo.getCanonicalFile(source.getParentFile()));
                continue;
            }

            SpecsLogs.msgWarn("Could not process source '" + source + "'");
        }

        // Add folders of includes
        for (String parserOption : parserOptions) {
            // Remove possible quotes
            if (parserOption.startsWith("\"")) {
                parserOption = parserOption.substring(1);
            }

            if (parserOption.endsWith("\"")) {
                parserOption = parserOption.substring(0, parserOption.length() - 1);
            }

            if (parserOption.startsWith("-I")) {
                sourceFolders
                        .add(SpecsIo.getCanonicalFile(SpecsIo.existingFolder(parserOption.substring("-I".length()))));
                continue;
            }
        }

        // Reorder source folders, shortest to longest
        List<File> orderedSources = new ArrayList<>(sourceFolders);
        Collections.sort(orderedSources);

        return orderedSources;
    }

}
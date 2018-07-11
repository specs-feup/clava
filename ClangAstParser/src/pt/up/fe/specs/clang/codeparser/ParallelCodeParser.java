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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clang.ClangAstParser;
import pt.up.fe.specs.clang.clavaparser.ClavaParser;
import pt.up.fe.specs.clang.codeparser.clangparser.AstDumpParser;
import pt.up.fe.specs.clang.codeparser.clangparser.ClangParser;
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
 * Calls the dumper once per file.
 * <p>
 * This allows parallelization of the parsing (e.g., one process per compilation file) and compilation of header files
 * and mixed compilation (e.g., C + OpenCL).
 * 
 * @author JoaoBispo
 *
 */
public class ParallelCodeParser extends ACodeParser {

    @Override
    public App parse(List<File> sources, List<String> compilerOptions) {
        ClangParser clangParser = new AstDumpParser(get(SHOW_CLANG_DUMP), get(USE_CUSTOM_RESOURCES));

        List<File> allSourceFolders = getInputSourceFolders(sources, compilerOptions);
        Map<String, File> allSources = SpecsIo.getFileMap(allSourceFolders, SourceType.getPermittedExtensions());
        // System.out.println("ALL SOURCE FOLDERS:" + allSourceFolders);
        // System.out.println("ALL SOURCES:" + allSources);

        // Map<String, TranslationUnit> tunits = new HashMap<>();
        List<TranslationUnit> tUnits = new ArrayList<>();
        // Parse files, individually
        // int id = 1;
        for (Entry<String, File> sourceFile : allSources.entrySet()) {
            DataStore options = ClavaOptions.toDataStore(compilerOptions);

            // Adapt compiler options according to the file
            adaptOptions(options, new File(sourceFile.getKey()));

            TranslationUnit tunit = clangParser.parse(new File(sourceFile.getKey()), options);
            tUnits.add(tunit);

            if (get(SHOW_CLANG_DUMP)) {
                SpecsLogs.msgInfo("Clang Dump:\n" + SpecsIo.read(new File(ClangAstParser.getClangDumpFilename())));
            }

            // if (showClangAst) {
            if (get(SHOW_CLANG_AST)) {
                // TODO: Flag in dumper that dumps Clang AST to file
                SpecsLogs.msgInfo("Clang AST not supported for ParallelCodeParser");
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

        if (get(SHOW_CLAVA_AST)) {
            SpecsLogs.msgInfo("CLAVA AST:\n" + app.toTree());
        }

        // if (showCode) {
        if (get(SHOW_CODE)) {
            SpecsLogs.msgInfo("Code:\n" + app.getCode());
        }

        SpecsLogs.msgInfo("--- AST parsing report ---");
        // checkUndefinedNodes(app);

        return app;

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

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
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clang.codeparser.clangparser.AstDumpParser;
import pt.up.fe.specs.clang.codeparser.clangparser.ClangParser;
import pt.up.fe.specs.clang.streamparserv2.ClangStreamParser;
import pt.up.fe.specs.clang.textparser.TextParser;
import pt.up.fe.specs.clang.transforms.TreeTransformer;
import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.ast.LegacyToDataStore;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.clava.utils.SourceType;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.SpecsSystem;

/**
 * Calls the dumper once per file.
 * <p>
 * This allows parallelization of the parsing (e.g., one process per compilation file) and compilation of header files
 * and mixed compilation (e.g., C + OpenCL).
 * 
 * @author JoaoBispo
 *
 */
// public class ParallelCodeParser extends ACodeParser<ParallelCodeParser> {
public class ParallelCodeParser extends CodeParser {

    /// DATAKEY BEGIN
    public static final DataKey<Boolean> PARALLEL_PARSING = KeyFactory.bool("parallelParsing").setDefault(() -> true);
    /// DATAKEY END

    @Override
    public App parse(List<File> sources, List<String> compilerOptions) {

        List<File> allSourceFolders = getInputSourceFolders(sources, compilerOptions);

        Map<String, File> allSources = SpecsIo.getFileMap(allSourceFolders, SourceType.getPermittedExtensions());

        System.out.println(
                "All Sources:" + allSources.keySet().stream().map(Object::toString).collect(Collectors.joining(", ")));

        ConcurrentLinkedQueue<String> clangDump = new ConcurrentLinkedQueue<>();
        // ConcurrentLinkedQueue<File> workingFolders = new ConcurrentLinkedQueue<>();

        DataStore options = ClavaOptions.toDataStore(compilerOptions);

        // Add context to config
        ClavaContext context = new ClavaContext();
        options.add(ClavaNode.CONTEXT, context);

        Standard standard = getStandard(sources, options);
        // config.getTry(ClavaOptions.STANDARD).ifPresent(standard -> arguments.add(standard.getFlag()));

        long tic = System.nanoTime();

        List<TranslationUnit> tUnits = SpecsCollections.getStream(allSources.keySet(), get(PARALLEL_PARSING))
                .map(sourceFile -> parseSource(new File(sourceFile), standard, options, clangDump))
                .collect(Collectors.toList());

        // // Sort translation units
        // Collections.sort(tUnits, (tunit1, tunit2) -> tunit1.getFile().compareTo(tunit2.getFile()));

        // System.out.println(
        // "TUNITS:" + tUnits.stream().map(tunit -> tunit.getFile().toString()).collect(Collectors.joining(", ")));

        if (get(SHOW_EXEC_INFO)) {
            ClavaLog.metrics(SpecsStrings.takeTime("Code to TUs", tic));
            ClavaLog.metrics("Current memory used (Java):" + SpecsStrings.parseSize(SpecsSystem.getUsedMemory(true)));
        }

        if (get(SHOW_CLANG_DUMP)) {
            SpecsLogs.msgInfo(clangDump.stream().collect(Collectors.joining("\n")));
        }

        // if (showClangAst) {
        if (get(SHOW_CLANG_AST)) {
            SpecsLogs.msgInfo("Clang AST not supported for ParallelCodeParser");
        }

        // if (get(CLEAN)) {
        // workingFolders.stream().forEach(SpecsIo::deleteFolder);
        // }

        // System.out.println("ALL SOURCE FOLDERS:" + allSourceFolders);
        // System.out.println("ALL SOURCES:" + allSources);

        // Map<String, TranslationUnit> tunits = new HashMap<>();
        // List<TranslationUnit> tUnits = new ArrayList<>();
        // Parse files, individually
        // int id = 1;

        // for (Entry<String, File> sourceFile : allSources.entrySet()) {
        // DataStore options = ClavaOptions.toDataStore(compilerOptions);
        //
        // // Adapt compiler options according to the file
        // adaptOptions(options, new File(sourceFile.getKey()));
        //
        // ClangParser clangParser = new AstDumpParser(get(SHOW_CLANG_DUMP), get(USE_CUSTOM_RESOURCES));
        //
        // TranslationUnit tunit = clangParser.parse(new File(sourceFile.getKey()), options);
        // tUnits.add(tunit);
        //
        // if (get(SHOW_CLANG_DUMP)) {
        // SpecsLogs.msgInfo("Clang Dump:\n" + SpecsIo.read(new File(ClangAstParser.getClangDumpFilename())));
        // }
        //
        // // if (showClangAst) {
        // if (get(SHOW_CLANG_AST)) {
        // // TODO: Flag in dumper that dumps Clang AST to file
        // SpecsLogs.msgInfo("Clang AST not supported for ParallelCodeParser");
        // }
        //
        // }

        tic = System.nanoTime();

        App app = LegacyToDataStore.getFactory().app(tUnits);

        app.setSourcesFromStrings(allSources);
        app.addConfig(ClavaOptions.toDataStore(compilerOptions));

        // Add text elements (comments, pragmas) to the tree
        new TextParser(app.getContext()).addElements(app);

        // Applies several passes to make the tree resemble more the original code, e.g., remove implicit nodes from
        // original clang tree
        // new TreeTransformer(ClavaParser.getPostParsingRules()).transform(app);
        new TreeTransformer(ClangStreamParser.getPostParsingRules()).transform(app);

        if (get(SHOW_EXEC_INFO)) {
            ClavaLog.metrics(SpecsStrings.takeTime("TUs to Clava AST", tic));
            String usedSize = SpecsStrings.parseSize(SpecsSystem.getUsedMemory(true));
            ClavaLog.metrics("Current memory used (Java):" + usedSize);
        }

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
    //
    // private <T> Stream<T> getSourceFileStream(Collection<T> sourceFiles) {
    // if (get(PARALLEL_PARSING)) {
    // return sourceFiles.parallelStream();
    // }
    //
    // return sourceFiles.stream();
    // }

    private Standard getStandard(List<File> sources, DataStore options) {
        // If standard has been defined, return it
        if (options.hasValue(ClavaOptions.STANDARD)) {
            return options.get(ClavaOptions.STANDARD);
        }

        // Try to infer the standard form the sources extension
        Set<Standard> possibleStandards = new HashSet<>();
        boolean isCl = false;

        for (File source : sources) {
            String extension = SpecsIo.getExtension(source);

            if (extension.equals("cl")) {
                isCl = true;
                continue;
            }

            Standard.fromExtension(extension).ifPresent(std -> possibleStandards.add(std));
        }

        if (possibleStandards.isEmpty()) {
            if (isCl) {
                return Standard.C99;
            }

            // Use C99 as standard, possible only .h files
            return Standard.C99;
            // throw new RuntimeException(
            // "Could not determing a default standard from this list of source files: " + sources);
        }

        if (possibleStandards.size() == 1) {
            return possibleStandards.stream().findFirst().get();
        }

        throw new RuntimeException("Found more than one possible standard (" + possibleStandards
                + ") from this list of source files: " + sources);

        // config.getTry(ClavaOptions.STANDARD).ifPresent(standard -> arguments.add(standard.getFlag()));
        // config.getTry(ClavaOptions.STANDARD).ifPresent(standard -> arguments.add(standard.getFlag()));

        // TODO Auto-generated method stub
        // return null;
    }

    private TranslationUnit parseSource(File sourceFile, Standard standard, DataStore options,
            ConcurrentLinkedQueue<String> clangDump) {
        // ConcurrentLinkedQueue<String> clangDump, ConcurrentLinkedQueue<File> workingFolders) {

        // Adapt compiler options according to the file
        adaptOptions(options, sourceFile);

        // Disable streaming of console output if parsing is to be done in parallel
        boolean streamConsoleOutput = !get(PARALLEL_PARSING);
        ClangParser clangParser = new AstDumpParser(get(SHOW_CLANG_DUMP), get(USE_CUSTOM_RESOURCES),
                streamConsoleOutput);

        TranslationUnit tunit = clangParser.parse(sourceFile, standard, options);

        if (get(SHOW_CLANG_DUMP)) {
            // SpecsLogs.msgInfo("Clang Dump:\n" + SpecsIo.read(new File(ClangAstParser.getClangDumpFilename())));
            // SpecsLogs.msgInfo(clangParser.getClangDump());
            clangDump.add(clangParser.getClangDump());
        }

        if (get(CLEAN)) {
            // if (clangParser.getLastWorkingFolder() == null) {
            // workingFolders.add(clangParser.getLastWorkingFolder());
            // }
            if (clangParser.getLastWorkingFolder() == null) {
                SpecsLogs.msgInfo("No working folder found for source file '" + sourceFile + "'");
            } else {
                SpecsIo.deleteFolder(clangParser.getLastWorkingFolder());
            }

        }

        return tunit;
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

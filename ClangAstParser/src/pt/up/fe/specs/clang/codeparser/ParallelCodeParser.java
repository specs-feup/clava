/**
 * Copyright 2018 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clang.codeparser;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;
import pt.up.fe.specs.clang.ClangAstKeys;
import pt.up.fe.specs.clang.ClangFiles;
import pt.up.fe.specs.clang.ClangResources;
import pt.up.fe.specs.clang.dumper.ClangAstData;
import pt.up.fe.specs.clang.dumper.ClangAstDumper;
import pt.up.fe.specs.clang.dumper.ClangAstParser;
import pt.up.fe.specs.clang.transforms.TreeTransformer;
import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.clava.parsing.snippet.TextParser;
import pt.up.fe.specs.clava.utils.SourceType;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.SpecsSystem;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Calls the dumper once per file.
 * <p>
 * This allows parallelization of the parsing (e.g., one process per compilation file) and compilation of header files
 * and mixed compilation (e.g., C + OpenCL).
 *
 * @author JoaoBispo
 */
public class ParallelCodeParser extends CodeParser {

    /// DATAKEY BEGIN

    public static final DataKey<Boolean> PARALLEL_PARSING = KeyFactory.bool("parallelParsing")
            .setDefault(() -> true)
            .setLabel("Parallel parsing of source files");

    public static final DataKey<Integer> PARSING_NUM_THREADS = KeyFactory.integer("parsingNumThreads", 0)
            .setLabel("Number of threads to use for parallel parsing");

    public static final DataKey<Integer> SYSTEM_INCLUDES_THRESHOLD = KeyFactory.integer("systemIncludesThreshold", 1)
            .setLabel("System Includes parsing threshold (1+ - one or more levels of system includes, 0 all levels)");

    public static final DataKey<Boolean> CONTINUE_ON_PARSING_ERRORS = KeyFactory.bool("continueOnParsingErrors")
            .setLabel("Ignores parsing errors in C/C++ source code");

    public static final DataKey<Boolean> SYNTAX_ONLY = KeyFactory.bool("syntaxOnly")
            .setLabel("Runs the compiler/dumper pipeline only to validate syntax, without decoding the AST");

    /// DATAKEY END

    @Override
    public App parse(List<File> inputSources, List<String> compilerOptions, ClavaContext context) {

        // All files, header and implementation
        Map<String, File> allUserSources = SpecsIo.getFileMap(inputSources, SourceType.getPermittedExtensions());

        List<File> allSourceFolders = getInputSourceFolders(inputSources, compilerOptions);

        Map<String, File> allSources = SpecsIo.getFileMap(allSourceFolders, SourceType.getPermittedExtensions());

        ConcurrentLinkedQueue<String> clangDump = new ConcurrentLinkedQueue<>();
        ConcurrentLinkedQueue<String> syntaxErrors = new ConcurrentLinkedQueue<>();

        boolean syntaxOnly = get(SYNTAX_ONLY);

        DataStore options = ClangAstKeys.toDataStore(compilerOptions);

        // Add context to config
        // ClavaContext context = new ClavaContext();
        options.add(ClavaNode.CONTEXT, context);

        List<File> sources = allUserSources.keySet().stream()
                .map(File::new)
                // Sort files in order to maintain a consistent execution order
                .sorted()
                .collect(Collectors.toList());

        Standard standard = getStandard(sources, options);

        ClangResources clangResources = new ClangResources(this);

        var clangFiles = clangResources.getClangFiles(get(ClangAstKeys.LIBC_CXX_MODE));
        options.set(ClangAstKeys.LIBC_CXX_MODE, clangFiles.libcMode());

        ClavaLog.info("Found " + sources.size() + " source files");

        File parsingFolder = SpecsIo.getTempFolder("clava_parsing_" + UUID.randomUUID().toString());
        ClavaLog.debug(() -> "Parsing using folder '" + parsingFolder + "'");

        ParallelProgressCounter counter = new ParallelProgressCounter(sources.size());

        long tic = System.nanoTime();

        int numThreads = get(PARALLEL_PARSING) ? get(PARSING_NUM_THREADS) : 1;
        if (numThreads <= 0) {
            numThreads = Runtime.getRuntime().availableProcessors();
        }

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        List<Future<ClangAstData>> futureTUnits = new ArrayList<>();
        for (int i = 0; i < sources.size(); i++) {
            String id = Integer.toString(i + 1);
            File source = sources.get(i);

            Future<ClangAstData> tUnit = executor
                    .submit(() -> parseSource(source, id, standard, options, clangDump,
                            counter, parsingFolder, clangFiles, syntaxErrors));

            futureTUnits.add(tUnit);

        }

        // No more taks to submit
        executor.shutdown();

        // Collect parsing results
        List<ClangAstData> clangParserResults = new ArrayList<>();
        List<File> ignoredFiles = new ArrayList<>();
        for (int i = 0; i < sources.size(); i++) {
            var future = futureTUnits.get(i);
            try {
                var parserData = SpecsSystem.get(future);
                clangParserResults.add(parserData);
            } catch (Exception e) {
                if (syntaxOnly) {
                    throw new RuntimeException("Error while validating syntax of file '" + sources.get(i) + "'", e);
                }

                SpecsLogs.warn("Could not parse file '" + sources.get(i) + "', will be ignored", e);
                ignoredFiles.add(sources.get(i));
                continue;
            }

        }

        // Delete temporary folder
        SpecsIo.deleteFolder(parsingFolder);

        // No AST was decoded, just report syntax validation errors
        if (syntaxOnly) {
            List<String> validationErrors = new ArrayList<>(syntaxErrors);
            if (!validationErrors.isEmpty() && !get(CONTINUE_ON_PARSING_ERRORS)) {
                throw new ClavaParserException(validationErrors, clangFiles);
            }

            return null;
        }

        // // Sort translation units

        // "TUNITS:" + tUnits.stream().map(tunit -> tunit.getFile().toString()).collect(Collectors.joining(", ")));

        if (get(SHOW_EXEC_INFO)) {
            ClavaLog.metrics(SpecsStrings.takeTime("Code to AST", tic));
        }

        if (get(SHOW_CLANG_DUMP)) {
            SpecsLogs.msgInfo(clangDump.stream().collect(Collectors.joining("\n")));
        }

        boolean hasParsingErrors = clangParserResults.stream()
                .filter(data -> data.get(ClangAstData.HAS_ERRORS))
                .findAny()
                .isPresent();

        if (hasParsingErrors && !get(CONTINUE_ON_PARSING_ERRORS)) {
            List<String> errors = clangParserResults.stream()
                    .filter(data -> data.get(ClangAstData.HAS_ERRORS))
                    .map(data -> data.get(ClangAstData.LINES_NOT_PARSED))
                    .collect(Collectors.toList());

            throw new ClavaParserException(errors, clangFiles);
        }

        tic = System.nanoTime();
        boolean normalizeNodes = true;
        List<TranslationUnit> tUnits = new TUnitProcessor(clangParserResults, normalizeNodes).getTranslationUnits();

        // If errors, set error messages in corresponding TUnit
        for (var parserData : clangParserResults) {
            if (!parserData.get(ClangAstData.HAS_ERRORS)) {
                continue;
            }

            var errorOutput = parserData.get(ClangAstData.LINES_NOT_PARSED);
            parserData.get(ClangAstData.TRANSLATION_UNIT).set(TranslationUnit.HAS_PARSING_ERRORS);
            parserData.get(ClangAstData.TRANSLATION_UNIT).set(TranslationUnit.ERROR_OUTPUT, errorOutput);
        }

        App app = context.get(ClavaContext.FACTORY).app(tUnits);
        app.set(App.IGNORED_FILES, ignoredFiles);

        // Add App to context
        app.getContext().pushApp(app);

        app.setSourcesFromStrings(allSources);
        DataStore appConfig = ClangAstKeys.toDataStore(compilerOptions);
        appConfig.set(ClangAstKeys.LIBC_CXX_MODE, clangFiles.libcMode());
        app.addConfig(appConfig);

        // Applies several passes to make the tree resemble more the original code, e.g., remove implicit nodes from
        // original clang tree
        new TreeTransformer(ClangAstParser.getPostParsingRules()).transform(app);

        // Add text elements (comments, pragmas) to the tree
        new TextParser(app.getContext()).addElements(app);

        // Applies passes related with text elements
        new TreeTransformer(ClangAstParser.getTextParsingRules()).transform(app);

        if (get(SHOW_EXEC_INFO)) {
            ClavaLog.metrics(SpecsStrings.takeTime("AST Processing", tic));
            String usedSize = SpecsStrings.parseSize(SpecsSystem.getUsedMemory(true));
            ClavaLog.metrics("Current memory used (Java):" + usedSize);
        }

        // Perform second pass over types
        // processTypesSecondPass();

        if (get(SHOW_CLAVA_AST)) {
            SpecsLogs.msgInfo("CLAVA AST:\n" + app.toTree());
        }

        if (get(SHOW_CODE)) {
            SpecsLogs.msgInfo("Code:\n" + app.getCode());
        }

        SpecsLogs.msgInfo("--- AST parsing report ---");

        return app;

    }

    //

    private Standard getStandard(Collection<File> sources, DataStore options) {
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
            // "Could not determine a default standard from this list of source files: " + sources);
        }

        if (possibleStandards.size() == 1) {
            return possibleStandards.stream().findFirst().get();
        }

        throw new RuntimeException("Found more than one possible standard (" + possibleStandards
                + ") from this list of source files: " + sources);

        // TODO Auto-generated method stub
    }

    private ClangAstData parseSource(File sourceFile, String id, Standard standard, DataStore options,
                                     ConcurrentLinkedQueue<String> clangDump, ParallelProgressCounter counter, File parsingFolder,
                                     ClangFiles clangFiles, ConcurrentLinkedQueue<String> syntaxErrors) {

        // ConcurrentLinkedQueue<String> clangDump, ConcurrentLinkedQueue<File> workingFolders) {

        // Adapt compiler options according to the file

        // Disable streaming of console output if parsing is to be done in parallel
        // Only show output of console after parsing is done, when using parallel parsing
        boolean streamConsoleOutput = !get(PARALLEL_PARSING);

        ClangAstDumper clangParser = new ClangAstDumper(streamConsoleOutput, clangFiles.clangExecutable(),
                clangFiles.builtinIncludes(), clangFiles.systemResourceDir(), this)
                .setBaseFolder(parsingFolder)
                .setSystemIncludesThreshold(get(SYSTEM_INCLUDES_THRESHOLD));

        counter.print(sourceFile);

        // Run the same clang invocation, discard dumper output
        if (get(SYNTAX_ONLY)) {
            String error = clangParser.validateSyntax(sourceFile, id, standard, options);
            if (error != null) {
                syntaxErrors.add(error);
            }

            return null;
        }

        ClangAstData clangParserData = clangParser.parse(sourceFile, id, standard, options);

        if (get(SHOW_CLANG_DUMP)) {
            clangDump.add(clangParser.getClangDump());
        }

        if (get(CLEAN)) {
            if (clangParser.getLastWorkingFolder() == null) {
                SpecsLogs.msgInfo("No working folder found for source file '" + sourceFile + "'");
            } else {
                SpecsIo.deleteFolder(clangParser.getLastWorkingFolder());
            }

        }

        return clangParserData;
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
                sourceFolders.add(SpecsIo.getParent(source));
                continue;
            }

            SpecsLogs.warn("Could not process source '" + source + "'");
        }

        // Add folders of includes
        for (String parserOption : parserOptions) {

            // TODO: Check if needs to check for quotes before checking for -I

            boolean isInclude = parserOption.startsWith("-I");
            parserOption = parserOption.substring("-I".length());

            // Remove possible quotes
            if (parserOption.startsWith("\"")) {
                parserOption = parserOption.substring(1);
            }

            if (parserOption.endsWith("\"")) {
                parserOption = parserOption.substring(0, parserOption.length() - 1);
            }

            if (isInclude) {
                var includePath = new File(parserOption);

                if (!includePath.exists()) {
                    SpecsLogs.info("Include path not found, ignoring: '" + parserOption + "'");
                } else {
                    sourceFolders.add(SpecsIo.getCanonicalFile(SpecsIo.existingFolder(parserOption)));
                }

                continue;
            }
        }

        // Reorder source folders, shortest to longest
        List<File> orderedSources = new ArrayList<>(sourceFolders);
        Collections.sort(orderedSources);

        return orderedSources;
    }

}

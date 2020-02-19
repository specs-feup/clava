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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clang.ClangAstKeys;
import pt.up.fe.specs.clang.ClangResources;
import pt.up.fe.specs.clang.codeparser.clangparser.AstDumpParser;
import pt.up.fe.specs.clang.codeparser.clangparser.ClangParser;
import pt.up.fe.specs.clang.parsers.ClangParserData;
import pt.up.fe.specs.clang.streamparserv2.ClangStreamParser;
import pt.up.fe.specs.clang.textparser.TextParser;
import pt.up.fe.specs.clang.transforms.TreeTransformer;
import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.clava.utils.SourceType;
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

    public static final DataKey<Boolean> PARALLEL_PARSING = KeyFactory.bool("parallelParsing")
            // .setDefault(() -> true)
            .setLabel("Parallel parsing of source files");

    public static final DataKey<Integer> PARSING_NUM_THREADS = KeyFactory.integer("parsingNumThreads", 1)
            .setLabel("Number of threads to use for parallel parsing");

    public static final DataKey<Integer> SYSTEM_INCLUDES_THRESHOLD = KeyFactory.integer("systemIncludesThreshold", 1)
            .setLabel("System Includes parsing threshold (0 parses all system include headers found)");

    public static final DataKey<Boolean> CONTINUE_ON_PARSING_ERRORS = KeyFactory.bool("continueOnParsingErrors")
            .setLabel("Ignores parsing errors in C/C++ source code");

    // public static final DataKey<Integer> SYSTEM_INCLUDES_THRESHOLD = KeyFactory.integer("systemIncludesThreshold", 1)
    // .setLabel("Number of threads to use for parallel parsing");

    /// DATAKEY END

    @Override
    public App parse(List<File> inputSources, List<String> compilerOptions, ClavaContext context) {
        // ClavaLog.debug(() -> "[ParallelCodeParser] Input sources: " + inputSources);

        // All files, header and implementation
        Map<String, File> allUserSources = SpecsIo.getFileMap(inputSources, SourceType.getPermittedExtensions());
        // ClavaLog.debug(() -> "[ParallelCodeParser] All user sources: " + allUserSources);

        // System.out.println("sources:" + sources);
        List<File> allSourceFolders = getInputSourceFolders(inputSources, compilerOptions);
        // ClavaLog.debug(() -> "[ParallelCodeParser] All source folders: " + allSourceFolders);

        Map<String, File> allSources = SpecsIo.getFileMap(allSourceFolders, SourceType.getPermittedExtensions());
        // ClavaLog.debug(() -> "[ParallelCodeParser] All sources: " + allSources.values());
        // System.out.println("ALL SOURCES:" + allSources);
        // System.out.println(
        // "All Sources:" + allSources.keySet().stream().map(Object::toString).collect(Collectors.joining(", ")));

        ConcurrentLinkedQueue<String> clangDump = new ConcurrentLinkedQueue<>();
        // ConcurrentLinkedQueue<File> workingFolders = new ConcurrentLinkedQueue<>();

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
        // Standard standard = getStandard(allUserSources.values(), options);
        // config.getTry(ClavaOptions.STANDARD).ifPresent(standard -> arguments.add(standard.getFlag()));

        // Get version for the executable
        String version = options.get(ClangAstKeys.CLANGAST_VERSION);

        // Prepare resources before execution
        ClangResources clangResources = new ClangResources(get(SHOW_CLANG_DUMP));
        File clangExecutable = clangResources.prepareResources(version);

        List<String> builtinIncludes = clangResources.prepareIncludes(clangExecutable,
                get(ClangAstKeys.USE_PLATFORM_INCLUDES));

        ClavaLog.info("Found " + sources.size() + " source files");
        // ClavaLog.debug(() -> "[ParallelCodeParser] Files to parse:" + sources);

        File parsingFolder = SpecsIo.getTempFolder("clava_parsing_" + UUID.randomUUID().toString());
        ClavaLog.debug(() -> "Parsing using folder '" + parsingFolder + "'");

        // AtomicInteger currentSourceFileIndex = new AtomicInteger(0);
        ParallelProgressCounter counter = new ParallelProgressCounter(sources.size());

        long tic = System.nanoTime();

        // TODO: Allow parameter to specify parallelism
        int numThreads = get(PARALLEL_PARSING) ? get(PARSING_NUM_THREADS) : 1;

        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        List<Future<ClangParserData>> futureTUnits = new ArrayList<>();
        for (int i = 0; i < sources.size(); i++) {
            String id = Integer.toString(i + 1);
            File source = sources.get(i);

            Future<ClangParserData> tUnit = executor
                    .submit(() -> parseSource(source, id, standard, options, clangDump,
                            counter, parsingFolder, clangExecutable, builtinIncludes));

            futureTUnits.add(tUnit);

        }

        // No more taks to submit
        executor.shutdown();

        // Collect parsing results
        List<ClangParserData> clangParserResults = futureTUnits.stream()
                .map(SpecsSystem::get)
                .collect(Collectors.toList());

        // Delete temporary folder
        SpecsIo.deleteFolder(parsingFolder);

        // List<TranslationUnit> tUnits = SpecsCollections.getStream(allSources.keySet(), get(PARALLEL_PARSING))
        // .map(sourceFile -> parseSource(new File(sourceFile), standard, options, clangDump,
        // counter, parsingFolder))
        // .collect(Collectors.toList());
        // List<TranslationUnit> tUnits = SpecsCollections.getStream(sources, get(PARALLEL_PARSING))
        // .map(sourceFile -> parseSource(sourceFile, standard, options, clangDump,
        // counter, parsingFolder, clangExecutable, builtinIncludes))
        // .collect(Collectors.toList());

        // // Sort translation units
        // Collections.sort(tUnits, (tunit1, tunit2) -> tunit1.getFile().compareTo(tunit2.getFile()));

        // System.out.println(
        // "TUNITS:" + tUnits.stream().map(tunit -> tunit.getFile().toString()).collect(Collectors.joining(", ")));

        if (get(SHOW_EXEC_INFO)) {
            ClavaLog.metrics(SpecsStrings.takeTime("Code to AST", tic));
            // ClavaLog.metrics("Current memory used (Java):" +
            // SpecsStrings.parseSize(SpecsSystem.getUsedMemory(true)));
        }

        if (get(SHOW_CLANG_DUMP)) {
            SpecsLogs.msgInfo(clangDump.stream().collect(Collectors.joining("\n")));
        }

        // if (showClangAst) {
        if (get(SHOW_CLANG_AST)) {
            SpecsLogs.msgInfo("Clang AST not supported for ParallelCodeParser");
        }

        boolean hasParsingErrors = clangParserResults.stream()
                .filter(data -> data.get(ClangParserData.HAS_ERRORS))
                .findAny()
                .isPresent();

        if (hasParsingErrors && !get(CONTINUE_ON_PARSING_ERRORS)) {
            List<String> errors = clangParserResults.stream()
                    .filter(data -> data.get(ClangParserData.HAS_ERRORS))
                    .map(data -> data.get(ClangParserData.LINES_NOT_PARSED))
                    .collect(Collectors.toList());

            throw new ClavaParserException(errors);
        }

        tic = System.nanoTime();
        boolean normalizeNodes = true;
        List<TranslationUnit> tUnits = new TUnitProcessor(clangParserResults, normalizeNodes).getTranslationUnits();

        // If errors, set error messages in corresponding TUnit
        for (var parserData : clangParserResults) {
            if (!parserData.get(ClangParserData.HAS_ERRORS)) {
                continue;
            }

            var errorOutput = parserData.get(ClangParserData.LINES_NOT_PARSED);
            parserData.get(ClangParserData.TRANSLATION_UNIT).set(TranslationUnit.HAS_PARSING_ERRORS);
            parserData.get(ClangParserData.TRANSLATION_UNIT).set(TranslationUnit.ERROR_OUTPUT, errorOutput);
        }

        App app = context.get(ClavaContext.FACTORY).app(tUnits);

        // Add App to context
        // app.getContext().set(ClavaContext.APP, app);
        app.getContext().pushApp(app);

        app.setSourcesFromStrings(allSources);
        app.addConfig(ClangAstKeys.toDataStore(compilerOptions));

        // Applies several passes to make the tree resemble more the original code, e.g., remove implicit nodes from
        // original clang tree
        // new TreeTransformer(ClavaParser.getPostParsingRules()).transform(app);
        new TreeTransformer(ClangStreamParser.getPostParsingRules()).transform(app);

        // Add text elements (comments, pragmas) to the tree
        new TextParser(app.getContext()).addElements(app);

        // Applies passes related with text elements
        new TreeTransformer(ClangStreamParser.getTextParsingRules()).transform(app);

        if (get(SHOW_EXEC_INFO)) {
            ClavaLog.metrics(SpecsStrings.takeTime("AST Processing", tic));
            String usedSize = SpecsStrings.parseSize(SpecsSystem.getUsedMemory(true));
            ClavaLog.metrics("Current memory used (Java):" + usedSize);
        }

        // Cached filepaths metrics
        // ClavaLog.metrics(
        // "Cached filepaths analytics:\n" + app.getContext().get(ClavaContext.CACHED_FILEPATHS).getAnalytics());

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
        //
        // if (true) {
        // throw new RuntimeException("STOP");
        // }

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
            // throw new RuntimeException(
            // "Could not determine a default standard from this list of source files: " + sources);
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

    private ClangParserData parseSource(File sourceFile, String id, Standard standard, DataStore options,
            ConcurrentLinkedQueue<String> clangDump, ParallelProgressCounter counter, File parsingFolder,
            File clangExecutable, List<String> builtinIncludes) {
        // ConcurrentLinkedQueue<String> clangDump, ConcurrentLinkedQueue<File> workingFolders) {

        // Adapt compiler options according to the file
        // adaptOptions(options, sourceFile);

        // Disable streaming of console output if parsing is to be done in parallel
        // Only show output of console after parsing is done, when using parallel parsing
        boolean streamConsoleOutput = !get(PARALLEL_PARSING);

        ClangParser clangParser = new AstDumpParser(get(SHOW_CLANG_DUMP), get(USE_CUSTOM_RESOURCES),
                streamConsoleOutput, clangExecutable, builtinIncludes)
                        .setBaseFolder(parsingFolder)
                        .setSystemIncludesThreshold(get(SYSTEM_INCLUDES_THRESHOLD));
        // .setUsePlatformLibc(get(ClangAstKeys.USE_PLATFORM_INCLUDES));

        counter.print(sourceFile);
        // ClavaLog.info("Parsing '" + sourceFile.getAbsolutePath() + "'");
        ClangParserData clangParserData = clangParser.parse(sourceFile, id, standard, options);

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

        return clangParserData;
    }

    /*
    private void adaptOptions(DataStore options, File source) {
        // If OpenCL file, remove standard flag and add '-x cl'
        if (SpecsIo.getExtension(source).toLowerCase().equals("cl")) {
            if (options.hasValue(ClavaOptions.STANDARD)) {
                options.remove(ClavaOptions.STANDARD);
            }
    
            String currentFlags = options.get(ClavaOptions.FLAGS);
            String adaptedFlags = currentFlags + " -x cl";
            options.set(ClavaOptions.FLAGS, adaptedFlags);
        }
    
        // Check if the standard is compatible with the file type
        // Standard standard = options.getTry(ClavaOptions.STANDARD).orElse(null);
        // System.out.println("OPTIONS BEFORE:" + options);
        // Remove standard if extensions do not match
        // if (standard != null) {
        // if ((SourceType.isCExtension(SpecsIo.getExtension(source)) && standard.isCxx())
        // || SourceType.isCxxExtension(SpecsIo.getExtension(source)) && !standard.isCxx()) {
        //
        // options.remove(ClavaOptions.STANDARD);
        // }
        // }
        // System.out.println("OPTIONS AFTER:" + options);
    
    }
    */

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

            SpecsLogs.msgWarn("Could not process source '" + source + "'");
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
                sourceFolders.add(SpecsIo.getCanonicalFile(SpecsIo.existingFolder(parserOption)));
                continue;
            }
            // if (parserOption.startsWith("-I")) {
            // sourceFolders
            // .add(SpecsIo.getCanonicalFile(SpecsIo.existingFolder(parserOption.substring("-I".length()))));
            // continue;
            // }
        }

        // Reorder source folders, shortest to longest
        List<File> orderedSources = new ArrayList<>(sourceFolders);
        Collections.sort(orderedSources);

        return orderedSources;
    }

}

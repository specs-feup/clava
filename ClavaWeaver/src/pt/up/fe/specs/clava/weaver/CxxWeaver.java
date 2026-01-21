package pt.up.fe.specs.clava.weaver;

import org.lara.interpreter.joptions.config.interpreter.LaraiKeys;
import org.lara.interpreter.weaver.ast.AstMethods;
import org.lara.interpreter.weaver.interf.AGear;
import org.lara.interpreter.weaver.interf.JoinPoint;
import org.lara.interpreter.weaver.interf.events.Stage;
import org.lara.interpreter.weaver.options.WeaverOption;
import org.lara.language.specification.dsl.LanguageSpecification;
import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.storedefinition.StoreDefinition;
import org.suikasoft.jOptions.storedefinition.StoreDefinitionBuilder;
import pt.up.fe.specs.clang.ClangAstKeys;
import pt.up.fe.specs.clang.SupportedPlatform;
import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.clang.dumper.ClangAstDumper;
import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.Include;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.pragma.ClavaData;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.context.ClavaFactory;
import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.clava.parsing.snippet.SnippetParser;
import pt.up.fe.specs.clava.utils.SourceType;
import pt.up.fe.specs.clava.weaver.abstracts.weaver.ACxxWeaver;
import pt.up.fe.specs.clava.weaver.gears.CacheHandlerGear;
import pt.up.fe.specs.clava.weaver.gears.ModifiedFilesGear;
import pt.up.fe.specs.clava.weaver.joinpoints.CxxProgram;
import pt.up.fe.specs.clava.weaver.options.CxxWeaverOption;
import pt.up.fe.specs.clava.weaver.options.CxxWeaverOptions;
import pt.up.fe.specs.clava.weaver.utils.ClavaAstMethods;
import pt.up.fe.specs.util.*;
import pt.up.fe.specs.util.collections.AccumulatorMap;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.utilities.Buffer;
import pt.up.fe.specs.util.utilities.LineStream;
import pt.up.fe.specs.util.utilities.StringLines;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Weaver Implementation for CxxWeaver<br>
 * Since the generated abstract classes are always overwritten, their
 * implementation should be done by extending those
 * abstract classes with user-defined classes.<br>
 * The abstract class
 * {@link pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint} can be used
 * to add user-defined
 * methods and fields which the user intends to add for all join points and are
 * not intended to be used in LARA aspects.
 *
 * @author Lara Weaver Generator
 */
public class CxxWeaver extends ACxxWeaver {

    public static LanguageSpecification buildLanguageSpecification() {
        return LanguageSpecification.newInstance(ClavaWeaverResource.JOINPOINTS, ClavaWeaverResource.ARTIFACTS,
                ClavaWeaverResource.ACTIONS);
    }

    private static final List<String> CLAVA_PREDEFINED_EXTERNAL_DEPS = Arrays.asList("LAT - Lara Autotuning Tool",
            "https://github.com/specs-feup/LAT-Lara-Autotuning-Tool.git",
            "Benchmark - CHStone (import lara.benchmark.CHStoneBenchmarkSet)",
            "https://github.com/specs-feup/clava-benchmarks.git?folder=CHStone",
            "Benchmark - HiFlipVX (import lara.benchmark.HiFlipVXBenchmarkSet)",
            "https://github.com/specs-feup/clava-benchmarks.git?folder=HiFlipVX",
            // TODO: Missing LsuBencharkSet
            // "Benchmark - LSU (import lara.benchmark.LsuBenchmarkSet)",
            // "https://github.com/specs-feup/clava-benchmarks.git?folder=LSU",
            "Benchmark - NAS (import lara.benchmark.NasBenchmarkSet)",
            "https://github.com/specs-feup/clava-benchmarks.git?folder=NAS",
            "Benchmark - Parboil (import lara.benchmark.ParboilBenchmarkSet)",
            "https://github.com/specs-feup/clava-benchmarks.git?folder=Parboil",
            "Benchmark - Polybench (import lara.benchmark.PolybenchBenchmarkSet)",
            "https://github.com/specs-feup/clava-benchmarks.git?folder=Polybench",
            "Benchmark - Rosetta (import lara.benchmark.RosettaBenchmarkSet)",
            "https://github.com/specs-feup/clava-benchmarks.git?folder=Rosetta");

    private static final String TEMP_WEAVING_FOLDER = "__clava_woven";
    private static final String TEMP_SRC_FOLDER = "__clava_src";
    private static final String WOVEN_CODE_FOLDERNAME = "woven_code";

    private static final ThreadLocal<Buffer<File>> REBUILD_WEAVING_FOLDERS = ThreadLocal
            .withInitial(() -> new Buffer<>(2, CxxWeaver::newTemporaryWeavingFolder));

    private static final Set<String> LANGUAGES = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList("c", "cxx", "opencl")));

    private static final String CMAKE_GENERATED_FILES_FILENAME = "clava_generated_files.txt";
    private static final String CMAKE_IMPLEMENTATION_FILES_FILENAME = "clava_implementation_files.txt";
    private static final String CMAKE_INCLUDE_DIRS_FILENAME = "clava_include_dirs.txt";

    public static String getWovenCodeFoldername() {
        return WOVEN_CODE_FOLDERNAME;
    }

    private static final List<String> DEFAULT_COMMON_DUMPER_FLAGS = Arrays.asList("-Wno-unknown-pragmas");

    private static final List<String> DEFAULT_DUMPER_FLAGS = buildDefaultDumperFlags();

    private static List<String> buildDefaultDumperFlags() {
        List<String> defaultFlags = new ArrayList<>();

        // Default flags that are always added
        defaultFlags.addAll(DEFAULT_COMMON_DUMPER_FLAGS);

        SupportedPlatform currentPlatform = SupportedPlatform.getCurrentPlatform();

        // If MacOS, always add -ffreestanding
        if (currentPlatform == SupportedPlatform.MAC_OS) {
            defaultFlags.add("-ffreestanding");
        }

        return defaultFlags;
    }

    public static List<String> getDefaultFlags() {
        return DEFAULT_DUMPER_FLAGS;
    }

    /**
     * All definitions, including default LaraI keys and Clava-specific keys.
     */
    private static final Lazy<StoreDefinition> WEAVER_DEFINITION = Lazy.newInstance(() -> {
        return new StoreDefinitionBuilder("Clava Weaver")
                .addDefinition(LaraiKeys.STORE_DEFINITION)
                .addDefinition(CxxWeaverOption.STORE_DEFINITION)
                .build();
    });

    /**
     * This is used by a LARA aspect.
     *
     * @return
     */
    public static StoreDefinition getWeaverDefinition() {
        return WEAVER_DEFINITION.get();
    }

    // Parsing Context
    private ClavaContext context = null;

    // Gears
    private ModifiedFilesGear modifiedFilesGear = null;
    private CacheHandlerGear cacheHandlerGear = null;

    // Parsed program state
    private List<File> currentSources = null;
    private Map<File, File> currentBases = null;
    private Map<File, String> sourceFoldernames = null;
    private List<File> currentSourceFolders = null;
    private List<String> originalSourceFolders = null;

    private List<String> userFlags = null;

    private Set<String> messagesToUser;

    private AccumulatorMap<String> accMap;

    private ClavaWeaverData weaverData;

    public CxxWeaver() {
        reset();
    }

    private void reset() {
        // Gears
        this.modifiedFilesGear = new ModifiedFilesGear();
        this.cacheHandlerGear = new CacheHandlerGear();

        // Weaver configuration
        context = new ClavaContext();
        currentSources = new ArrayList<>();
        currentBases = null;
        sourceFoldernames = null;
        currentSourceFolders = null;
        originalSourceFolders = null;
        userFlags = null;

        // Set, in order to filter repeated messages
        // Linked, to preserve order
        messagesToUser = null;

        accMap = null;

        weaverData = null;
    }

    public ClavaWeaverData getWeaverData() {
        return weaverData;
    }

    public App getApp() {
        return getAppTry()
                .orElseThrow(() -> new RuntimeException(
                        "No App available, check if the code has compilation errors"));
    }

    public Optional<App> getAppTry() {

        if (weaverData == null) {
            return Optional.empty();
        }

        return weaverData.getAst();
    }

    public CxxProgram getAppJp() {
        return CxxJoinpoints.programFactory(getApp());
    }

    private Map<ClavaNode, Map<String, Object>> getUserValues() {
        return weaverData.getUserValues();
    }

    public boolean addMessageToUser(String message) {
        return messagesToUser.add(message);
    }

    /**
     * Set a file/folder in the weaver if it is valid file/folder type for the
     * weaver.
     *
     * @param source    the file with the source code
     * @param outputDir output directory for the generated file(s)
     * @param args      arguments to start the weaver
     * @return true if the file type is valid
     */
    @Override
    protected boolean begin(List<File> sources, File outputDir, DataStore args) {
        setData(args);
        this.weaverData = new ClavaWeaverData();
        this.accMap = new AccumulatorMap<>();
        this.messagesToUser = new LinkedHashSet<>();

        if (this.dataStore.get(CxxWeaverOption.DISABLE_CLAVA_INFO)) {
            SpecsLogs.getSpecsLogger().setLevelAll(Level.WARNING);
            ClavaLog.getLogger().setLevelAll(Level.WARNING);
        }

        // Create map with all sources, mapped to the corresponding base folder
        Map<File, File> allSources = new HashMap<>();
        for (var source : sources) {
            if (!source.exists()) {
                ClavaLog.info("Source path '" + source + "' does not exist, ignoring");
            } else if (source.isFile()) {
                allSources.put(source, null);
            } else if (source.isDirectory()) {
                allSources.put(source,
                        this.dataStore.get(CxxWeaverOption.FLAT_OUTPUT_FOLDER) ? null : source);
            } else {
                throw new RuntimeException("Case not implemented: " + source);
            }
        }

        updateSources(allSources);

        // Store original source folders
        // This means the current bases + folders of the sources
        Set<String> originalSourceFoldersSet = new LinkedHashSet<>();

        this.currentBases.values().stream()
                .filter(Objects::nonNull)
                .map(CxxWeaver::parseIncludePath)
                .forEach(originalSourceFoldersSet::add);

        getSources().stream()
                .map(CxxWeaver::parseIncludePath)
                .forEach(originalSourceFoldersSet::add);

        this.originalSourceFolders = new ArrayList<>(originalSourceFoldersSet);

        // If args does not have a standard, add a standard one based on the input files
        if (!this.dataStore.hasValue(ClavaOptions.STANDARD)) {
            this.dataStore.add(ClavaOptions.STANDARD, getStandard());
        }

        var parserOptions = buildParserOptions(dataStore);

        App app = null;

        // If weaving disabled, create empty App
        if (this.dataStore.get(CxxWeaverOption.DISABLE_WEAVING)) {
            SpecsLogs.msgInfo("Initial parsing disabled, creating empty 'program'");

            app = context.get(ClavaContext.FACTORY).app(Collections.emptyList());
            // First app, add it to context
            this.context.pushApp(app);
        } else {
            app = createApp(getSources(), parserOptions);
        }

        this.weaverData.pushAst(app);

        if (SpecsSystem.isDebug()) {
            // TODO: Option to dump clang and clava
            SpecsIo.write(new File("clavaDump.txt"), getApp().toString());
        }

        return true;
    }

    private List<String> buildParserOptions(DataStore args) {
        List<String> parserOptions = new ArrayList<>();

        // Add all source folders as include folders
        // Set<String> sourceIncludeFolders = getIncludeFlags(sources);
        Set<String> sourceIncludeFolders = getIncludeFlags(getSources());
        parserOptions.addAll(sourceIncludeFolders);

        // Add normal include folders
        for (File includeFolder : args.get(CxxWeaverOption.HEADER_INCLUDES)) {
            parserOptions.add("-I" + parseIncludePath(includeFolder));
        }

        // Add system include folders
        for (File includeFolder : args.get(CxxWeaverOption.SYSTEM_INCLUDES)) {
            parserOptions.add("-isystem");
            parserOptions.add(parseIncludePath(includeFolder));
        }

        // Add standard
        parserOptions.add(getStdFlag());

        // Add if new parsing should be disabled
        // parserOptions.add(CxxWeaverOption.)

        // Add default flags
        parserOptions.addAll(DEFAULT_DUMPER_FLAGS);

        // Add user flags
        userFlags = extractUserFlags(args);

        parserOptions.addAll(userFlags);

        return parserOptions;
    }

    /**
     * Updates the information relative to the current sources.
     *
     * <p>
     * Creation of sources follow this rules:<br>
     * 1) Single files listed in 'sources' are considered to not have a base folder
     * (relative path is null) <br>
     * 2) Source folders listed in 'sources' will have the parent folder as its base
     * folder <br>
     * 3) Source files or folders listed in 'map' will have the base folder
     * indicated in the map
     */
    private void updateSources(Map<File, File> map) {
        // TODO: Convert all folders to files, folders become bases when in sources
        map = obtainFiles(map);

        /// Sources
        this.currentSources = new ArrayList<>();
        // currentSources.addAll(sources);
        currentSources.addAll(map.keySet());

        // String datastoreFolderpath = args.get(JOptionKeys.CURRENT_FOLDER_PATH);
        // File datastoreFolder = datastoreFolderpath == null ? null : new
        // File(datastoreFolderpath);

        /// Bases
        currentBases = new HashMap<>();

        for (var entry : map.entrySet()) {
            if (entry.getKey().isDirectory()) {
                continue;
            }

            // File base = entry.getValue().equals(datastoreFolder) ? null :
            // entry.getValue();
            // currentBases.put(entry.getKey(), base);
            currentBases.put(entry.getKey(), entry.getValue());
        }

        /// Source folders
        Set<File> sourceFoldersSet = new HashSet<>();
        // sources.stream().filter(File::isDirectory).forEach(sourceFoldersSet::add);
        // Base folders are considered the base source folders
        map.values().stream()
                .filter(file -> file != null)
                .filter(File::isDirectory)
                .forEach(sourceFoldersSet::add);

        this.currentSourceFolders = new ArrayList<>(sourceFoldersSet);

        /// Source foldernames
        sourceFoldernames = new HashMap<>();

        // If base folder defined, use foldername as source name
        for (var entry : map.entrySet()) {
            if (entry.getValue() != null) {
                sourceFoldernames.put(entry.getKey(), entry.getValue().getName());
            }
        }

    }

    public List<String> getUserFlags() {
        return userFlags;
    }

    private List<String> extractUserFlags(DataStore args) {
        String flagsString = args.get(ClavaOptions.FLAGS);

        // Add string argument flags
        List<String> flags = Arrays.stream(flagsString.split(" "))
                // Only consider non-empty strings
                .filter(string -> !string.isEmpty())
                .collect(Collectors.toList());

        // Add JSON argument flags
        flags.addAll(args.get(ClavaOptions.FLAGS_LIST));

        return flags;
    }

    public String getStdFlag() {
        Standard standard = this.dataStore.get(ClavaOptions.STANDARD);
        return "-std=" + standard.getString();
    }

    public Standard getStandard() {

        if (this.dataStore.hasValue(ClavaOptions.STANDARD)) {
            return this.dataStore.get(ClavaOptions.STANDARD);
        }

        // Determine standard based on implementation files
        boolean isCxx = false;
        boolean isC = false;

        for (String implFile : SpecsIo.getFileMap(getSources(), SourceType.IMPLEMENTATION.getExtensions())
                .keySet()) {

            if (SourceType.isCxxExtension(SpecsIo.getExtension(implFile))) {
                isCxx = true;
                continue;
            }

            if (SourceType.isCExtension(SpecsIo.getExtension(implFile))) {
                isC = true;
                continue;
            }
        }

        if (isCxx && isC) {
            throw new RuntimeException(
                    "Found both C and C++ implementation files, currently this is not supported");
        } else if (!isCxx && !isC) {
            // Default to C++
            return Standard.CXX11;
            // throw new RuntimeException(
            // "Could not find neither C or C++ implementation files");
        } else if (isCxx) {
            return Standard.CXX11;
            // return "c++11";
        } else {
            return Standard.C99;
            // return "c99";
        }

    }

    public Set<String> getIncludeFlags() {
        if (getSources() == null) {
            SpecsLogs.warn("Source folders are not set");
            return Collections.emptySet();
        }

        return getIncludeFlags(getSources());
    }

    private Set<String> getIncludeFlags(List<File> sources) {
        Set<String> sourceIncludeFolders = sources.stream()
                .map(source -> "-I" + parseIncludePath(source))
                // Collect to a set, to remove duplicates
                .collect(Collectors.toSet());
        return sourceIncludeFolders;
    }

    public Set<String> getIncludeFolders() {
        if (getSources() == null) {
            SpecsLogs.warn("Source folders are not set");
            return Collections.emptySet();
        }

        return getIncludeFolders(getSources());
    }

    private Set<String> getIncludeFolders(List<File> sources) {
        Set<String> sourceIncludeFolders = sources.stream()
                .map(source -> parseIncludePath(source))
                // Collect to a set, to remove duplicates
                .collect(Collectors.toSet());
        return sourceIncludeFolders;
    }

    private static File getFirstSourceFolder(List<File> sources) {

        if (sources.isEmpty()) {
            return SpecsIo.getWorkingDir();
        }

        File firstSource = sources.stream()
                .filter(source -> source.exists())
                .findFirst()
                .orElse(SpecsIo.getWorkingDir());

        if (firstSource.isDirectory()) {
            return firstSource;
        }

        if (firstSource.isFile()) {
            return firstSource.getAbsoluteFile().getParentFile();
        }

        throw new RuntimeException("Could not process source '" + firstSource + "'");
    }

    private static String parseIncludePath(File path) {
        // If file, get parent folder
        File folderPath = path.isFile() ? path.getAbsoluteFile().getParentFile() : path;

        String folderAbsPath = folderPath.getAbsolutePath();

        return folderAbsPath;
        /*
         * if (!folderAbsPath.contains(" ")) {
         * return folderAbsPath;
         * }
         *
         * // If on Windows, do not escape, or it will not work:
         * // https://coderanch.com/t/627514/java/ProcessBuilder-incorrectly-processes-
         * embedded-spaces
         * if (SpecsPlatforms.isWindows()) {
         * return folderAbsPath;
         * }
         *
         * return "\"" + folderAbsPath + "\"";
         */
    }

    /**
     * Builds the -I argument
     *
     * @param include
     * @return
     */
    private static String buildIncludeArg(String includePath) {

        return "-I" + includePath;
    }

    public App createApp(List<File> sources, List<String> parserOptions) {
        return createApp(sources, parserOptions, Collections.emptyList());
    }

    /**
     * @param sources
     * @param parserOptions
     * @param extraOptions  options that should not be processed (e.g., header files
     *                      found in folders specified by -I flags are
     *                      automatically added to the compilation, if we want to
     *                      add header folders whose header files should not
     *                      be parsed, they can be specified here)
     * @return
     */
    public App createApp(List<File> sources, List<String> parserOptions, List<String> extraOptions) {
        ClavaLog.debug(() -> "Creating App from the following sources: " + sources);
        ClavaLog.debug(() -> "Creating App using the following options: " + parserOptions);
        ClavaLog.debug(() -> "Creating App using the following extra options: " + extraOptions);

        // Collect additional include folders
        Set<String> sourceIncludeFolders = getSourceIncludes(sources);
        ClavaLog.debug(() -> "Source include folders: " + sourceIncludeFolders);

        // Add include folders to extra options
        List<String> adaptedExtraOptions = new ArrayList<>(sourceIncludeFolders.size() + extraOptions.size());
        adaptedExtraOptions.addAll(extraOptions);
        sourceIncludeFolders.stream().map(includeFolder -> "-I" + includeFolder).forEach(adaptedExtraOptions::add);

        List<String> allFiles = sources.stream().map(File::toString).collect(Collectors.toList());

        // Sort filenames so that select order of files is consistent between OSes
        Collections.sort(allFiles);

        boolean useCustomResources = this.dataStore.get(ClavaOptions.CUSTOM_RESOURCES);

        CodeParser codeParser = CodeParser.newInstance();

        // Setup code parser
        codeParser.set(CodeParser.USE_CUSTOM_RESOURCES, useCustomResources);
        codeParser.set(CodeParser.CUDA_GPU_ARCH, this.dataStore.get(CodeParser.CUDA_GPU_ARCH));
        codeParser.set(CodeParser.CUDA_PATH, this.dataStore.get(CodeParser.CUDA_PATH));
        codeParser.set(ParallelCodeParser.PARALLEL_PARSING, this.dataStore.get(ParallelCodeParser.PARALLEL_PARSING));
        codeParser.set(ParallelCodeParser.PARSING_NUM_THREADS, this.dataStore.get(ParallelCodeParser.PARSING_NUM_THREADS));
        codeParser.set(ParallelCodeParser.SYSTEM_INCLUDES_THRESHOLD,
                this.dataStore.get(ParallelCodeParser.SYSTEM_INCLUDES_THRESHOLD));
        codeParser.set(ParallelCodeParser.CONTINUE_ON_PARSING_ERRORS,
                this.dataStore.get(ParallelCodeParser.CONTINUE_ON_PARSING_ERRORS));
        codeParser.set(ClangAstKeys.LIBC_CXX_MODE, this.dataStore.get(ClangAstKeys.LIBC_CXX_MODE));
        codeParser.set(CodeParser.DUMPER_FOLDER, this.dataStore.get(CodeParser.DUMPER_FOLDER));

        List<String> allParserOptions = new ArrayList<>(parserOptions.size() + adaptedExtraOptions.size());
        allParserOptions.addAll(parserOptions);
        allParserOptions.addAll(adaptedExtraOptions);
        App app = codeParser.parse(SpecsCollections.map(allFiles, File::new), allParserOptions, context);

        // Set source paths of each TranslationUnit
        app.setSources(currentBases);
        app.setSourceFoldernames(sourceFoldernames);

        // Set external dependencies
        app.getExternalDependencies()
                .setDisableRemoteDependencies(this.dataStore.get(ClavaOptions.DISABLE_REMOTE_DEPENDENCIES));

        return app;
    }

    private Set<String> getSourceIncludes(List<File> sources) {
        Set<String> sourceIncludeFolders = new LinkedHashSet<>();

        // Add folders of source files as include folders
        sources.stream()
                .map(CxxWeaver::parseIncludePath)
                .forEach(sourceIncludeFolders::add);

        sourceIncludeFolders.addAll(originalSourceFolders);
        return sourceIncludeFolders;
    }

    private boolean isCutoffFolder(File path) {
        // Ignore files
        if (path.isFile()) {
            return false;
        }

        // Ignore CMake build folder
        // if (path.isDirectory() && new File(path, "CMakeCache.txt").isFile()) {
        if (path.isDirectory() && new File(path, "CMakeFiles").isDirectory()) {
            ClavaLog.debug(() -> "Ignoring source folder due to being a CMake build folder: " + path.getAbsolutePath());
            return true;
        }

        return false;
    }

    private List<String> processSources(Map<String, File> sourceFiles, List<String> parserOptions) {

        SpecsCheck.checkArgument(!sourceFiles.isEmpty(),
                () -> "No C/C++ files found in the given source folders:" + getSources());

        Set<String> adaptedSources = new HashSet<>();

        // boolean skipHeaderFiles =
        // args.get(CxxWeaverOption.SKIP_HEADER_INCLUDES_PARSING);
        boolean skipHeaderFiles = !this.dataStore.get(CxxWeaverOption.PARSE_INCLUDES);

        if (skipHeaderFiles) {
            // Add only implementation files if skipping header includes
            for (String sourceFile : sourceFiles.keySet()) {
                var currentSourceFiles = SpecsIo.getFilesRecursive(new File(sourceFile));
                currentSourceFiles.stream()
                        .filter(currentSource -> !SourceType.isHeader(currentSource))
                        .map(File::getAbsolutePath)
                        .forEach(adaptedSources::add);
            }
        } else {
            sourceFiles.keySet().stream().forEach(adaptedSources::add);
        }

        // Add header files in normal include folders to the tree
        if (!skipHeaderFiles) {
            // Use parser options instead of weaver options, it can be a rebuild with other
            // folders
            // TODO: Remove dependency to parserOptions, instead ask for list of includes?
            List<File> headerIncludes = parserOptions.stream()
                    .map(CxxWeaver::headerFlagToFile)
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    // .FILTER(OPTION -> OPTION.STARTSWITH("-I"))
                    // .MAP(OPTION -> NEW FILE(OPTION.SUBSTRING("-I".LENGTH())))
                    .collect(Collectors.toList());

            // Gather header files
            Set<String> headerExtensions = SourceType.HEADER.getExtensions();
            Map<String, File> headerFilesMap = SpecsIo.getFileMap(headerIncludes, true, headerExtensions,
                    this::isCutoffFolder);

            headerFilesMap.keySet().stream()
                    .forEach(adaptedSources::add);

            // adaptedSources.addAll(headerIncludes);

            ClavaLog.debug(() -> "Found the following normal header includes: " + headerIncludes);
            ClavaLog.debug(() -> "Adding the following header includes from the options: " + headerFilesMap.keySet());
            // ClavaLog.debug(() -> "Original header includes: " +
            // args.get(CxxWeaverOption.HEADER_INCLUDES));
            // for (File includeFolder : args.get(CxxWeaverOption.HEADER_INCLUDES)) {
            // adaptedSources.add(includeFolder);
            // // parserOptions.add("-I" + parseIncludePath(includeFolder));
            // }
        }

        return new ArrayList<>(adaptedSources);
    }

    private static Optional<File> headerFlagToFile(String headerFlag) {
        if (!headerFlag.startsWith("-I")) {
            return Optional.empty();
        }

        String filepath = headerFlag.substring("-I".length());

        if (filepath.startsWith("\"")) {
            SpecsCheck.checkArgument(filepath.endsWith("\""),
                    () -> "Expected header flag to end with '\"', if it starts with '\"'");
            filepath = filepath.substring(1, filepath.length() - 1);
        }

        return Optional.of(new File(filepath));

    }

    /**
     * Return a JoinPoint instance of the language root, i.e., an instance of
     * AProgram
     *
     * @return an instance of the join point root/program
     */
    @Override
    public JoinPoint getRootJp() {
        return CxxJoinpoints.create(getApp());
    }

    public String getProgramName() {
        return getFirstSourceFolder(getSources()).getName();
    }

    public File getWeavingFolder() {

        String outputFoldername = this.dataStore.get(CxxWeaverOption.WOVEN_CODE_FOLDERNAME);
        if (outputFoldername.isEmpty()) {
            ClavaLog.info("No name defined for the output folder, using default value 'output'");
            outputFoldername = "output";
        }

        File outputFolder = SpecsIo.mkdir(this.dataStore.get(LaraiKeys.OUTPUT_FOLDER), outputFoldername);
        return outputFolder;
    }

    /**
     * Closes the weaver to the specified output directory location, if the weaver
     * generates new file(s)
     *
     * @return if close was successful
     */
    @Override
    protected boolean close() {

        // if (!args.get(CxxWeaverOption.DISABLE_WEAVING)) {
        // Process App files
        getAppTry().ifPresent(app -> {
            if (!app.getTranslationUnits().isEmpty()) {
                if (this.dataStore.get(CxxWeaverOption.CHECK_SYNTAX)) {
                    SpecsLogs.msgInfo("Checking woven code syntax...");
                    rebuildAst(false);
                }

                // Terminate weaver execution with final steps required and writing output files

                // Write output files if code generation is not disabled
                if (!this.dataStore.get(CxxWeaverOption.DISABLE_CODE_GENERATION)) {
                    writeCode(getWeavingFolder());
                }

                // Write CMake helper files
                if (this.dataStore.get(CxxWeaverOption.GENERATE_CMAKE_HELPER_FILES)) {
                    generateCmakerHelperFiles();
                }

            }
        });

        /// Clean-up phase

        // Delete temporary weaving folder, if exists
        SpecsIo.deleteFolder(new File(TEMP_WEAVING_FOLDER));

        // Delete temporary source folder, if exists
        SpecsIo.deleteFolder(new File(TEMP_SRC_FOLDER));

        if (this.dataStore != null) {
            // Delete intermediary files
            if (this.dataStore.get(CxxWeaverOption.CLEAN_INTERMEDIATE_FILES)) {
                for (String tempFile : ClangAstDumper.getTempFiles()) {
                    new File(tempFile).delete();
                }
            }

            // Re-enable output
            if (this.dataStore.get(CxxWeaverOption.DISABLE_CLAVA_INFO)) {
                SpecsLogs.getSpecsLogger().setLevelAll(null);
                ClavaLog.getLogger().setLevelAll(null);
            }
        }

        if ((messagesToUser != null) && !messagesToUser.isEmpty()) {
            ClavaLog.info(" - Messages -");
            messagesToUser.forEach(ClavaLog::info);
        }

        // Clear weaver data
        reset();

        return true;
    }

    private void generateCmakerHelperFiles() {
        // Generated files
        Set<File> generatedFiles = new HashSet<>();

        // Add generated files based on input code
        generatedFiles.addAll(weaverData.getGeneratedFiles());
        // Add manually generated files
        generatedFiles.addAll(weaverData.getManuallyWrittenFiles());

        String generatedFilesContent = generatedFiles.stream()
                // Convert to absolute path
                // .map(file -> SpecsIo.getCanonicalFile(file).getAbsolutePath())
                .map(file -> SpecsIo.getCanonicalPath(file))
                .map(SpecsIo::normalizePath)
                // CMake-friendly list
                .collect(Collectors.joining(";"));

        File cmakeGeneratedFiles = new File(getWeavingFolder(), CMAKE_GENERATED_FILES_FILENAME);

        SpecsIo.write(cmakeGeneratedFiles, generatedFilesContent);

        // Get only implementation files
        String implementationFilesContent = generatedFiles.stream()
                .filter(file -> SourceType.getType(file.getName()) == SourceType.IMPLEMENTATION)
                // .map(file -> SpecsIo.getCanonicalFile(file).getAbsolutePath())
                .map(file -> SpecsIo.getCanonicalPath(file))
                .map(SpecsIo::normalizePath)
                // CMake-friendly list
                .collect(Collectors.joining(";"));

        File cmakeImplementationFiles = new File(getWeavingFolder(), CMAKE_IMPLEMENTATION_FILES_FILENAME);

        SpecsIo.write(cmakeImplementationFiles, implementationFilesContent);

        // Determine new include dirs
        List<File> newIncludeDirs = new ArrayList<>();
        // Add folders for generated files
        newIncludeDirs.addAll(getAllIncludeFolders(getWeavingFolder(), generatedFiles));

        // Add original includes at the end, in case there are "out-of-source" files
        // (e.g., .inc files) that are needed
        newIncludeDirs.addAll(this.dataStore.get(CxxWeaverOption.HEADER_INCLUDES).getFiles());

        // Add includes from original sources
        originalSourceFolders.stream().map(sourceFolder -> new File(sourceFolder))
                .forEach(newIncludeDirs::add);

        // If we are skipping the parsing of include folders, we should include the
        // original include folders as includes
        // if (args.get(CxxWeaverOption.SKIP_HEADER_INCLUDES_PARSING)) {
        // List<File> originalHeaderIncludes =
        // args.get(CxxWeaverOption.HEADER_INCLUDES).getFiles();
        // newIncludeDirs.addAll(originalHeaderIncludes);
        // ClavaLog.debug(() -> "Skip headers is enabled, adding original headers: " +
        // originalHeaderIncludes);
        // }

        // String includeFoldersContent =
        // getIncludePaths(getWeavingFolder()).stream().collect(Collectors.joining(";"));
        String includeFoldersContent = newIncludeDirs.stream()
                // .map(File::getAbsolutePath)
                .map(SpecsIo::getCanonicalPath)
                .map(SpecsIo::normalizePath)
                .collect(Collectors.joining(";"));

        File cmakeIncludes = new File(getWeavingFolder(), CMAKE_INCLUDE_DIRS_FILENAME);
        SpecsIo.write(cmakeIncludes, includeFoldersContent);

        String cmakeSubProjectsContent = getApp().getExternalDependencies().getProjects().stream()
                .map(SpecsIo::getCanonicalPath)
                .map(SpecsIo::normalizePath)
                .collect(Collectors.joining(";"));
        File cmakeSubProjects = new File(getWeavingFolder(), "clava_sub_projects.txt");
        SpecsIo.write(cmakeSubProjects, cmakeSubProjectsContent);

        String cmakeSubLibsContent = getApp().getExternalDependencies().getProjectsLibs().stream()
                .collect(Collectors.joining(";"));
        File cmakeSubLibs = new File(getWeavingFolder(), "clava_sub_libs.txt");
        SpecsIo.write(cmakeSubLibs, cmakeSubLibsContent);
    }

    @Override
    public void writeCode(File outputFolder) {
        // If copy files is enabled, first copy source files to output folder
        if (this.dataStore.get(CxxWeaverOption.COPY_FILES_IN_SOURCES)) {
            for (File source : currentSourceFolders) {
                // If file, just copy the file
                // if (source.isFile()) {
                // SpecsIo.copy(source, new File(outputFolder, source.getName()));
                // continue;
                // }

                if (source.isDirectory()) {
                    File destFolder = SpecsIo.mkdir(outputFolder, source.getName());
                    SpecsIo.copyFolderContents(source, destFolder);
                    continue;
                }

                SpecsLogs.warn("Case not defined for source '" + source + "', is neither a file or a folder");
            }
        }

        Set<String> modifiedFilenames = getModifiedFilenames();

        // Get files and contents to write
        Map<File, String> files = getApp().getCode(outputFolder, modifiedFilenames);

        // Write files that have changed
        for (Entry<File, String> entry : files.entrySet()) {
            File destinationFile = entry.getKey();
            // System.out.println("DESTINATION FILE:" + destinationFile);
            String code = entry.getValue();

            // If file already exists, and is the same as the file that we are about to
            // write, skip
            if (destinationFile.isFile() && areEqual(destinationFile, code)) {
                continue;
            }

            SpecsLogs.msgInfo("Changes in file '" + destinationFile + "'");
            SpecsIo.write(destinationFile, code);
        }

        // Store which files were generated
        weaverData.setGeneratedFiles(files.keySet());
    }

    private Set<String> getModifiedFilenames() {
        // If option is disabled, return null
        if (!this.dataStore.get(CxxWeaverOption.GENERATE_MODIFIED_CODE_ONLY)) {
            return null;
        }

        // Option is enabled, get list of filenames to generate from AST
        return modifiedFilesGear.getModifiedFilenames();
    }

    private static boolean areEqual(File expected, String actual) {

        // Case where the file is empty
        if (expected.length() == 0) {
            return actual.isEmpty();
        }

        try (LineStream original = LineStream.newInstance(expected)) {
            StringLines codeLines = StringLines.newInstance(actual);

            long counter = 0;
            while (original.hasNextLine()) {
                counter++;

                if (!codeLines.hasNextLine()) {
                    SpecsLogs
                            .msgLib(
                                    "Expected has line but actual has no more lines.\nExpected:" + original.nextLine());
                    return false;
                }

                String expectedLine = original.nextLine();
                String actualLine = codeLines.nextLine();

                if (!expectedLine.equals(actualLine)) {
                    SpecsLogs.msgLib(
                            "Difference on line " + counter + ".\nActual:" + actualLine + "\nExpected:" + expectedLine);
                    return false;
                }
            }

            // Check if there are still other lines
            if (codeLines.hasNextLine()) {
                return false;
            }

        }

        return true;
    }

    /**
     * Returns a list of Gears associated to this weaver engine
     *
     * @return a list of implementations of {@link AGear} or null if no gears are
     * available
     */
    @Override
    public List<AGear> getGears() {
        return Arrays.asList(modifiedFilesGear, cacheHandlerGear);
    }

    @Override
    public List<WeaverOption> getOptions() {
        return CxxWeaverOption.STORE_DEFINITION.getKeys().stream()
                .map(CxxWeaverOptions::getOption)
                .collect(Collectors.toList());

    }

    @Override
    public String getName() {
        // v1.2.2
        return "Clava";
    }

    public DataStore getConfig() {
        return this.dataStore;
    }

    public TranslationUnit rebuildFile(TranslationUnit tUnit) {

        // Clear data object for the ids of this file
        var nodes = tUnit.getDescendantsAndSelfStream().collect(Collectors.toList());
        ClavaData.clearAllCaches(nodes);

        // Write current tree to a temporary folder
        File tempFolder = REBUILD_WEAVING_FOLDERS.get().next();

        File destinationFile = tUnit.getDestinationFile(tempFolder);
        String code = tUnit.getCode();
        SpecsIo.write(destinationFile, code);

        // List<File> writtenFiles = getApp().write(tempFolder, flattenFolders);
        ClavaLog.debug(() -> "Rebuilding file '" + destinationFile + "'");

        Set<File> includeFolders = getSourceIncludeFoldersFromTempFolder(tempFolder);

        List<String> rebuildOptions = new ArrayList<>();

        // Copy current options, removing previous normal includes
        var parserOptions = buildParserOptions(this.dataStore);
        parserOptions.stream()
                .filter(option -> !option.startsWith("-I"))
                .forEach(rebuildOptions::add);

        // Add include folders
        for (File includeFolder : includeFolders) {
            // rebuildOptions.add(0, "\"-I" + includeFolder.getAbsolutePath() + "\"");
            rebuildOptions.add(0, CxxWeaver.buildIncludeArg(includeFolder.getAbsolutePath()));
        }

        // Add extra includes
        for (File extraInclude : getExternalIncludeFolders()) {
            // rebuildOptions.add(0, "\"-I" + extraInclude.getAbsolutePath() + "\"");
            rebuildOptions.add(0, CxxWeaver.buildIncludeArg(extraInclude.getAbsolutePath()));
        }

        // Write the other translation units and add folder as includes, in case they
        // are needed
        String currentCodeFoldername = TEMP_WEAVING_FOLDER + "_for_file_rebuild";
        File currentCodeFolder = SpecsIo.mkdir(currentCodeFoldername).getAbsoluteFile();
        SpecsIo.deleteFolderContents(currentCodeFolder);

        // Add include
        // rebuildOptions.add(0, "\"-I" + currentCodeFolder.getAbsolutePath() + "\"");
        rebuildOptions.add(0, CxxWeaver.buildIncludeArg(currentCodeFolder.getAbsolutePath()));

        for (TranslationUnit otherTUnit : tUnit.getApp().getTranslationUnits()) {

            // Skip self
            if (otherTUnit == tUnit) {
                continue;
            }

            otherTUnit.write(currentCodeFolder);
        }

        // App rebuiltApp = createApp(srcFolders, rebuildOptions);

        App rebuiltApp = createApp(Arrays.asList(destinationFile), rebuildOptions);

        // Remove app from context stack
        context.popApp();

        // Delete current code folder
        SpecsIo.deleteFolder(currentCodeFolder);

        // After rebuilding, clear current app cache
        getApp().clearCache();
        getEventTrigger().triggerAction(Stage.DURING,
                "CxxWeaver.rebuildFile",
                CxxJoinpoints.create(tUnit), Collections.emptyList(), Optional.empty());

        // Return correct TranslationUnit
        for (TranslationUnit tu : rebuiltApp.getTranslationUnits()) {
            if (SpecsIo.getCanonicalFile(destinationFile).equals(SpecsIo.getCanonicalFile(tu.getFile()))) {
                return tu;
            }
            // System.out.println("TU: " + tu.getFile());
            // System.out.println("IS SAME: " + destinationFile.equals(tu.getFile()));
        }

        throw new RuntimeException("Could not find TranslationUnit that corresponds to the rebuilt file '"
                + destinationFile + "':\n" + rebuiltApp.getTranslationUnits());
        // return rebuiltApp.getTranslationUnits().get(0);
    }

    public void rebuildAstFuzzy() {
        int maxIterations = 1;

        ClavaLog.debug("Fuzzy parsing started");

        int currentIteration = 0;
        boolean hasParsingErrors = true;
        while (hasParsingErrors && currentIteration < maxIterations) {
            currentIteration++;

            ClavaLog.debug("Fuzzy parsing iteration " + currentIteration);

            // Save AST
            pushAst();

            // Rebuild
            rebuildAst(true);

            var fuzzyApp = getApp();
            hasParsingErrors = fuzzyApp.hasParsingErrors();

            // No parsing errors, job done
            if (!hasParsingErrors) {
                break;
            }

            // Collect error for each translation unit
            Map<String, String> tunitsErrors = new HashMap<>();
            for (var tunit : fuzzyApp.getTranslationUnits()) {
                System.out.println("CHECKING " + tunit.getRelativeFilepath());
                if (!tunit.get(TranslationUnit.HAS_PARSING_ERRORS)) {
                    continue;
                }
                System.out.println("ADDED");
                var errorOutput = tunit.get(TranslationUnit.ERROR_OUTPUT);

                String tunitId = tunit.getRelativeFilepath();
                tunitsErrors.put(tunitId, errorOutput);
            }

            // Restore app
            popAst();

            var originalApp = getApp();

            for (var originalTunit : originalApp.getTranslationUnits()) {
                var errorOutput = tunitsErrors.get(originalTunit.getRelativeFilepath());
                System.out.println("HAS ERRORS? " + originalTunit.getRelativeFilepath());
                if (errorOutput == null) {
                    System.out.println("NO");
                    continue;
                }
                System.out.println("YES");

                ClavaLog.debug("Trying to fix file '" + originalTunit.getRelativeFilepath() + "'");
                fuzzyFix(originalTunit, errorOutput);
            }
        }

        if (hasParsingErrors && currentIteration == maxIterations) {
            ClavaLog.debug("Stopping after achieving maximum number of iterations (" + maxIterations + ")");
        }

        ClavaLog.debug("Fuzzy parsing ended");
    }

    private void fuzzyFix(TranslationUnit tunit, String errorOutput) {
        System.out.println("FIXING " + tunit.getRelativeFilepath());
        System.out.println("ERROR:" + errorOutput);
    }

    /**
     * @param update if true, the weaver will update its state to use the rebuilt
     *               tree instead of the original tree
     */
    public boolean rebuildAst(boolean update) {
        // Check if inside apply

        // Write current tree to a temporary folder
        File tempFolder = REBUILD_WEAVING_FOLDERS.get().next();

        // Ensure folder is empty
        SpecsIo.deleteFolderContents(tempFolder);

        List<File> writtenFiles = getApp().write(tempFolder);
        ClavaLog.debug(() -> "Files written during rebuild: " + writtenFiles);

        Set<File> includeFolders = getSourceIncludeFoldersFromTempFolder(tempFolder);

        ClavaLog.debug(() -> "Include folders for rebuild, from folder '" + tempFolder + "': " + includeFolders);

        List<String> extraOptions = new ArrayList<>();

        // Add original includes as extra options, in case it needs any header file that
        // is excluded from parsing (e.g.,
        // .incl)
        List<File> originalHeaderIncludes = this.dataStore.get(CxxWeaverOption.HEADER_INCLUDES).getFiles();
        originalHeaderIncludes.stream().map(folder -> CxxWeaver.buildIncludeArg(folder.getAbsolutePath()))
                .forEach(extraOptions::add);

        List<String> rebuildOptions = new ArrayList<>();

        // Copy current options, removing previous normal includes
        var parserOptions = buildParserOptions(this.dataStore);
        parserOptions.stream()
                .filter(option -> !(option.startsWith("-I") || option.startsWith("-i")))
                .forEach(rebuildOptions::add);

        // Add include folders
        for (File includeFolder : includeFolders) {
            rebuildOptions.add(0, CxxWeaver.buildIncludeArg(includeFolder.getAbsolutePath()));
        }

        // Add extra includes
        for (File extraInclude : getExternalIncludeFolders()) {
            rebuildOptions.add(0, CxxWeaver.buildIncludeArg(extraInclude.getAbsolutePath()));
        }

        var previousBases = currentBases;
        var rebuildBases = new HashMap<File, File>();
        writtenFiles.stream()
                .forEach(writtenFile -> rebuildBases.put(SpecsIo.getCanonicalFile(writtenFile), tempFolder));

        currentBases = rebuildBases;
        App rebuiltApp = createApp(writtenFiles, rebuildOptions, extraOptions);

        // Restore current bases
        currentBases = previousBases;

        // Creating an app automatically pushes the App in the Context
        context.popApp();

        // Clear data
        ClavaData.clearAllCaches();

        // Base folder is now the temporary folder
        if (update) {
            currentBases = rebuildBases;

            // Discard current app
            weaverData.popAst();

            // Add rebuilt app
            weaverData.pushAst(rebuiltApp);

            // Create file->base map
            // Since files where all written to the same folder:
            // 1) If the parent folder is the same as the temp folder, it has not base
            // folder;
            // 2) Otherwise, the temp folder is the base folder
            Map<File, File> writtenFilesToBase = new HashMap<>();

            for (File writtenFile : writtenFiles) {

                // If the parent folder is the same as the temp folder, it has no base folder
                if (writtenFile.getParentFile().equals(tempFolder)) {
                    writtenFilesToBase.put(writtenFile, null);
                    continue;
                }

                // Calculate base folder as being the path next to temp folder
                String relativePath = SpecsIo.getRelativePath(writtenFile, tempFolder);

                int slashIndex = relativePath.indexOf('/');
                SpecsCheck.checkArgument(slashIndex != -1,
                        () -> "Expected to have at least one slash: " + relativePath);
                String sourceFoldername = relativePath.substring(0, slashIndex);

                writtenFilesToBase.put(writtenFile, new File(tempFolder, sourceFoldername));
                // File baseFolder = writtenFile.getParentFile().equals(tempFolder) ? null :
                // tempFolder;
                // writtenFilesToBase.put(writtenFile, baseFolder);
            }
            // writtenFiles.stream().forEach(
            // file -> file.getParentFile().equals(tempFolder) ? null :
            // writtenFilesToBase.put(file, tempFolder));

            updateSources(writtenFilesToBase);

            // baseFolder = tempFolder;
        }

        return rebuiltApp.get(App.IGNORED_FILES).size() == 0;

        // Clear user values, all stored nodes are invalid now
        // userValues = new HashMap<>();
        // Discard user values
        // userValuesStack.pop();
        // userValuesStack.push(new HashMap<>());
    }

    /**
     * Creates a new temporary folder for weaving.
     *
     * <p>
     * The folder will be deleted when the JVM exits.
     *
     * @return
     */
    private static File newTemporaryWeavingFolder() {

        File tempFolder = SpecsIo.getTempFolder(TEMP_WEAVING_FOLDER + "_" + UUID.randomUUID().toString());

        SpecsIo.deleteFolderContents(tempFolder);

        // Register temporary folder and its contents for deletion
        SpecsIo.deleteOnExit(tempFolder);

        return tempFolder.getAbsoluteFile();
    }

    public Object getUserField(ClavaNode node, String fieldName) {
        // Get node values
        Map<String, Object> values = getUserValues().get(node);
        if (values == null) {
            return null;
        }

        return values.get(fieldName);
    }

    public Object setUserField(ClavaNode node, String fieldName, Object value) {
        // Get node values
        Map<String, Object> values = getUserValues().get(node);

        // Initialize map if null
        if (values == null) {
            values = new HashMap<>();
            getUserValues().put(node, values);
        }

        return values.put(fieldName, value);
    }

    public List<File> getSources() {
        return currentSources;
    }

    public boolean clearUserField(ClavaNode node) {
        return getUserValues().remove(node) != null;
    }

    public static CxxWeaver getCxxWeaver() {
        return (CxxWeaver) getThreadLocalWeaver();
    }

    @Override
    public Set<String> getLanguages() {
        return LANGUAGES;
    }

    public void pushAst() {
        // Create a copy of app and push it
        App clonedApp = (App) getApp().copy(true);
        weaverData.pushAst(clonedApp);
    }

    public void pushAst(App app) {
        // Adjust context in case it is different
        if (app.getContext() != getContex()) {
            ClavaLog.debug("Pushing app with different context, might happen due to serialization/deserialization");
            app.set(ClavaNode.CONTEXT, getContex());
        }

        weaverData.pushAst(app);
    }

    public void popAst() {
        // Discard app and user values
        weaverData.popAst();
    }

    public Integer nextId(String prefix) {
        return accMap.add(prefix);
    }

    public List<Include> getAvailableIncludes() {
        // Search on sources and normal includes

        List<File> searchPaths = new ArrayList<>();
        searchPaths.addAll(getSources());
        searchPaths.addAll(this.dataStore.get(CxxWeaverOption.HEADER_INCLUDES).getFiles());
        // System.out.println("SEARCH PATHS:" + searchPaths);
        Set<String> includeFolders = searchPaths.stream()
                // .map(CxxWeaver::parseIncludePath)
                .map(path -> path.isFile() ? path.getParentFile().getAbsolutePath() : path.getAbsolutePath())
                .collect(Collectors.toSet());

        // System.out.println("INCLUDE PATHS:" + includeFolders);

        List<Include> includes = new ArrayList<>();

        Set<String> seenIncludes = new HashSet<>();
        for (String includeFolderName : includeFolders) {
            File includeFolder = new File(includeFolderName);

            // Get all files from folder
            // List<File> currentIncludes = SpecsIo.getFilesRecursive(includeFolder,
            // TranslationUnit.getHeaderExtensions());
            List<File> currentIncludes = SpecsIo.getFilesRecursive(includeFolder,
                    SourceType.HEADER.getExtensions());

            currentIncludes.stream()
                    // Filter already added includes
                    .filter(path -> seenIncludes.add(SpecsIo.getCanonicalPath(path)))
                    .map(currentInclude -> new Include(currentInclude,
                            SpecsIo.getRelativePath(currentInclude, includeFolder), -1, false))
                    .forEach(include -> includes.add(include));
        }

        return includes;
    }

    public static ClavaFactory getFactory() {
        return getContex().get(ClavaContext.FACTORY);
    }

    public static ClavaContext getContex() {
        return getCxxWeaver().getApp().getContext();
    }

    public static SnippetParser getSnippetParser() {
        return new SnippetParser(getContex());
    }

    /**
     * Helper method which returns include folders of the source files.
     *
     * <p>
     * For the temporary folder, the source folders are the children folders of the
     * temporary folder, and the temporary
     * folder itself.
     *
     * @param weavingFolder
     * @return
     */
    private Set<File> getSourceIncludeFoldersFromTempFolder(File weavingFolder) {
        return getSourceIncludeFolders(weavingFolder, false);
    }

    /**
     *
     */
    private Set<File> getSourceIncludeFolders(File weavingFolder, boolean onlyHeaders) {
        Set<File> includeFolders = new LinkedHashSet<>();
        includeFolders.addAll(SpecsIo.getFolders(weavingFolder));
        includeFolders.add(weavingFolder);

        return includeFolders;
    }

    private Set<File> getExternalIncludeFolders() {
        return getApp().getExternalDependencies().getExtraIncludes().stream()
                .map(SpecsIo::getCanonicalFile)
                .collect(Collectors.toCollection(() -> new LinkedHashSet<>()));
    }

    private Set<File> getAllIncludeFolders(File weavingFolder, Set<File> generatedFiles) {
        Set<File> includePaths = new LinkedHashSet<>();
        includePaths.addAll(getSourceIncludeFolders(weavingFolder, true));
        includePaths.addAll(getExternalIncludeFolders());

        // Add includes to manually generated files
        generatedFiles.stream()
                .filter(SourceType::isHeader)
                .map(File::getParentFile)
                .forEach(includePaths::add);

        return includePaths;
    }

    /**
     * Normalizes key paths to be only files
     */
    private Map<File, File> obtainFiles(Map<File, File> filesToBases) {
        var parserOptions = buildParserOptions(this.dataStore);

        Map<File, File> processedFiles = new HashMap<>();

        for (var sourceAndBase : filesToBases.entrySet()) {
            File source = sourceAndBase.getKey();
            // If a file, just add it (unless we are skipping header parsing)
            if (source.isFile()) {
                if (shouldSkipFile(source)) {
                    continue;
                }

                processedFiles.put(source, sourceAndBase.getValue());
            }

            // Process folder
            obtainFiles(sourceAndBase.getKey(), sourceAndBase.getValue(), processedFiles, parserOptions);
        }

        return processedFiles;

    }

    private boolean shouldSkipFile(File file) {
        return !this.dataStore.get(CxxWeaverOption.PARSE_INCLUDES) && SourceType.isHeader(file);
    }

    private void obtainFiles(File folder, File baseFolder, Map<File, File> processedFiles, List<String> parserOptions) {
        // All files specified by the user, header and implementation
        Set<String> extensions = SourceType.getPermittedExtensions();

        Map<String, File> allUserFilesMap = SpecsIo.getFileMap(Arrays.asList(folder), true, extensions,
                this::isCutoffFolder);
        ClavaLog.debug(() -> "All user sources: " + allUserFilesMap.values());

        // Convert to list, add header files in include folders if enabled
        List<String> allFiles = processSources(allUserFilesMap, parserOptions);
        ClavaLog.debug(() -> "All sources: " + allFiles);

        allFiles.stream().forEach(filename -> processedFiles.put(new File(filename), baseFolder));
    }

    @Override
    protected LanguageSpecification buildLangSpecs() {
        return buildLanguageSpecification();
    }

    @Override
    public List<String> getPredefinedExternalDependencies() {
        return SpecsCollections.concatList(super.getPredefinedExternalDependencies(), CLAVA_PREDEFINED_EXTERNAL_DEPS);
    }

    public int getStackSize() {
        return context.getStackSize();
    }

    @Override
    public AstMethods getAstMethods() {
        return new ClavaAstMethods(this, ClavaNode.class, node -> CxxJoinpoints.create(node),
                node -> ClavaCommonLanguage.getJoinPointName(node), node -> node.getScopeChildren());
    }

    public void clearAppHistory() {
        context.clearAppHistory();
    }
}

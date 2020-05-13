package pt.up.fe.specs.clava.weaver;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.lara.interpreter.joptions.config.interpreter.LaraiKeys;
import org.lara.interpreter.profile.WeavingReport;
import org.lara.interpreter.weaver.interf.AGear;
import org.lara.interpreter.weaver.interf.JoinPoint;
import org.lara.interpreter.weaver.options.WeaverOption;
import org.lara.language.specification.LanguageSpecification;
import org.lara.language.specification.dsl.LanguageSpecificationV2;
import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.storedefinition.StoreDefinition;
import org.suikasoft.jOptions.storedefinition.StoreDefinitionBuilder;

import pt.up.fe.specs.antarex.clava.AntarexClavaLaraApis;
import pt.up.fe.specs.antarex.clava.JsAntarexApiResource;
import pt.up.fe.specs.clang.ClangAstKeys;
import pt.up.fe.specs.clang.ClangAstParser;
import pt.up.fe.specs.clang.SupportedPlatform;
import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.Include;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.context.ClavaFactory;
import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.clava.parsing.snippet.SnippetParser;
import pt.up.fe.specs.clava.utils.SourceType;
import pt.up.fe.specs.clava.weaver.abstracts.weaver.ACxxWeaver;
import pt.up.fe.specs.clava.weaver.gears.InsideApplyGear;
import pt.up.fe.specs.clava.weaver.gears.ModifiedFilesGear;
import pt.up.fe.specs.clava.weaver.importable.AstFactory;
import pt.up.fe.specs.clava.weaver.importable.ClavaPlatforms;
import pt.up.fe.specs.clava.weaver.importable.Format;
import pt.up.fe.specs.clava.weaver.importable.LowLevelApi;
import pt.up.fe.specs.clava.weaver.joinpoints.CxxProgram;
import pt.up.fe.specs.clava.weaver.options.CxxWeaverOption;
import pt.up.fe.specs.clava.weaver.options.CxxWeaverOptions;
import pt.up.fe.specs.lang.SpecsPlatforms;
import pt.up.fe.specs.lara.LaraExtraApis;
import pt.up.fe.specs.lara.langspec.LangSpecsXmlParser;
import pt.up.fe.specs.lara.unit.LaraUnitLauncher;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.collections.AccumulatorMap;
import pt.up.fe.specs.util.csv.CsvField;
import pt.up.fe.specs.util.csv.CsvWriter;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.parsing.arguments.ArgumentsParser;
import pt.up.fe.specs.util.providers.ResourceProvider;
import pt.up.fe.specs.util.utilities.Buffer;
import pt.up.fe.specs.util.utilities.LineStream;
import pt.up.fe.specs.util.utilities.ProgressCounter;
import pt.up.fe.specs.util.utilities.StringLines;

/**
 * Weaver Implementation for CxxWeaver<br>
 * Since the generated abstract classes are always overwritten, their implementation should be done by extending those
 * abstract classes with user-defined classes.<br>
 * The abstract class {@link pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint} can be used to add user-defined
 * methods and fields which the user intends to add for all join points and are not intended to be used in LARA aspects.
 *
 * @author Lara Weaver Generator
 */
public class CxxWeaver extends ACxxWeaver {

    static {
        // DevUtils.addDevProject(
        // new File("C:\\Users\\JoaoBispo\\Desktop\\shared\\specs-lara\\2017 COMLAN\\RangeValueMonitor\\lara"),
        // "COMLAN",
        // true, true);
    }

    public static LanguageSpecificationV2 buildLanguageSpecification() {
        // var langSpecV1 = LanguageSpecification.newInstance(ClavaWeaverResource.JOINPOINTS,
        // ClavaWeaverResource.ARTIFACTS,
        // ClavaWeaverResource.ACTIONS, true);
        //
        // return JoinPointFactory.fromOld(langSpecV1);
        // System.out.println("JPS: " + ClavaWeaverResource.JOINPOINTS.read());
        return LangSpecsXmlParser.parse(ClavaWeaverResource.JOINPOINTS, ClavaWeaverResource.ARTIFACTS,
                ClavaWeaverResource.ACTIONS, true);
    }

    /**
     * @deprecated
     * @return
     */
    @Deprecated
    public static LanguageSpecification buildLanguageSpecificationOld() {
        return LanguageSpecification.newInstance(ClavaWeaverResource.JOINPOINTS, ClavaWeaverResource.ARTIFACTS,
                ClavaWeaverResource.ACTIONS, true);
    }

    // private static final boolean SHOW_MEMORY_USAGE = true;

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

    // private static final Set<String> EXTENSIONS_IMPLEMENTATION = new HashSet<>(Arrays.asList(
    // "c", "cpp"));
    // private static final Set<String> EXTENSIONS_HEADERS = new HashSet<>(Arrays.asList("h", "hpp"));

    public static String getWovenCodeFoldername() {
        return WOVEN_CODE_FOLDERNAME;
    }

    private static final List<String> DEFAULT_COMMON_DUMPER_FLAGS = Arrays.asList("-Wno-unknown-pragmas");
    // private static final List<String> DEFAULT_DUMPER_FLAGS = Arrays.asList();

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

    private static final List<ResourceProvider> CLAVA_LARA_API = new ArrayList<>();
    static {
        CLAVA_LARA_API.addAll(LaraExtraApis.getApis());
        CLAVA_LARA_API.addAll(ClavaLaraApis.getApis());
        CLAVA_LARA_API.addAll(AntarexClavaLaraApis.getApis());
    }

    private static final List<Class<?>> CLAVA_IMPORTABLE_CLASSES = new ArrayList<>();
    static {
        CLAVA_IMPORTABLE_CLASSES.addAll(LaraExtraApis.getImportableClasses());
        CLAVA_IMPORTABLE_CLASSES.addAll(ClavaLaraApis.getImportableClasses());
        CLAVA_IMPORTABLE_CLASSES.addAll(
                Arrays.asList(SpecsPlatforms.class, AstFactory.class, Format.class, LowLevelApi.class, CsvWriter.class,
                        CsvField.class, ProgressCounter.class, ClavaPlatforms.class, ClavaWeaverLauncher.class,
                        ArgumentsParser.class, CxxWeaverApi.class));

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

    // Weaver configuration
    private DataStore args = null;

    // Parsing Context
    private final ClavaContext context;

    // Gears
    private final ModifiedFilesGear modifiedFilesGear;
    private final InsideApplyGear insideApplyGear;

    // Parsed program state
    // private Deque<App> apps;
    // private Deque<Map<ClavaNode, Map<String, Object>>> userValuesStack;

    // private File outputDir = null;
    // private List<File> originalSources = null;
    private List<File> currentSources = null;
    private Map<File, File> currentBases = null;
    private Map<File, String> sourceFoldernames = null;
    private List<File> currentSourceFolders = null;
    private List<String> originalSourceFolders = null;

    private List<String> userFlags = null;
    // private CxxJoinpoints jpFactory = null;

    // private File baseFolder = null;
    // private List<String> parserOptions = new ArrayList<>();

    // private Logger infoLogger = null;
    // private Level previousLevel = null;

    private Set<String> messagesToUser;

    private AccumulatorMap<String> accMap;

    private ClavaWeaverData weaverData;

    private final ClavaMetrics metrics;

    public CxxWeaver() {
        // Gears
        this.modifiedFilesGear = new ModifiedFilesGear();
        this.insideApplyGear = new InsideApplyGear();

        context = new ClavaContext();

        // Weaver configuration
        args = null;

        // outputDir = null;
        currentSources = new ArrayList<>();
        userFlags = null;

        // baseFolder = null;
        // parserOptions = new ArrayList<>();

        // infoLogger = null;
        // previousLevel = null;

        // Set, in order to filter repeated messages
        // Linked, to preserve order
        messagesToUser = null;

        accMap = null;

        weaverData = null;

        metrics = new ClavaMetrics();
        this.setWeaverProfiler(metrics);
    }

    public WeavingReport getWeavingReport() {
        return metrics.getReport();
    }

    public ClavaWeaverData getWeaverData() {
        return weaverData;
    }

    public App getApp() {
        // if (args.get(CxxWeaverOption.DISABLE_WEAVING)) {
        // throw new RuntimeException("Tried to access top-level node, but weaving is disabled");
        // }

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

    // public File getBaseSourceFolder() {
    // return baseFolder;
    // }

    public CxxProgram getAppJp() {
        return CxxJoinpoints.programFactory(getApp());
    }

    private Map<ClavaNode, Map<String, Object>> getUserValues() {
        return weaverData.getUserValues();
        // return userValuesStack.peek();
    }

    public boolean addMessageToUser(String message) {
        return messagesToUser.add(message);
    }

    /**
     * Warns the lara interpreter if the weaver accepts a folder as the application or only one file at a time.
     *
     * @return true if the weaver is able to work with several files, false if only works with one file
     */
    @Override
    public boolean handlesApplicationFolder() {
        // Can the weaver handle an application folder?
        return true;
    }

    /**
     * Set a file/folder in the weaver if it is valid file/folder type for the weaver.
     *
     * @param source
     *            the file with the source code
     * @param outputDir
     *            output directory for the generated file(s)
     * @param args
     *            arguments to start the weaver
     * @return true if the file type is valid
     */
    @Override
    public boolean begin(List<File> sources, File outputDir, DataStore args) {
        reset();

        // Set args
        this.args = args;

        // TODO: Temporarily disabled
        // ClavaLog.debug(() -> "Clava Weaver arguments: " + args);

        // Add normal include folders to the sources
        // sources.addAll(args.get(CxxWeaverOption.HEADER_INCLUDES).getFiles());

        weaverData = new ClavaWeaverData(args);

        accMap = new AccumulatorMap<>();

        // Logger.getLogger(LoggingUtils.INFO_TAG).setLevel(Level.WARNING);
        //
        // Logger.getLogger(LoggingUtils.INFO_TAG).setUseParentHandlers(false);

        if (args.get(CxxWeaverOption.DISABLE_CLAVA_INFO)) {
            // System.out.println("DISABLING CLAVA INFO FOR LOGGER " + SpecsLogs.getSpecsLogger());
            // Needs to keep a strong reference, or it can be garbage collected
            /*
            infoLogger = Logger.getLogger(SpecsLogs.INFO_TAG);
            previousLevel = infoLogger.getLevel();
            infoLogger.setLevel(Level.WARNING);
            */
            SpecsLogs.getSpecsLogger().setLevelAll(Level.WARNING);
            ClavaLog.getLogger().setLevelAll(Level.WARNING);
        }

        // boolean disableWeaving = args.get(CxxWeaverOption.DISABLE_WEAVING);

        // Set weaver in CxxFactory, so that it can have access to a weaver configuration
        // This setting is local to the thread

        // Weaver arguments
        // this.originalSources = sources;
        // this.currentSources = buildSources(sources, args.get(LaraiKeys.WORKSPACE_EXTRA));

        // Create map with all sources, mapped to the corresponding base folder
        Map<File, File> allSources = new HashMap<>();
        for (var source : sources) {
            if (source.isFile()) {
                allSources.put(source, null);
                continue;
            }

            if (source.isDirectory()) {
                allSources.put(source, source);
                continue;
            }

            throw new RuntimeException("Case not implemented: " + source);
        }

        allSources.putAll(args.get(LaraiKeys.WORKSPACE_EXTRA));
        updateSources(allSources);

        // Store original source folders
        // This means the current bases + folders of the sources
        Set<String> originalSourceFoldersSet = new LinkedHashSet<>();

        currentBases.values().stream()
                .filter(base -> base != null)
                .map(CxxWeaver::parseIncludePath)
                .forEach(originalSourceFoldersSet::add);

        getSources().stream()
                .map(CxxWeaver::parseIncludePath)
                .forEach(originalSourceFoldersSet::add);

        originalSourceFolders = new ArrayList<>(originalSourceFoldersSet);
        // System.out.println("ORIGINAL SOURCES: " + originalSourceFolders);
        // updateSources(sources, args.get(LaraiKeys.WORKSPACE_EXTRA));

        // this.outputDir = outputDir;

        // If args does not have a standard, add a standard one based on the input files
        if (!this.args.hasValue(ClavaOptions.STANDARD)) {
            this.args.add(ClavaOptions.STANDARD, getStandard());
        }

        var parserOptions = buildParserOptions(args);

        // Initialize weaver with the input file/folder

        // First folder is considered the base folder
        // Only needs folder if we are doing weaving
        // if (!disableWeaving) {
        // baseFolder = getFirstSourceFolder(sources);
        // }

        // Init messages to user
        messagesToUser = new LinkedHashSet<>();

        // If weaving disabled, create empty App
        if (args.get(CxxWeaverOption.DISABLE_WEAVING)) {
            SpecsLogs.msgInfo("Initial parsing disabled, creating empty 'program'");

            App emptyApp = context.get(ClavaContext.FACTORY).app(Collections.emptyList());
            // First app, add it to context
            context.pushApp(emptyApp);
            weaverData.pushAst(emptyApp);
            return true;
        }

        // weaverData.pushAst(createApp(sources, parserOptions));
        weaverData.pushAst(createApp(getSources(), parserOptions));

        // TODO: Option to dump clang and clava
        SpecsIo.write(new File("clavaDump.txt"), getApp().toString());

        return true;
    }

    private List<String> buildParserOptions(DataStore args) {
        List<String> parserOptions = new ArrayList<>();

        // Initialize list of options for parser
        parserOptions = new ArrayList<>();

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
     * 1) Single files listed in 'sources' are considered to not have a base folder (relative path is null) <br>
     * 2) Source folders listed in 'sources' will have the parent folder as its base folder <br>
     * 3) Source files or folders listed in 'map' will have the base folder indicated in the map
     * 
     */
    private void updateSources(Map<File, File> map) {
        // TODO: Convert all folders to files, folders become bases when in sources
        map = obtainFiles(map);

        /// Sources
        this.currentSources = new ArrayList<>();
        // currentSources.addAll(sources);
        currentSources.addAll(map.keySet());

        // String datastoreFolderpath = args.get(JOptionKeys.CURRENT_FOLDER_PATH);
        // File datastoreFolder = datastoreFolderpath == null ? null : new File(datastoreFolderpath);

        /// Bases
        currentBases = new HashMap<>();

        for (var entry : map.entrySet()) {
            if (entry.getKey().isDirectory()) {
                continue;
            }

            // File base = entry.getValue().equals(datastoreFolder) ? null : entry.getValue();
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

    private void reset() {
        // Reset gears
        modifiedFilesGear.reset();
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
        // String std = getStandard().getString();
        //
        // return "-std=" + std;

        Standard standard = args.get(ClavaOptions.STANDARD);
        return "-std=" + standard.getString();

    }

    public Standard getStandard() {

        if (args.hasValue(ClavaOptions.STANDARD)) {
            return args.get(ClavaOptions.STANDARD);
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
            SpecsLogs.msgWarn("Source folders are not set");
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
            SpecsLogs.msgWarn("Source folders are not set");
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
        // Preconditions.checkArgument(!sources.isEmpty(), "Needs at least one source specified (file or folder)");

        if (sources.isEmpty()) {
            return SpecsIo.getWorkingDir();
        }

        File firstSource = sources.stream()
                .filter(source -> source.exists())
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Needs to specify at least one source (file or folder) that exists, found none. Input sources:"
                                + sources));

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
        if (!folderAbsPath.contains(" ")) {
            return folderAbsPath;
        }
        
        // If on Windows, do not escape, or it will not work:
        // https://coderanch.com/t/627514/java/ProcessBuilder-incorrectly-processes-embedded-spaces
        if (SpecsPlatforms.isWindows()) {
            return folderAbsPath;
        }
        
        return "\"" + folderAbsPath + "\"";
        */
    }

    public App createApp(List<File> sources, List<String> parserOptions) {
        return createApp(sources, parserOptions, Collections.emptyList());
    }

    /**
     *
     * @param sources
     * @param parserOptions
     * @param extraOptions
     *            options that should not be processed (e.g., header files found in folders specified by -I flags are
     *            automatically added to the compilation, if we want to add header folders whose header files should not
     *            be parsed, they can be specified here)
     * @return
     */
    public App createApp(List<File> sources, List<String> parserOptions, List<String> extraOptions) {
        ClavaLog.debug(() -> "Creating App from the following sources: " + sources);
        ClavaLog.debug(() -> "Creating App using the following options: " + parserOptions);
        ClavaLog.debug(() -> "Creating App using the following extra options: " + extraOptions);

        /*
        // List<File> adaptedSources = adaptSources(sources, parserOptions);
        // ClavaLog.debug(() -> "Adapted sources: " + adaptedSources);
        
        // System.out.println("ADAPTED SOURCES:" + adaptedSources);
        
        // App newApp = new CodeParser().parseParallel(sources, parserOptions);
        // System.out.println("APP SOURCE:" + newApp.getCode());
        
        // List<File> allSourceFolders = getInputSourceFolders(sources, parserOptions);
        // Map<String, File> allSources = SpecsIo.getFileMap(allSourceFolders, SourceType.getPermittedExtensions());
        // System.out.println("ALL SOURCE FOLDERS:" + allSourceFolders);
        // System.out.println("ALL SOURCES:" + allSources);
        
        // All files specified by the user, header and implementation
        Set<String> extensions = SourceType.getPermittedExtensions();
        // Map<String, File> allFilesMap = SpecsIo.getFileMap(adaptedSources, true, extensions, this::isCutoffFolder);
        Map<String, File> allUserFilesMap = SpecsIo.getFileMap(sources, true, extensions, this::isCutoffFolder);
        ClavaLog.debug(() -> "All user sources: " + allUserFilesMap.values());
        
        // Map<String, File> allFilesMap = SpecsIo.getFileMap(adaptedSources, SourceType.getPermittedExtensions());
        // System.out.println("ALL FILES MAP:" + allFilesMap);
        
        // List<String> implementationFilenames = processSources(sources);
        
        // Convert to list, add header files in include folders if enabled
        List<String> allFiles = processSources(allUserFilesMap, parserOptions);
        ClavaLog.debug(() -> "All sources: " + allFiles);
        
        // System.out.println("ALL FILES:" + allFiles);
        
        // TODO: If option to separe include folders in generation is on, it should return just that folder
        // List<File> includeFolders = sources;
        
        // addFlagsFromFiles(includeFolders, filenames, parserOptions);
        // addFlagsFromFiles(allFiles, parserOptions);
        */

        // Collect additional include folders
        Set<String> sourceIncludeFolders = getSourceIncludes(sources);
        ClavaLog.debug(() -> "Source include folders: " + sourceIncludeFolders);
        // originalSourceFolders
        // Set<String> sourceIncludeFolders =

        // Add include folders to extra options
        List<String> adaptedExtraOptions = new ArrayList<>(sourceIncludeFolders.size() + extraOptions.size());
        adaptedExtraOptions.addAll(extraOptions);
        sourceIncludeFolders.stream().map(includeFolder -> "-I" + includeFolder).forEach(adaptedExtraOptions::add);
        // System.out.println("EXTRA OPTIONS: " + extraOptions);
        // System.out.println("ADAPTED EXTRA OPTIONS: " + adaptedExtraOptions);
        List<String> allFiles = sources.stream().map(File::toString).collect(Collectors.toList());

        // Sort filenames so that select order of files is consistent between OSes
        Collections.sort(allFiles);

        boolean useCustomResources = getConfig().get(ClavaOptions.CUSTOM_RESOURCES);

        CodeParser codeParser = CodeParser.newInstance();
        codeParser.set(CodeParser.USE_CUSTOM_RESOURCES, useCustomResources);
        codeParser.set(ParallelCodeParser.PARALLEL_PARSING, getConfig().get(ParallelCodeParser.PARALLEL_PARSING));
        codeParser.set(ParallelCodeParser.PARSING_NUM_THREADS, getConfig().get(ParallelCodeParser.PARSING_NUM_THREADS));
        codeParser.set(ParallelCodeParser.SYSTEM_INCLUDES_THRESHOLD,
                getConfig().get(ParallelCodeParser.SYSTEM_INCLUDES_THRESHOLD));
        codeParser.set(ParallelCodeParser.CONTINUE_ON_PARSING_ERRORS,
                getConfig().get(ParallelCodeParser.CONTINUE_ON_PARSING_ERRORS));
        codeParser.set(ClangAstKeys.USE_PLATFORM_INCLUDES, getConfig().get(ClangAstKeys.USE_PLATFORM_INCLUDES));

        List<String> allParserOptions = new ArrayList<>(parserOptions.size() + adaptedExtraOptions.size());
        allParserOptions.addAll(parserOptions);
        allParserOptions.addAll(adaptedExtraOptions);
        App app = codeParser.parse(SpecsCollections.map(allFiles, File::new), allParserOptions, context);

        // Set source paths of each TranslationUnit
        // app.setSourcesFromStrings(allFiles);
        app.setSources(currentBases);
        app.setSourceFoldernames(sourceFoldernames);

        // Set options

        // Set external dependencies
        app.getExternalDependencies()
                .setDisableRemoteDependencies(getConfig().get(ClavaOptions.DISABLE_REMOTE_DEPENDENCIES));

        return app;

        /*
        // TODO: parse should receive File instead of String?
        long tic = System.nanoTime();
        
        // boolean disableNewParsingMethod = getConfig().get(ClavaOptions.DISABLE_CLAVA_DATA_NODES);
        // ClangRootNode ast = new ClangAstParser(false, useCustomResources, disableNewParsingMethod).parse(
        ClangRootNode ast = new ClangAstParser(false, useCustomResources).parse(
                implementationFilenames,
                parserOptions);
        
        SpecsLogs.msgInfo(SpecsStrings.takeTime("Clang Parsing and Dump", tic));
        if (SHOW_MEMORY_USAGE) {
            SpecsLogs
                    .msgInfo("Current memory used (Java):" + SpecsStrings.parseSize(SpecsSystem.getUsedMemory(true)));
        }
        
        try (ClavaParser clavaParser = new ClavaParser(ast)) {
            tic = System.nanoTime();
            App app = clavaParser.parse();
            // System.out.println("ALL FILES: " + allFiles);
            // Set source paths of each TranslationUnit
            app.setSourcesFromStrings(allFiles);
        
            // Set options
        
            // Set external dependencies
            app.getExternalDependencies()
                    .setDisableRemoteDependencies(getConfig().get(ClavaOptions.DISABLE_REMOTE_DEPENDENCIES));
        
            // app.setSources(sources);
            SpecsLogs.msgInfo(SpecsStrings.takeTime("Clang AST to Clava", tic));
        
            // tic = System.nanoTime();
            // ClavaPragmas.processClavaPragmas(app);
            // SpecsLogs.msgInfo(SpecsStrings.takeTime("Weaver AST processing", tic));
        
            if (SHOW_MEMORY_USAGE) {
                SpecsLogs.msgInfo("Current memory used (Java):"
                        + SpecsStrings.parseSize(SpecsSystem.getUsedMemory(true)));
                // LoggingUtils.msgInfo("Heap size (Java):" + ParseUtils.parseSize(Runtime.getRuntime().maxMemory()));
            }
        
            System.out.println("APP:" + app.toTree());
        
            return app;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        */

    }

    private Set<String> getSourceIncludes(List<File> sources) {
        Set<String> sourceIncludeFolders = new LinkedHashSet<>();

        // Add folders of source files as include folders
        sources.stream()
                .map(CxxWeaver::parseIncludePath)
                .forEach(sourceIncludeFolders::add);

        // Add original source folders as includes if skipping header parsing
        // if (args.get(CxxWeaverOption.SKIP_HEADER_INCLUDES_PARSING)) {
        sourceIncludeFolders.addAll(originalSourceFolders);
        // System.out.println("ORIGINAL SOURCE FOLDERS: " + originalSourceFolders);
        // originalSourceFolders.stream().forEach(sourceIncludeFolders::add);
        // }
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

    /**
     * Adapts initial source files.
     *
     * <p>
     * E.g., if compiling for CMake, adds normal include folders as source folders.
     *
     * @param sources
     * @param parserOptions2
     * @return
     */
    /*
    private List<File> adaptSources(List<File> sources, List<String> parserOptions) {
        List<File> adaptedSources = new ArrayList<>(sources);
        // if (args.get(CxxWeaverOption.GENERATE_CMAKE_HELPER_FILES)) {
    
        // Add header files in normal include folders to the tree
        if (!args.get(CxxWeaverOption.SKIP_HEADER_INCLUDES_PARSING)) {
            // Use parser options instead of weaver options, it can be a rebuild with other folders
            List<File> headerIncludes = parserOptions.stream()
                    .filter(option -> option.startsWith("-I"))
                    .map(option -> new File(option.substring("-I".length())))
                    .collect(Collectors.toList());
    
            adaptedSources.addAll(headerIncludes);
    
            ClavaLog.debug(() -> "Adding the following header includes from the options: " + headerIncludes);
            ClavaLog.debug(() -> "Original header includes: " + args.get(CxxWeaverOption.HEADER_INCLUDES));
            // for (File includeFolder : args.get(CxxWeaverOption.HEADER_INCLUDES)) {
            // adaptedSources.add(includeFolder);
            // // parserOptions.add("-I" + parseIncludePath(includeFolder));
            // }
        }
    
        // parserOptions.stream()
        // .filter(option -> option.startsWith("-I"))
        // .map(option -> option.substring("-I".length()))
        // .forEach(includeFolder -> adaptedSources.add(new File(includeFolder)));
        // }
    
        return adaptedSources;
    }
    */
    private List<String> processSources(Map<String, File> sourceFiles, List<String> parserOptions) {

        SpecsCheck.checkArgument(!sourceFiles.isEmpty(),
                () -> "No C/C++ files found in the given source folders:" + getSources());

        Set<String> adaptedSources = new HashSet<>();

        // boolean skipHeaderFiles = args.get(CxxWeaverOption.SKIP_HEADER_INCLUDES_PARSING);
        boolean skipHeaderFiles = !args.get(CxxWeaverOption.PARSE_INCLUDES);

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
            // Use parser options instead of weaver options, it can be a rebuild with other folders
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
            // ClavaLog.debug(() -> "Original header includes: " + args.get(CxxWeaverOption.HEADER_INCLUDES));
            // for (File includeFolder : args.get(CxxWeaverOption.HEADER_INCLUDES)) {
            // adaptedSources.add(includeFolder);
            // // parserOptions.add("-I" + parseIncludePath(includeFolder));
            // }
        }

        return new ArrayList<>(adaptedSources);
        // return sourceFiles.keySet().stream()
        // .collect(Collectors.toList());
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
     * Return a JoinPoint instance of the language root, i.e., an instance of AProgram
     *
     * @return an instance of the join point root/program
     */
    @Override
    public JoinPoint select() {
        return CxxJoinpoints.create(getApp());
    }

    public String getProgramName() {
        return getFirstSourceFolder(getSources()).getName();
        // return getSources().stream()
        // .findFirst()
        // .map(File::getName)
        // .orElse("C/C++ Program");
        // return baseFolder.getName();
    }

    public File getWeavingFolder() {

        String outputFoldername = args.get(CxxWeaverOption.WOVEN_CODE_FOLDERNAME);
        if (outputFoldername.isEmpty()) {
            ClavaLog.info("No name defined for the output folder, using default value 'output'");
            outputFoldername = "output";
        }
        // System.out.println("OUTPUT FOLDER:" + args.get(LaraiKeys.OUTPUT_FOLDER));
        // System.out.println("WOVEN CODE FOLDERNAME:" + args.get(CxxWeaverOption.WOVEN_CODE_FOLDERNAME));
        // return SpecsIo.mkdir(outputDir, args.get(CxxWeaverOption.WOVEN_CODE_FOLDERNAME));
        File outputFolder = SpecsIo.mkdir(args.get(LaraiKeys.OUTPUT_FOLDER), outputFoldername);

        return outputFolder;
    }

    /**
     * Closes the weaver to the specified output directory location, if the weaver generates new file(s)
     *
     * @return if close was successful
     */
    @Override
    public boolean close() {

        // if (!args.get(CxxWeaverOption.DISABLE_WEAVING)) {
        // Process App files
        if (!getApp().getTranslationUnits().isEmpty()) {
            if (args.get(CxxWeaverOption.CHECK_SYNTAX)) {
                SpecsLogs.msgInfo("Checking woven code syntax...");
                rebuildAst(false);
                // rebuildAst(true);
            }

            // Terminate weaver execution with final steps required and writing output files

            // Write output files if code generation is not disabled
            if (!args.get(CxxWeaverOption.DISABLE_CODE_GENERATION)) {
                writeCode(getWeavingFolder());
            }

            // Write CMake helper files
            if (args.get(CxxWeaverOption.GENERATE_CMAKE_HELPER_FILES)) {
                generateCmakerHelperFiles();
            }

        }

        /// Clean-up phase

        // if (!SpecsSystem.isDebug()) {
        // Delete temporary weaving folder, if exists
        SpecsIo.deleteFolder(new File(TEMP_WEAVING_FOLDER));

        // Delete temporary source folder, if exists
        SpecsIo.deleteFolder(new File(TEMP_SRC_FOLDER));

        // }

        // Delete intermediary files
        if (args.get(CxxWeaverOption.CLEAN_INTERMEDIATE_FILES))

        {
            for (String tempFile : ClangAstParser.getTempFiles()) {
                new File(tempFile).delete();
            }
        }

        // Re-enable output
        if (args.get(CxxWeaverOption.DISABLE_CLAVA_INFO)) {
            // System.out.println("REENABLING CLAVA INFO FOR LOGGER " + SpecsLogs.getSpecsLogger());
            /*
            Preconditions.checkNotNull(infoLogger);
            infoLogger.setLevel(previousLevel);
            infoLogger = null;
            previousLevel = null;
            */
            SpecsLogs.getSpecsLogger().setLevelAll(null);
            ClavaLog.getLogger().setLevelAll(null);
        }

        if (!messagesToUser.isEmpty()) {
            ClavaLog.info(" - Messages -");
            messagesToUser.forEach(ClavaLog::info);
        }

        // Clear weaver data
        weaverData = null;

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

        // Add original includes at the end, in case there are "out-of-source" files (e.g., .inc files) that are needed
        newIncludeDirs.addAll(args.get(CxxWeaverOption.HEADER_INCLUDES).getFiles());

        // Add includes from original sources
        originalSourceFolders.stream().map(sourceFolder -> new File(sourceFolder))
                .forEach(newIncludeDirs::add);

        // If we are skipping the parsing of include folders, we should include the original include folders as includes
        // if (args.get(CxxWeaverOption.SKIP_HEADER_INCLUDES_PARSING)) {
        // List<File> originalHeaderIncludes = args.get(CxxWeaverOption.HEADER_INCLUDES).getFiles();
        // newIncludeDirs.addAll(originalHeaderIncludes);
        // ClavaLog.debug(() -> "Skip headers is enabled, adding original headers: " + originalHeaderIncludes);
        // }

        // String includeFoldersContent = getIncludePaths(getWeavingFolder()).stream().collect(Collectors.joining(";"));
        String includeFoldersContent = newIncludeDirs.stream()
                // .map(File::getAbsolutePath)
                .map(SpecsIo::getCanonicalPath)
                .map(SpecsIo::normalizePath)
                .collect(Collectors.joining(";"));

        File cmakeIncludes = new File(getWeavingFolder(), CMAKE_INCLUDE_DIRS_FILENAME);
        SpecsIo.write(cmakeIncludes, includeFoldersContent);
    }

    @Override
    public void writeCode(File outputFolder) {
        // If copy files is enabled, first copy source files to output folder
        if (getConfig().get(CxxWeaverOption.COPY_FILES_IN_SOURCES)) {
            // for (File source : getSources()) {
            // // If file, just copy the file
            // if (source.isFile()) {
            // SpecsIo.copy(source, new File(outputFolder, source.getName()));
            // continue;
            // }
            //
            // if (source.isDirectory()) {
            // File destFolder = SpecsIo.mkdir(outputFolder, source.getName());
            // SpecsIo.copyFolderContents(source, destFolder);
            // continue;
            // }
            //
            // SpecsLogs.warn("Case not defined for source '" + source + "', is neither a file or a folder");
            // }

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

            // If file already exists, and is the same as the file that we are about to write, skip
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
        if (!args.get(CxxWeaverOption.GENERATE_MODIFIED_CODE_ONLY)) {
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
     * @return a list of implementations of {@link AGear} or null if no gears are available
     */
    @Override
    public List<AGear> getGears() {
        return Arrays.asList(modifiedFilesGear, insideApplyGear);
    }

    @Override
    public List<WeaverOption> getOptions() {
        return CxxWeaverOption.STORE_DEFINITION.getKeys().stream()
                .map(CxxWeaverOptions::getOption)
                .collect(Collectors.toList());

    }

    @Override
    public LanguageSpecification getLanguageSpecification() {
        return buildLanguageSpecificationOld();
    }

    @Override
    public String getName() {
        // v1.2.2
        return "Clava";
    }

    public DataStore getConfig() {
        return args;
    }

    public TranslationUnit rebuildFile(TranslationUnit tUnit) {

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
        var parserOptions = buildParserOptions(args);
        parserOptions.stream()
                .filter(option -> !option.startsWith("-I"))
                .forEach(rebuildOptions::add);

        // Add include folders
        for (File includeFolder : includeFolders) {
            // rebuildOptions.add(0, "\"-I" + includeFolder.getAbsolutePath() + "\"");
            rebuildOptions.add(0, "-I" + includeFolder.getAbsolutePath());
        }

        // Add extra includes
        for (File extraInclude : getExternalIncludeFolders()) {
            // rebuildOptions.add(0, "\"-I" + extraInclude.getAbsolutePath() + "\"");
            rebuildOptions.add(0, "-I" + extraInclude.getAbsolutePath());
        }

        // Write the other translation units and add folder as includes, in case they are needed
        String currentCodeFoldername = TEMP_WEAVING_FOLDER + "_for_file_rebuild";
        File currentCodeFolder = SpecsIo.mkdir(currentCodeFoldername).getAbsoluteFile();
        SpecsIo.deleteFolderContents(currentCodeFolder, true);

        // Add include
        // rebuildOptions.add(0, "\"-I" + currentCodeFolder.getAbsolutePath() + "\"");
        rebuildOptions.add(0, "-I" + currentCodeFolder.getAbsolutePath());

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

        // SpecsCheck.checkArgument(rebuiltApp.getTranslationUnits().size() == 1,
        // () -> "Expected number of translation units to be 1, got " + rebuiltApp.getTranslationUnits().size()
        // + ":\n" + rebuiltApp.getTranslationUnits());

        // After rebuilding, clear current app cache
        getApp().clearCache();

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
     *
     * @param update
     *            if true, the weaver will update its state to use the rebuilt tree instead of the original tree
     */
    public void rebuildAst(boolean update) {
        // Check if inside apply

        // Write current tree to a temporary folder
        File tempFolder = REBUILD_WEAVING_FOLDERS.get().next();

        // Ensure folder is empty
        SpecsIo.deleteFolderContents(tempFolder);

        List<File> writtenFiles = getApp().write(tempFolder);
        ClavaLog.debug(() -> "Files written during rebuild: " + writtenFiles);

        Set<File> includeFolders = getSourceIncludeFoldersFromTempFolder(tempFolder);

        /*
        // If we are skipping the parsing of include folders, we should include the original include folders as includes
        if (args.get(CxxWeaverOption.SKIP_HEADER_INCLUDES_PARSING)) {
            List<File> originalHeaderIncludes = args.get(CxxWeaverOption.HEADER_INCLUDES).getFiles();
            includeFolders.addAll(originalHeaderIncludes);
            ClavaLog.debug(
                    () -> "Skip headers is enabled, adding original headers to rebuild: " + originalHeaderIncludes);
        }
        */

        ClavaLog.debug(() -> "Include folders for rebuild, from folder '" + tempFolder + "': " + includeFolders);

        List<String> extraOptions = new ArrayList<>();

        // Add original includes as extra options, in case it needs any header file that is excluded from parsing (e.g.,
        // .incl)
        List<File> originalHeaderIncludes = args.get(CxxWeaverOption.HEADER_INCLUDES).getFiles();
        originalHeaderIncludes.stream().map(folder -> "-I" + folder.getAbsolutePath())
                .forEach(extraOptions::add);
        // includeFolders.addAll(originalHeaderIncludes);

        // ClavaLog.debug(() -> "All include folders for rebuild" + includeFolders);

        List<String> rebuildOptions = new ArrayList<>();

        // Copy current options, removing previous normal includes
        var parserOptions = buildParserOptions(args);
        parserOptions.stream()
                .filter(option -> !option.startsWith("-I"))
                .forEach(rebuildOptions::add);
        // rebuildOptions.addAll(parserOptions);

        // Add include folders
        for (File includeFolder : includeFolders) {
            // rebuildOptions.add(0, "\"-I" + includeFolder.getAbsolutePath() + "\"");
            rebuildOptions.add(0, "-I" + includeFolder.getAbsolutePath());
        }

        // Add extra includes
        // for (File extraInclude : getApp().getExternalDependencies().getExtraIncludes()) {
        for (File extraInclude : getExternalIncludeFolders()) {
            // rebuildOptions.add(0, "\"-I" + extraInclude.getAbsolutePath() + "\"");
            rebuildOptions.add(0, "-I" + extraInclude.getAbsolutePath());
        }

        // App rebuiltApp = createApp(srcFolders, rebuildOptions);
        // List<File> srcFolders = new ArrayList<>(includeFolders);

        // App rebuiltApp = createApp(srcFolders, rebuildOptions);

        var previousBases = currentBases;
        var rebuildBases = new HashMap<File, File>();
        for (var writtenFile : writtenFiles) {
            rebuildBases.put(SpecsIo.getCanonicalFile(writtenFile), tempFolder);
        }

        currentBases = rebuildBases;
        App rebuiltApp = createApp(writtenFiles, rebuildOptions, extraOptions);

        // Restore current bases
        currentBases = previousBases;

        // rebuiltApp.getTranslationUnits().forEach(tu -> System.out.println("Relative: " + tu.getRelativeFilepath()));

        // Creating an app automatically pushes the App in the Context
        context.popApp();

        // if (update) {
        // // Top app is the one we want, pop the app before that one
        // weaverData.popAst();
        // weaverData.pushAst(rebuiltApp);
        // currentSources = srcFolders;
        //
        // }
        // System.out.println("TUs:"
        // + getApp().getTranslationUnits().stream().map(tu -> tu.getFilename())
        // .collect(Collectors.toList()));
        //
        // System.out.println("TUs Rebuilt:"
        // + rebuiltApp.getTranslationUnits().stream().map(tu -> tu.getFilename())
        // .collect(Collectors.toList()));

        // Base folder is now the temporary folder
        if (update) {
            currentBases = rebuildBases;

            // Discard current app
            weaverData.popAst();

            // Add rebuilt app
            weaverData.pushAst(rebuiltApp);

            // Create file->base map
            // Since files where all written to the same folder:
            // 1) If the parent folder is the same as the temp folder, it has not base folder;
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
                // File baseFolder = writtenFile.getParentFile().equals(tempFolder) ? null : tempFolder;
                // writtenFilesToBase.put(writtenFile, baseFolder);
            }
            // writtenFiles.stream().forEach(
            // file -> file.getParentFile().equals(tempFolder) ? null : writtenFilesToBase.put(file, tempFolder));

            updateSources(writtenFilesToBase);

            // baseFolder = tempFolder;
        }

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

        SpecsIo.deleteFolderContents(tempFolder, true);

        // Register temporary folder and its contents for deletion
        SpecsIo.deleteOnExit(tempFolder);

        return tempFolder.getAbsoluteFile();
    }

    @Override
    public List<Class<?>> getImportableClasses() {
        return CLAVA_IMPORTABLE_CLASSES;

    }

    @Override
    public List<ResourceProvider> getAspectsAPI() {
        return CLAVA_LARA_API;
    }

    @Override
    public List<ResourceProvider> getImportableScripts() {
        return ResourceProvider.getResourcesFromEnum(Arrays.asList(JsApiResource.class, JsAntarexApiResource.class));
    }

    @Override
    public ResourceProvider getIcon() {
        // return () -> "cxxweaver/clava_icon_48x48.ico";
        return () -> "clava/clava_icon_300dpi.png";
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

        // App newApp = getApp().pushAst();
        // weaverData.pushAst(newApp);
        weaverData.pushAst(clonedApp);

        // weaverData.pushAst(clonedApp);
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
        searchPaths.addAll(args.get(CxxWeaverOption.HEADER_INCLUDES).getFiles());
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

    // public static String getRelativeFilepath(TranslationUnit tunit) {
    // return tunit.getRelativeFilepath(CxxWeaver.getCxxWeaver().getBaseSourceFolder());
    // }

    // public static String getRelativeFolderpath(TranslationUnit tunit) {
    // return tunit.getRelativeFolderpath(CxxWeaver.getCxxWeaver().getBaseSourceFolder());
    // }

    @Override
    public boolean executeUnitTestMode(DataStore dataStore) {
        int unitResults = LaraUnitLauncher.execute(dataStore, getClass().getName());

        return unitResults == 0;
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
     * For the temporary folder, the source folders are the children folders of the temporary folder, and the temporary
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
        /*
        boolean flattenFolders = getConfig().get(CxxWeaverOption.FLATTEN_WOVEN_CODE_FOLDER_STRUCTURE);
        
        // For all Translation Units, collect new destination folders
        return getApp().getTranslationUnits().stream()
                // .map(tu -> new File(tu.getDestinationFolder(weavingFolder, flattenFolders),
                // tu.getRelativeFolderpath()))
                // Consider only header files
                .filter(tu -> onlyHeaders ? tu.isHeaderFile() : true)
                .map(tu -> tu.getDestinationFolder(weavingFolder, flattenFolders))
                .map(file -> SpecsIo.getCanonicalFile(file))
                .collect(Collectors.toCollection(() -> new LinkedHashSet<>()));
        */
    }

    private Set<File> getExternalIncludeFolders() {
        return getApp().getExternalDependencies().getExtraIncludes().stream()
                .map(SpecsIo::getCanonicalFile)
                .collect(Collectors.toCollection(() -> new LinkedHashSet<>()));
    }

    private Set<File> getAllIncludeFolders(File weavingFolder, Set<File> generatedFiles) {
        Set<File> includePaths = new LinkedHashSet<>();
        // System.out.println("SOURCE INCLUDE FOLDERS: " + getSourceIncludeFolders(weavingFolder, true));
        // System.out.println("EXTERNAL INCLUDE FOLDERS: " + getExternalIncludeFolders());
        // System.out.println("GENERATED FILES: " + generatedFiles);
        includePaths.addAll(getSourceIncludeFolders(weavingFolder, true));
        includePaths.addAll(getExternalIncludeFolders());

        /*
        if(!generatedFiles.isEmpty()) {
            // Get translation units that correspond to generated files
            getApp().getTranslationUnits();
        }
        */

        // System.out.println("INCLUDES BEFORE:" + includePaths);
        // Add includes to manually generated files
        generatedFiles.stream()
                .filter(SourceType::isHeader)
                .map(File::getParentFile)
                .forEach(includePaths::add);
        // .collect(Collectors.toList());
        // System.out.println("INCLUDES AFTER:" + includePaths);
        return includePaths;
    }

    /*
    
    private Set<String> getIncludePaths(File weavingFolder) {
        Set<String> includePaths = new HashSet<>();
    
        boolean flattenFolders = getConfig().get(CxxWeaverOption.FLATTEN_WOVEN_CODE_FOLDER_STRUCTURE);
        // System.out.println("FLATTEN FOLDERS:" + flattenFolders);
        // for (TranslationUnit tunit : getApp().getTranslationUnits()) {
        // System.out.println("TUNIT: " + tunit.getLocation());
        // System.out.println("DESTINATION FOLDER:" + tunit.getDestinationFolder(weavingFolder, flattenFolders));
        // System.out.println("Relative filepath:" + tunit.getRelativeFilepath());
        // System.out.println("Relative folderpath:" + tunit.getRelativeFolderpath());
        //
        // }
    
        // For all Translation Units, collect new destination folders
        getApp().getTranslationUnits().stream()
                // .map(tu -> tu.getDestinationFolder(weavingFolder, flattenFolders))
                .map(tu -> new File(tu.getDestinationFolder(weavingFolder, flattenFolders),
                        tu.getRelativeFolderpath()))
                .map(file -> SpecsIo.getCanonicalPath(file))
                .forEach(includePaths::add);
    
        // getApp().getTranslationUnits().stream()
        // .map(tu -> SpecsIo.getCanonicalPath(new File(tu.getFolderpath())))
        // .forEach(includePaths::add);
    
        getApp().getExternalDependencies().getExtraIncludes().stream()
                .map(SpecsIo::getCanonicalPath)
                .forEach(includePaths::add);
    
        return includePaths;
    }
    
    */

    /**
     * Creates an empty App.
     *
     * @return
     */
    // private static App createEmptyApp(ClavaContext context) {
    // return context.get(ClavaContext.FACTORY).app(Collections.emptyList());
    // }

    /**
     * Normalizes key paths to be only files
     */
    private Map<File, File> obtainFiles(Map<File, File> filesToBases) {
        var parserOptions = buildParserOptions(args);

        Map<File, File> processedFiles = new HashMap<>();

        for (var sourceAndBase : filesToBases.entrySet()) {
            // If a file, just add it
            if (sourceAndBase.getKey().isFile()) {
                processedFiles.put(sourceAndBase.getKey(), sourceAndBase.getValue());
            }

            // Process folder
            obtainFiles(sourceAndBase.getKey(), sourceAndBase.getValue(), processedFiles, parserOptions);
        }

        return processedFiles;

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
}

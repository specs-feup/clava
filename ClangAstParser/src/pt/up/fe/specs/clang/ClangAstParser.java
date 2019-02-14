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

package pt.up.fe.specs.clang;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.JOptionsUtils;
import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.streamparser.LineStreamParser;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.ast.genericnode.ClangRootNode;
import pt.up.fe.specs.clang.ast.genericnode.ClangRootNode.ClangRootData;
import pt.up.fe.specs.clang.ast.genericnode.GenericClangNode;
import pt.up.fe.specs.clang.astlineparser.AstParser;
import pt.up.fe.specs.clang.datastore.LocalOptionsKeys;
import pt.up.fe.specs.clang.includes.ClangIncludes;
import pt.up.fe.specs.clang.parsers.ClangParserData;
import pt.up.fe.specs.clang.parsers.ClangStreamParserV2;
import pt.up.fe.specs.clang.parsers.ClavaNodes;
import pt.up.fe.specs.clang.streamparser.StreamKeys;
import pt.up.fe.specs.clang.streamparser.StreamParser;
import pt.up.fe.specs.clang.streamparserv2.ClangStreamParser;
import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.SourceRange;
import pt.up.fe.specs.clava.ast.LegacyToDataStore;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.omp.OMPDirective;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.parsing.arguments.ArgumentsParser;
import pt.up.fe.specs.util.providers.FileResourceManager;
import pt.up.fe.specs.util.providers.FileResourceProvider;
import pt.up.fe.specs.util.providers.FileResourceProvider.ResourceWriteData;
import pt.up.fe.specs.util.system.ProcessOutput;
import pt.up.fe.specs.util.utilities.StringLines;

/**
 * Calls Clang and parsers a set of C/C++ files, returning a ClangAst.
 *
 * @author JoaoBispo
 *
 */
public class ClangAstParser {

    private static boolean STRICT_MODE = false;

    private final static String LOCAL_OPTIONS_FILE = "local_options.xml";

    private final static Set<String> TOP_NODES_WITHOUT_TYPES = new HashSet<>(
            Arrays.asList("NamespaceDecl", "AccessSpecDecl", "FriendDecl"));

    private final static String CLANG_DUMP_FILENAME = "clangDump.txt";
    private final static String STDERR_DUMP_FILENAME = "stderr.txt";

    private static final List<String> CLANGAST_TEMP_FILES = Arrays.asList("includes.txt", CLANG_DUMP_FILENAME,
            // "clavaDump.txt", "nodetypes.txt", "types.txt", "is_temporary.txt", "template_args.txt",
            "clavaDump.txt", "nodetypes.txt", "types.txt", "is_temporary.txt",
            "omp.txt", "invalid_source.txt", "enum_integer_type.txt", "consumer_order.txt",
            "types_with_templates.txt");

    private static final String CLANGAST_RESOURCES_FILENAME = "clang_ast.resources";

    private static final String TRANSLATION_UNIT_SET_PREFIX = "COUNTER";

    public static String getTranslationUnitSetPrefix() {
        return TRANSLATION_UNIT_SET_PREFIX;
    }

    public static List<String> getTempFiles() {
        return CLANGAST_TEMP_FILES;
    }

    public static String getClangDumpFilename() {
        return CLANG_DUMP_FILENAME;
    }

    public static boolean isStrictMode() {
        return STRICT_MODE;
    }

    public static void strictMode(boolean value) {
        STRICT_MODE = value;
    }

    public static String getLocalOptionsFile() {
        return LOCAL_OPTIONS_FILE;
    }

    private final boolean dumpStdout;
    private final boolean useCustomResources;
    // private final boolean disableNewParsingMethod;
    private final FileResourceManager clangAstResources;

    public ClangAstParser() {
        this(false, false);

    }

    // public ClangAstParser(boolean dumpStdout, boolean useCustomResources, boolean disableNewParsingMethod) {
    public ClangAstParser(boolean dumpStdout, boolean useCustomResources) {
        this.dumpStdout = dumpStdout;
        this.useCustomResources = useCustomResources;
        // this.disableNewParsingMethod = disableNewParsingMethod;

        clangAstResources = FileResourceManager.fromEnum(ClangAstFileResource.class);

        if (this.useCustomResources) {
            clangAstResources.addLocalResources(CLANGAST_RESOURCES_FILENAME);
        }

    }

    public ClangRootNode parse(Collection<String> files, List<String> options) {

        ClangRootNode output = parse(files, ClangAstKeys.toDataStore(options));

        return output;
    }

    public ClangRootNode parse(Collection<String> files, DataStore config) {
        return parse(files, config, null);
    }

    public ClangRootNode parse(Collection<String> files, DataStore config, Integer id) {

        DataStore localData = JOptionsUtils.loadDataStore(ClangAstParser.LOCAL_OPTIONS_FILE, getClass(),
                LocalOptionsKeys.getProvider().getStoreDefinition());

        // Get version for the executable
        String version = config.get(ClangAstKeys.CLANGAST_VERSION);
        // boolean usePlatformIncludes = config.get(ClangAstKeys.USE_PLATFORM_INCLUDES);
        boolean usePlatformIncludes = false;

        // Copy resources
        File clangExecutable = prepareResources(version);

        List<String> arguments = new ArrayList<>();
        arguments.add(clangExecutable.getAbsolutePath());

        arguments.addAll(files);

        if (id != null) {
            arguments.add("-id=" + id);
        }

        arguments.add("--");

        // Add standard
        config.getTry(ClavaOptions.STANDARD).ifPresent(standard -> arguments.add(standard.getFlag()));
        // arguments.add(config.get(ClavaOptions.STANDARD).getFlag());

        List<String> systemIncludes = new ArrayList<>();

        // Add includes bundled with program
        // (only on Windows, it is expected that a Linux system has its own headers for libc/libc++)
        // if (Platforms.isWindows()) {
        systemIncludes.addAll(prepareIncludes(clangExecutable, usePlatformIncludes));
        // }

        // Add custom includes
        systemIncludes.addAll(localData.get(LocalOptionsKeys.SYSTEM_INCLUDES).getStringList());

        // Add local system includes
        // for (String systemInclude : localData.get(LocalOptionsKeys.SYSTEM_INCLUDES)) {
        for (String systemInclude : systemIncludes) {
            arguments.add("-isystem");
            arguments.add(systemInclude);
        }

        // If there still are arguments left using, pass them after '--'
        arguments.addAll(ArgumentsParser.newCommandLine().parse(config.get(ClavaOptions.FLAGS)));

        SpecsLogs.msgInfo("Calling Clang AST Dumper: " + arguments.stream().collect(Collectors.joining(" ")));

        ClavaContext context = new ClavaContext();

        // Add context to config
        config.add(ClavaNode.CONTEXT, context);

        ClangParserData parsedData = null;

        ProcessOutput<List<ClangNode>, DataStore> output = null;

        // ProcessOutputAsString output = SpecsSystem.runProcess(arguments, true, false);
        try (LineStreamParser<ClangParserData> lineStreamParser = ClangStreamParserV2.newInstance(context)) {
            if (SpecsSystem.isDebug()) {
                lineStreamParser.getData().set(ClangParserData.DEBUG, true);
            }

            // ProcessOutput<List<ClangNode>, DataStore> output = SpecsSystem.runProcess(arguments, this::processOutput,
            output = SpecsSystem.runProcess(arguments, this::processOutput,
                    inputStream -> processStdErr(config, inputStream, lineStreamParser));

            parsedData = lineStreamParser.getData();

            String numLines = SpecsStrings.toDecimal(lineStreamParser.getReadLines());
            String size = SpecsStrings.toBytes(lineStreamParser.getReadChars());
            ClavaLog.metrics("Dumped " + numLines + " lines (~" + size + ")");

            String numNodes = SpecsStrings
                    .toDecimal(lineStreamParser.getData().getClavaNodes().getNodes().size());
            ClavaLog.metrics("Total parsed ClavaNodes: " + numNodes);

            String numCopies = SpecsStrings
                    .toDecimal(lineStreamParser.getData().get(ClangParserData.CONTEXT).get(ClavaContext.METRICS)
                            .getNumCopies());
            ClavaLog.metrics("Extra ClavaNodes created through copies: " + numCopies);

        } catch (Exception e) {
            throw new RuntimeException("Error while running Clang AST dumper", e);
        }

        // Error output has information about types, separate this information from the warnings
        DataStore stderr = output.getStdErr();

        ClangStreamParser clangStreamParser = new ClangStreamParser(parsedData, SpecsSystem.isDebug());
        App newApp = clangStreamParser.parse();

        // Set app in context
        context.pushApp(newApp);
        // context.set(ClavaContext.APP, newApp);

        // if (SpecsSystem.isDebug()) {
        // // App newApp = new ClangStreamParser(stderr).parse();
        // System.out.println("NEW APP CODE:\n" + newApp.getCode());
        // }

        /*
        ClangStreamParser clangStreamParser = new ClangStreamParser(stderr, SpecsSystem.isDebug());
        App newApp = clangStreamParser.parse();
        
        if (SpecsSystem.isDebug()) {
            // App newApp = new ClangStreamParser(stderr).parse();
            System.out.println("NEW APP CODE:\n" + newApp.getCode());
        }
        */
        // System.out.println("NUM DECLS:" + stderr.get(ClangNodeParsing.getNodeDataKey(Decl.class)).size());
        // System.out.println("KEYS:" + stderr.get);
        // DataStore stderr = new StdErrParser().parse(output.getStdErr());

        // Print stderr output
        // if (isDebug()) {
        // IoUtils.write(new File("stderr.txt"), output.getStdErr());
        // }

        String warnings = stderr.get(StreamKeys.WARNINGS);

        // Throw exception if there as any error
        if (output.getReturnValue() != 0) {
            throw new RuntimeException("There where errors during dumping:\n" + warnings);
        }

        // Check if there are error messages
        if (!warnings.isEmpty()) {
            SpecsLogs.msgInfo("There where warnings during dumping:\n" + warnings);
        }

        // Check if there where no interleaved executions
        // checkInterleavedExecutions();

        // Get clang output
        // String clangOutput = output.getStdOut();

        // Always write to a file, to be able to check dump
        // IoUtils.write(new File("clangDump.txt"), clangOutput);
        SpecsIo.write(new File("types.txt"), stderr.get(StreamKeys.TYPES));
        // System.out.println("OUTPUT:\n" + output.getOutput());

        // Parse Clang output
        // List<ClangNode> clangDump = new AstParser().parse(output.getStdOut());
        List<ClangNode> clangDump = output.getStdOut();

        // System.out.println(
        // "TOP LEVEL NODES:" + clangDump.stream().map(ClangNode::getExtendedId).collect(Collectors.toList()));

        // Top-level Clang Nodes
        // Map<String, ClangNode> topLevelClangNodes = clangDump.stream()
        // .collect(Collectors.toMap(ClangNode::getExtendedId, ClangNode::getThis));
        // System.out.println("TOP LEVEL CLANG NODES:" + topLevelClangNodes.keySet());
        // System.out.println("TOP LEVEL CLANG NODES:" + topLevelClangNodes);

        // Add Source Ranges
        addSourceRanges(clangDump, stderr);

        // Parse Clang types
        List<ClangNode> clangTypes = removeDuplicates(new AstParser().parse(stderr.get(StreamKeys.TYPES)));
        addSourceRanges(clangTypes, stderr);

        // Parse node-types map
        // This file includes several repetitions and even addresses that will not appear
        // in the dump. However, all nodes in the dump (clangDump and types) should be
        // represented in the map.
        // Map<String, String> nodeToTypes = parseNodeToTypes(IoUtils.read(new File("nodetypes.txt")));
        Map<String, String> nodeToTypes = parseAddrToAddr(SpecsIo.read(new File("nodetypes.txt")));

        // Remove top-level nodes that do not have a corresponding type
        // clangDump = cleanup(clangDump, nodeToTypes);

        // Make sure all nodes in clangDump have a corresponding type
        // check(clangDump, clangTypes, nodeToTypes);

        // Parse includes
        ClangIncludes includes = ClangIncludes.newInstance(SpecsIo.existingFile("includes.txt"));

        // Get nodes that can have template arguments
        // Set<String> hasTemplateArguments = parseTemplateArguments(SpecsIo.read("template_args.txt"));

        // Get nodes that are temporary
        Set<String> isTemporary = parseIsTemporary(SpecsIo.read("is_temporary.txt"));

        // Get OpenMP directives
        // Map<String, OMPDirective> ompDirectives = parseOmpDirectives(IoUtils.read("omp.txt"));
        Map<String, OMPDirective> ompDirectives = new HashMap<>();

        // Get enum integer types
        Map<String, String> enumToIntegerType = parseEnumIntegerTypes(SpecsIo.read("enum_integer_type.txt"));

        // Check if no new nodes should be used
        // Map<String, ClavaNode> newNodes = disableNewParsingMethod ? new HashMap<>()
        // : lineStreamParser.getData().get(ClangParserKeys.CLAVA_NODES);
        // Map<String, ClavaNode> newNodes = lineStreamParser.getData().get(ClangParserKeys.CLAVA_NODES);
        ClavaNodes newNodes = parsedData.get(ClangParserData.CLAVA_NODES);

        ClangRootData clangRootData = new ClangRootData(config, includes, clangTypes, nodeToTypes,
                isTemporary, ompDirectives, enumToIntegerType, stderr,
                newNodes, clangDump);

        return new ClangRootNode(clangRootData, clangDump);
    }

    public List<ClangNode> processOutput(InputStream inputStream) {
        return processOutput(inputStream, new File(CLANG_DUMP_FILENAME));
    }

    public List<ClangNode> processOutput(InputStream inputStream, File clangDumpFilename) {
        File dumpfile = dumpStdout | isDebug() ? clangDumpFilename : null;

        // Parse Clang output
        List<ClangNode> clangDump = new AstParser(dumpfile).parse(inputStream);

        return clangDump;
    }

    public DataStore processStdErr(DataStore clavaData, InputStream inputStream,
            LineStreamParser<ClangParserData> lineStreamParser) {

        return processStdErr(clavaData, inputStream, lineStreamParser, new File(STDERR_DUMP_FILENAME));
    }

    public DataStore processStdErr(DataStore clavaData, InputStream inputStream,
            LineStreamParser<ClangParserData> lineStreamParser, File stderrDumpFilename) {

        File dumpfile = isDebug() ? stderrDumpFilename : null;

        // TODO: Temporary, needs to be set again, since this will run in a separate thread
        LegacyToDataStore.CLAVA_CONTEXT.set(lineStreamParser.getData().get(ClavaNode.CONTEXT));

        // Parse StdErr from ClangAst
        return new StreamParser(clavaData, dumpfile, lineStreamParser).parse(inputStream);
    }

    public static void addSourceRanges(List<ClangNode> clangDump, DataStore stderr) {

        Map<String, SourceRange> sourceRanges = stderr.get(StreamKeys.SOURCE_RANGES);

        for (ClangNode node : clangDump) {
            node.getDescendantsAndSelfStream().forEach(descendant -> addSourceRange(descendant, sourceRanges));
        }

    }

    private static void addSourceRange(ClangNode node, Map<String, SourceRange> sourceRanges) {
        if (!(node instanceof GenericClangNode)) {
            return;
        }

        GenericClangNode genericClangNode = (GenericClangNode) node;

        if (!node.hasId()) {
            return;
        }

        SourceRange sourceRange = sourceRanges.get(node.getExtendedId());
        sourceRange = sourceRange == null ? SourceRange.invalidRange() : sourceRange;

        genericClangNode.setLocation(sourceRange);

    }

    private static boolean isDebug() {
        return new File("debug").isFile();
    }

    public static void checkInterleavedExecutions() {
        File consumerOrder = new File("consumer_order.txt");
        if (!consumerOrder.isFile()) {
            SpecsLogs.msgInfo("Could not find file 'consumer_order.txt'");
            return;
        }

        List<String> lines = StringLines.getLines(new File("consumer_order.txt"));

        // if (lines.size() % 2 == 0) {
        // LoggingUtils.msgWarn("Expected even number of lines, got '" + lines.size() + "'");
        // return;
        // }
        Preconditions.checkArgument(lines.size() % 2 == 0, "Expected even number of lines, got '" + lines.size() + "'");

        String line1Prefix = "ASTConsumer built ";
        String line2Prefix = "ASTConsumer destroyed ";

        for (int i = 0; i < lines.size(); i += 2) {
            String line1 = lines.get(i);
            String line2 = lines.get(i + 1);

            Preconditions.checkArgument(line1.startsWith(line1Prefix));
            Preconditions.checkArgument(line2.startsWith(line2Prefix));

            String subString1 = line1.substring(line1Prefix.length());
            String subString2 = line2.substring(line2Prefix.length());

            Preconditions.checkArgument(subString1.equals(subString2));
        }

    }

    public static Map<String, String> parseEnumIntegerTypes(String enumIntegerTypes) {
        return parseAddrToAddr(enumIntegerTypes);
        /*
        return StringLines.newInstance(enumIntegerTypes).stream()
        	.map(line -> line.split("->"))
        	.collect(Collectors.toMap(hexes -> Long.decode(hexes[0]), hexes -> Long.decode(hexes[1]),
        		// Keep the last value
        		(value1, value2) -> value2));
        		*/
    }

    public List<String> prepareIncludes(File clangExecutable, boolean usePlatformIncludes) {

        File resourceFolder = getClangResourceFolder();

        File includesBaseFolder = SpecsIo.mkdir(resourceFolder, "clang_includes");
        // ZipResourceManager zipManager = new ZipResourceManager(includesBaseFolder);

        // Create list of include zips
        List<FileResourceProvider> includesZips = new ArrayList<>();
        includesZips.add(clangAstResources.get(ClangAstFileResource.BUILTIN_INCLUDES));

        // Check if built-in libc/c++ needs to be included
        if (useBuiltinLibc(clangExecutable, usePlatformIncludes)) {
            includesZips.add(getLibCResource(SupportedPlatform.getCurrentPlatform()));
        }

        // List<FileResourceProvider> includesZips = Arrays.asList(
        // clangAstResources.get(ClangAstFileResource.BUILTIN_INCLUDES_3_8),
        // getLibCResource(SupportedPlatform.getCurrentPlatform()));

        // Download includes zips, check if any of them is new
        List<ResourceWriteData> zipFiles = includesZips.stream()
                .map(resource -> resource.writeVersioned(resourceFolder, ClangAstParser.class))
                .collect(Collectors.toList());
        // .filter(resourceOutput -> resourceOutput.isNewFile())
        // .findAny()
        // .isPresent();

        // If a new file has been written, delete includes folder, and extract all zips again
        // Extracting all because zips might have several folders and we are not determining which should be updated
        if (zipFiles.stream().filter(ResourceWriteData::isNewFile).findAny().isPresent()) {
            // Clean folder
            SpecsIo.deleteFolderContents(includesBaseFolder);

            // Extract zips
            zipFiles.stream().forEach(zipFile -> SpecsIo.extractZip(zipFile.getFile(), includesBaseFolder));
        }

        // // Clang built-in includes, to be used in all platforms
        // // Write Clang headers
        // ResourceWriteData builtinIncludesZip = clangAstResources.get(ClangAstFileResource.BUILTIN_INCLUDES_3_8)
        // .writeVersioned(resourceFolder, ClangAstParser.class);
        // // ResourceWriteData builtinIncludesZip = ClangAstWebResource.BUILTIN_INCLUDES_3_8.writeVersioned(
        // // resourceFolder, ClangAstParser.class);
        //
        // // boolean hasFolderBeenCleared = false;
        //
        // zipManager.extract(builtinIncludesZip);
        // /*
        // // Unzip file, if new
        // if (builtinIncludesZip.isNewFile()) {
        // // Ensure folder is empty
        // if (!hasFolderBeenCleared) {
        // hasFolderBeenCleared = true;
        // SpecsIo.deleteFolderContents(includesBaseFolder);
        // }
        //
        // SpecsIo.extractZip(builtinIncludesZip.getFile(), includesBaseFolder);
        // // Cannot delete zip file, it will be used to check if there is a new file or not
        // }
        // */
        // // Test if include files are available
        // boolean hasLibC = hasLibC(clangExecutable);
        // // boolean hasLibC = true;
        //
        // if (!hasLibC) {
        // // Obtain correct version of libc/c++
        // FileResourceProvider libcResource = getLibCResource(SupportedPlatform.getCurrentPlatform());
        //
        // if (libcResource == null) {
        // ClavaLog.info("Could not detect LibC/C++, and currently there is no bundled alternative for platform '"
        // + SupportedPlatform.getCurrentPlatform() + "'. System includes might not work.");
        // } else {
        //
        // // Write Clang headers
        // ResourceWriteData libcZip = libcResource.writeVersioned(resourceFolder,
        // ClangAstParser.class);
        //
        // zipManager.extract(libcZip);
        //
        // /*
        // // Unzip file, if new
        // if (libcZip.isNewFile()) {
        // // Ensure folder is empty
        // if (!hasFolderBeenCleared) {
        // hasFolderBeenCleared = true;
        // // Ensure folder is empty
        // SpecsIo.deleteFolderContents(includesBaseFolder);
        // }
        //
        // SpecsIo.extractZip(libcZip.getFile(), includesBaseFolder);
        //
        // // Cannot delete zip file, it will be used to check if there is a new file or not
        // }
        //
        // */
        // }
        //
        // }

        // List<String> includes = new ArrayList<>();

        // File includesFolder = IoUtils.safeFolder(resourceFolder, "includes");
        // File libcFolder = IoUtils.safeFolder(resourceFolder, "include_libc");
        // File libcFolder = IoUtils.safeFolder(resourceFolder, "libc_visualstudio");
        // File libcFolder = IoUtils.safeFolder(resourceFolder, "libc");
        // File libcExtraFolder = IoUtils.safeFolder(resourceFolder, "libc_extra");
        // File libcxxFolder = IoUtils.safeFolder(resourceFolder, "include_libc++");

        // includes.add(libcFolder.getAbsolutePath());
        // includes.add(libcExtraFolder.getAbsolutePath());
        // includes.add(libcxxFolder.getAbsolutePath());

        // Add all folders inside base folder as system include
        List<String> includes = SpecsIo.getFolders(includesBaseFolder).stream()
                .map(file -> file.getAbsolutePath())
                .collect(Collectors.toList());

        // Sort them alphabetically, include order can be important
        Collections.sort(includes);

        // If on linux, make folders and files accessible to all users
        if (SupportedPlatform.getCurrentPlatform().isLinux()) {
            SpecsSystem.runProcess(Arrays.asList("chmod", "-R", "777", resourceFolder.getAbsolutePath()), false, true);
        }

        return includes;
    }

    private boolean useBuiltinLibc(File clangExecutable, boolean usePlatformIncludes) {

        // If platform includes is disable, always use built-in headers
        if (!usePlatformIncludes) {
            return true;
        }

        // If headers of both libc and libc++ are available, do not use built-in libc
        return !hasLibC(clangExecutable);
    }

    /**
     * Detects if the system has libc/licxx installed.
     * 
     * @param clangExecutable
     * @return
     */
    private boolean hasLibC(File clangExecutable) {
        // return false;

        // If Windows, return false and always use bundled LIBC++
        // if (SupportedPlatform.getCurrentPlatform().isWindows()) {
        // return false;
        // }

        File clangTest = SpecsIo.mkdir(SpecsIo.getTempFolder(), "clang_ast_test");

        // Write test files
        List<File> testFiles = Arrays.asList(ClangAstResource.TEST_INCLUDES_C, ClangAstResource.TEST_INCLUDES_CPP)
                .stream()
                .map(resource -> resource.write(clangTest))
                .collect(Collectors.toList());

        // If on linux, make folders and files accessible to all users
        if (SupportedPlatform.getCurrentPlatform().isLinux()) {
            SpecsSystem.runProcess(Arrays.asList("chmod", "-R", "777", clangTest.getAbsolutePath()), false, true);
        }

        // boolean needsLib = Arrays.asList(ClangAstResource.TEST_INCLUDES_C, ClangAstResource.TEST_INCLUDES_CPP)
        /*
        boolean needsLib = testFiles.parallelStream()
                .map(testFile -> testFile(clangExecutable, clangTest, testFile))
                // Check if test fails in any of cases
                .filter(hasInclude -> !hasInclude)
                .findAny()
                .isPresent();
        */
        boolean needsLib = false;
        for (File testFile : testFiles) {
            ProcessOutput<List<ClangNode>, DataStore> output = testFile(clangExecutable, clangTest, testFile);
            // ClavaLog.debug(message);
            // System.out.println("RETURN VALUE:" + output.getReturnValue());
            // System.out.println("STD OUT:" + output.getStdOut());
            // System.out.println("STD ERR:" + output.getStdErr().get(StreamKeys.WARNINGS));

            // boolean foundInclude = !output.getStdOut().isEmpty();
            boolean foundInclude = output.getReturnValue() == 0;

            if (foundInclude) {
                SpecsCheck.checkArgument(output.getStdOut().isEmpty(),
                        () -> "Expected std output to be empty: " + output.getStdOut());
                SpecsCheck.checkArgument(output.getStdErr().get(StreamKeys.WARNINGS).isEmpty(),
                        () -> "Expected err output to be empty: " + output.getStdErr().get(StreamKeys.WARNINGS));
            }

            if (!foundInclude) {
                needsLib = true;
                break;
            }
            // return foundInclude;
        }

        if (needsLib) {
            ClavaLog.debug("Could not find system libc/licxx");
        } else {
            ClavaLog.debug("Detected system's libc and licxx");
        }

        return !needsLib;

    }

    // private boolean testFile(File clangExecutable, File testFolder, ResourceProvider testResource) {
    private ProcessOutput<List<ClangNode>, DataStore> testFile(File clangExecutable, File testFolder, File testFile) {
        // File testFile = testResource.write(testFolder);

        List<String> arguments = Arrays.asList(clangExecutable.getAbsolutePath(), testFile.getAbsolutePath(), "--");
        ClavaContext context = new ClavaContext();

        try (LineStreamParser<ClangParserData> clangStreamParser = ClangStreamParserV2.newInstance(context)) {

            ProcessOutput<List<ClangNode>, DataStore> output = SpecsSystem.runProcess(arguments, this::processOutput,
                    inputStream -> processStdErr(DataStore.newInstance("testFile DataStore"), inputStream,
                            clangStreamParser));

            return output;
            // boolean foundInclude = !output.getStdOut().isEmpty();
            //
            // return foundInclude;

        } catch (Exception e) {
            throw new RuntimeException("Error while testing include", e);
        }
    }

    public static Set<String> parseIsTemporary(String isTemporary) {
        return parseAddrSet(isTemporary);
        // return StringLines.getLines(isTemporary).stream()
        // .map(line -> Long.decode(line))
        // .collect(Collectors.toSet());
    }

    /*
    private static Set<String> parseTemplateArguments(String templateArgs) {
        return parseAddrSet(templateArgs);
        // return StringLines.getLines(templateArgs).stream()
        // .map(line -> Long.decode(line))
        // .collect(Collectors.toSet());
    }
    */

    /*
    private static Map<String, List<Qualifier>> parseTypeQualifiers(String typeQualifiersString) {
        Map<String, List<Qualifier>> typeQualifiers = new HashMap<>();
    
        for (String line : StringLines.getLines(typeQualifiersString)) {
    
            String[] parts = line.split("->");
            Preconditions.checkArgument(parts.length == 2);
            String nodeAddress = parts[0].trim();
            String qualifiersStr = parts[1].trim();
    
            List<Qualifier> qualifiers = new StringParser(qualifiersStr).apply(ClangDataParsers::parseQualifiers);
    
            typeQualifiers.put(nodeAddress, qualifiers);
        }
    
        return typeQualifiers;
    }
    */
    /*
    private static List<ClangNode> cleanup(List<ClangNode> clangDump, Map<Long, Long> nodeToTypes) {
        List<ClangNode> cleanedDump = new ArrayList<>();
    
        Set<String> removedTypes = new HashSet<>();
    
        for (ClangNode node : clangDump) {
            // Check if node has a type
            if (TOP_NODES_WITHOUT_TYPES.contains(node.getName())) {
                cleanedDump.add(node);
                continue;
            }
    
            // If node is not in nodeToTypes, ignore it
            Long typeId = nodeToTypes.get(node.getId());
    
            if (typeId == null) {
                removedTypes.add(node.getName());
                // cleanedDump.add(node);
    
                // LoggingUtils.msgInfo("Could not find type for node of type '" + node.getName() + "' - 0x"
                // + Long.toHexString(node.getId()));
                continue;
            }
            System.out.println("ADDING:" + node.getName());
            cleanedDump.add(node);
        }
    
        LoggingUtils.msgInfo("Cleaned nodes of types: " + removedTypes);
    
        return cleanedDump;
    }
    */
    /**
     * Checks if all nodes in clangDump appear in nodeToTypes, and that the respective mapping exists in clangTypes.
     *
     * <p>
     * If a node in ClangDump does not appear in the nodeToTypes, it is dropped.
     *
     * @param clangDump
     * @param clangTypes
     * @param nodeToTypes
     */
    public static List<ClangNode> check(List<ClangNode> clangDump, List<ClangNode> clangTypes,
            Map<String, String> nodeToTypes) {

        // Check if there is any top level node where all the types of its nodes are defined

        List<ClangNode> cleanedDump = new ArrayList<>();

        List<ClangNode> allNodes = clangDump.stream()
                .flatMap(topLevelNode -> topLevelNode.getDescendantsAndSelfStream())
                .collect(Collectors.toList());

        for (ClangNode node : allNodes) {
            // Ignore nodes without id
            if (!node.hasId()) {
                cleanedDump.add(node);
                continue;
            }

            // Statements do not have a type
            if (node.getName().endsWith("Stmt")) {
                cleanedDump.add(node);
                continue;
            }

            // Check if node has a type
            if (TOP_NODES_WITHOUT_TYPES.contains(node.getName())) {
                cleanedDump.add(node);
                continue;
            }

            // If node is not in nodeToTypes, ignore it
            // Long typeId = nodeToTypes.get(node.getId());
            String typeId = nodeToTypes.get(node.getInfo().getExtendedId());

            if (typeId == null) {
                // cleanedDump.add(node);

                SpecsLogs.msgInfo("Could not find type for node of type '" + node.getName() + "' - "
                        + node.getExtendedId());
                continue;
            }

            cleanedDump.add(node);
        }

        return cleanedDump;
    }

    // private static List<ClangNode> parseClangTypes(String typesString) {
    // List<ClangNode> types = new AstParser().parse(typesString);
    //
    // return removeDuplicates(types);
    // }

    public static List<ClangNode> removeDuplicates(List<ClangNode> nodes) {

        // Types will not be part of the tree, multiple nodes can refer to them
        // Duplicates can be removed safely

        Set<String> addedNodes = new HashSet<>();
        List<ClangNode> uniqueNodes = new ArrayList<>();
        for (ClangNode node : nodes) {

            // Ignore null nodes
            if (node.getName().equals("NULL")) {
                continue;
            }

            String id = node.getExtendedId();

            // if (addedNodes.contains(node.getAddressTry().get())) {
            if (addedNodes.contains(id)) {
                continue;
            }

            // addedNodes.add(node.getAddressTry().get());
            addedNodes.add(id);
            uniqueNodes.add(node);
        }

        // System.out.println("ORIGINAL TYPES: " + nodes.size());
        // System.out.println("UNIQUE TYPES:" + uniqueNodes.size());
        // System.out.println("REMOVED:" + (nodes.size() - uniqueNodes.size()));

        return uniqueNodes;
    }

    public static Map<String, String> parseAddrToAddr(String contents) {

        Map<String, String> addrRelations = new HashMap<>();
        for (String line : StringLines.newInstance(contents)) {

            String[] values = line.split("->");
            String sourceAddr = values[0];
            String destAddr = values[1];

            // If type address is 0, ignore
            if (destAddr.startsWith("0_")) {
                continue;
            }

            String previousType = addrRelations.put(sourceAddr, destAddr);

            if (previousType != null) {

                if (previousType.equals(destAddr)) {
                    SpecsLogs.msgInfo("Duplicated entry, check");
                    continue;
                }

                if (addrRelations.containsKey(sourceAddr)) {
                    System.out.println("CHOOSE: " + sourceAddr + " (prev:"
                            + previousType + ", new:"
                            + destAddr + ")");
                }
            }

        }

        return addrRelations;
    }

    private static Set<String> parseAddrSet(String contents) {

        Set<String> addrSet = new HashSet<>();
        for (String line : StringLines.newInstance(contents)) {
            addrSet.add(line.trim());
        }

        return addrSet;

    }

    /**
     *
     * @return path to the executable that was copied
     */
    public File prepareResources(String version) {

        File resourceFolder = getClangResourceFolder();

        SupportedPlatform platform = SupportedPlatform.getCurrentPlatform();
        FileResourceProvider executableResource = getExecutableResource(platform);

        // If version not defined, use the latest version of the resource
        if (version.isEmpty()) {
            version = executableResource.getVersion();
        }

        // ClangAst executable versions are separated by an underscore
        executableResource = executableResource.createResourceVersion("_" + version);

        // Copy executable
        ResourceWriteData executable = executableResource.writeVersioned(resourceFolder, ClangAstParser.class);

        // If Windows, copy additional dependencies
        if (platform == SupportedPlatform.WINDOWS) {
            for (FileResourceProvider resource : getWindowsResources()) {
                resource.writeVersioned(resourceFolder, ClangAstParser.class);
            }
        }

        // If file is new and we are in a flavor of Linux, make file executable
        if (executable.isNewFile() && platform.isLinux()) {
            SpecsSystem.runProcess(Arrays.asList("chmod", "+x", executable.getFile().getAbsolutePath()), false, true);
        }

        // If on linux, make folders and files accessible to all users
        if (platform.isLinux()) {
            SpecsSystem.runProcess(Arrays.asList("chmod", "-R", "777", resourceFolder.getAbsolutePath()), false, true);
        }

        return executable.getFile();
    }

    private FileResourceProvider getExecutableResource(SupportedPlatform platform) {

        switch (platform) {
        case WINDOWS:
            return clangAstResources.get(ClangAstFileResource.WIN_EXE);
        case CENTOS:
            return clangAstResources.get(ClangAstFileResource.CENTOS_EXE);
        case LINUX:
            return clangAstResources.get(ClangAstFileResource.LINUX_EXE);
        case MAC_OS:
            return clangAstResources.get(ClangAstFileResource.MAC_OS_EXE);
        default:
            throw new RuntimeException("Case not defined: '" + platform + "'");
        }
    }

    private FileResourceProvider getLibCResource(SupportedPlatform platform) {
        return clangAstResources.get(ClangAstFileResource.LIBC_CXX);
        /*        
        switch (platform) {
        case WINDOWS:
            return clangAstResources.get(ClangAstFileResource.LIBC_CXX_WINDOWS);
        // return ClangAstWebResource.LIBC_CXX_WINDOWS;
        case MAC_OS:
            return clangAstResources.get(ClangAstFileResource.LIBC_CXX_MAC_OS);
        case CENTOS6:
            return clangAstResources.get(ClangAstFileResource.LIBC_CXX_CENTOS6);
        case LINUX:
            return clangAstResources.get(ClangAstFileResource.LIBC_CXX_LINUX);
        
        // return ClangAstWebResource.LIBC_CXX_MAC_OS;
        // case CENTOS6:
        // return clangAstResources.get(ClangAstFileResource.LIBC_CXX_WINDOWS);
        // return ClangAstWebResource.LIBC_CXX_MAC_OS;
        default:
            return null;
        // throw new RuntimeException("LibC/C++ not available for platform '" + platform + "'");
        }
        */
    }

    public static File getClangResourceFolder() {
        String tempDir = System.getProperty("java.io.tmpdir");
        // String baseFilename = new JarPath(ClangAstLauncher.class, "clangjar").buildJarPath();
        // File resourceFolder = new File(baseFilename, "clang_ast_exe");
        File resourceFolder = new File(tempDir, "clang_ast_exe");
        return resourceFolder;
    }

    private List<FileResourceProvider> getWindowsResources() {
        List<FileResourceProvider> windowsResources = new ArrayList<>();

        windowsResources.add(clangAstResources.get(ClangAstFileResource.WIN_DLL1));
        windowsResources.add(clangAstResources.get(ClangAstFileResource.WIN_DLL2));
        windowsResources.add(clangAstResources.get(ClangAstFileResource.WIN_DLL3));

        return windowsResources;
        // clangAstResources.get(resourceEnum)
        //
        // return Arrays.asList(WIN_DLL1, WIN_DLL2, WIN_DLL3);
    }
}

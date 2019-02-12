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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.JOptionsUtils;
import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.streamparser.LineStreamParser;

import pt.up.fe.specs.clang.ClangAstFileResource;
import pt.up.fe.specs.clang.ClangAstKeys;
import pt.up.fe.specs.clang.ClangAstParser;
import pt.up.fe.specs.clang.SupportedPlatform;
import pt.up.fe.specs.clang.datastore.LocalOptionsKeys;
import pt.up.fe.specs.clang.parsers.ClangParserData;
import pt.up.fe.specs.clang.parsers.ClangStreamParserV2;
import pt.up.fe.specs.clang.textparser.TextParser;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.parsing.arguments.ArgumentsParser;
import pt.up.fe.specs.util.providers.FileResourceManager;
import pt.up.fe.specs.util.providers.FileResourceProvider;
import pt.up.fe.specs.util.providers.FileResourceProvider.ResourceWriteData;
import pt.up.fe.specs.util.providers.ResourceProvider;
import pt.up.fe.specs.util.system.ProcessOutput;
import pt.up.fe.specs.util.utilities.LineStream;

public class ClangDumperParser {

    // private final static String CLANG_DUMP_FILENAME = "clangDump.txt";
    private final static String STDERR_DUMP_FILENAME = "stderr.txt";
    private static final String CLANGAST_RESOURCES_FILENAME = "clang_ast.resources";
    private final static String LOCAL_OPTIONS_FILE = "local_options.xml";

    // private final boolean dumpStdout;
    private final boolean useCustomResources;
    private final FileResourceManager clangAstResources;

    public ClangDumperParser() {
        this(false, false);

    }

    public ClangDumperParser(boolean dumpStdout, boolean useCustomResources) {
        // this.dumpStdout = dumpStdout;
        this.useCustomResources = useCustomResources;

        clangAstResources = FileResourceManager.fromEnum(ClangAstFileResource.class);

        if (this.useCustomResources) {
            clangAstResources.addLocalResources(CLANGAST_RESOURCES_FILENAME);
        }

    }

    public App parse(Collection<String> files, List<String> options) {

        return parse(files, ClangAstKeys.toDataStore(options));
    }

    public App parse(Collection<String> files, DataStore config) {

        ClavaContext context = new ClavaContext();

        DataStore localData = JOptionsUtils.loadDataStore(LOCAL_OPTIONS_FILE, getClass(),
                LocalOptionsKeys.getProvider().getStoreDefinition());

        // Get version for the executable
        String version = config.get(ClangAstKeys.CLANGAST_VERSION);

        // Copy resources
        File clangExecutable = prepareResources(version);

        List<String> arguments = new ArrayList<>();
        arguments.add(clangExecutable.getAbsolutePath());

        arguments.addAll(files);

        arguments.add("--");

        // Add standard if present
        if (config.hasValue(ClavaOptions.STANDARD)) {
            arguments.add(config.get(ClavaOptions.STANDARD).getFlag());
        }

        List<String> systemIncludes = new ArrayList<>();

        // Add includes bundled with program
        // (only on Windows, it is expected that a Linux system has its own headers for libc/libc++)
        // if (Platforms.isWindows()) {
        systemIncludes.addAll(prepareIncludes(clangExecutable));
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

        // ProcessOutputAsString output = SpecsSystem.runProcess(arguments, true, false);
        // LineStreamParserV2 lineStreamParser = ClangStreamParserV2.newInstance();
        // if (SpecsSystem.isDebug()) {
        // lineStreamParser.getData().set(ClangParserKeys.DEBUG, true);
        // }

        ProcessOutput<String, ClangParserData> output = SpecsSystem.runProcess(arguments, this::processOutput,
                inputStream -> processStdErr(inputStream, context));

        String warnings = output.getStdErr().get(ClangParserData.LINES_NOT_PARSED);

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

        ClangStreamParser clangStreamParser = new ClangStreamParser(output.getStdErr(), SpecsSystem.isDebug());
        App app = clangStreamParser.parse();

        // Set app in context
        // context.set(ClavaContext.APP, app);
        context.pushApp(app);

        // Add text elements (comments, pragmas) to the tree
        new TextParser(app.getContext()).addElements(app);

        return app;
    }

    private ClangParserData processStdErr(InputStream inputStream, ClavaContext context) {
        // Create LineStreamParser
        try (LineStreamParser<ClangParserData> lineStreamParser = ClangStreamParserV2.newInstance(context)) {

            // Set debug
            if (SpecsSystem.isDebug()) {
                lineStreamParser.getData().set(ClangParserData.DEBUG, true);
            }

            // Dump file
            File dumpfile = SpecsSystem.isDebug() ? new File(STDERR_DUMP_FILENAME) : null;

            // Parse input stream
            String linesNotParsed = lineStreamParser.parse(inputStream, dumpfile);

            // Add lines not parsed to DataStore
            ClangParserData data = lineStreamParser.getData();
            data.set(ClangParserData.LINES_NOT_PARSED, linesNotParsed);

            // Return data
            return data;
        } catch (Exception e) {
            throw new RuntimeException("Error while parsing output of Clang AST dumper", e);
        }

    }

    private String processOutput(InputStream inputStream) {
        StringBuilder output = new StringBuilder();
        try (LineStream lines = LineStream.newInstance(inputStream, null)) {
            while (lines.hasNextLine()) {
                output.append(lines.nextLine()).append("\n");
            }
        }

        return output.toString();
    }

    /**
     *
     * @return path to the executable that was copied
     */
    private File prepareResources(String version) {

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

        return executable.getFile();

    }

    public static File getClangResourceFolder() {
        String tempDir = System.getProperty("java.io.tmpdir");
        // String baseFilename = new JarPath(ClangAstLauncher.class, "clangjar").buildJarPath();
        // File resourceFolder = new File(baseFilename, "clang_ast_exe");
        File resourceFolder = new File(tempDir, "clang_ast_exe");
        return resourceFolder;
    }

    private FileResourceProvider getExecutableResource(SupportedPlatform platform) {

        switch (platform) {
        case WINDOWS:
            return clangAstResources.get(ClangAstFileResource.WIN_EXE);
        case CENTOS6:
            return clangAstResources.get(ClangAstFileResource.CENTOS6_EXE);
        case LINUX:
            return clangAstResources.get(ClangAstFileResource.LINUX_EXE);
        case MAC_OS:
            return clangAstResources.get(ClangAstFileResource.MAC_OS_EXE);
        default:
            throw new RuntimeException("Case not defined: '" + platform + "'");
        }
    }

    private List<FileResourceProvider> getWindowsResources() {
        List<FileResourceProvider> windowsResources = new ArrayList<>();

        windowsResources.add(clangAstResources.get(ClangAstFileResource.WIN_DLL1));
        windowsResources.add(clangAstResources.get(ClangAstFileResource.WIN_DLL2));
        windowsResources.add(clangAstResources.get(ClangAstFileResource.WIN_DLL3));

        return windowsResources;
    }

    private List<String> prepareIncludes(File clangExecutable) {

        File resourceFolder = getClangResourceFolder();

        File includesBaseFolder = SpecsIo.mkdir(resourceFolder, "clang_includes");
        // ZipResourceManager zipManager = new ZipResourceManager(includesBaseFolder);

        // Download includes zips, check if any of them is new
        List<FileResourceProvider> includesZips = Arrays.asList(
                clangAstResources.get(ClangAstFileResource.BUILTIN_INCLUDES),
                getLibCResource(SupportedPlatform.getCurrentPlatform()));

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

        /*
        // Clang built-in includes, to be used in all platforms
        // Write Clang headers
        ResourceWriteData builtinIncludesZip = clangAstResources.get(ClangAstFileResource.BUILTIN_INCLUDES_3_8)
                .writeVersioned(resourceFolder, ClangAstParser.class);
        // ResourceWriteData builtinIncludesZip = ClangAstWebResource.BUILTIN_INCLUDES_3_8.writeVersioned(
        // resourceFolder, ClangAstParser.class);
        
        // boolean hasFolderBeenCleared = false;
        
        zipManager.extract(builtinIncludesZip);
        
        // Test if include files are available
        boolean hasLibC = hasLibC(clangExecutable);
        // boolean hasLibC = true;
        
        if (!hasLibC) {
            // Obtain correct version of libc/c++
            FileResourceProvider libcResource = getLibCResource(SupportedPlatform.getCurrentPlatform());
        
            if (libcResource == null) {
                ClavaLog.info("Could not detect LibC/C++, and currently there is no bundled alternative for platform '"
                        + SupportedPlatform.getCurrentPlatform() + "'. System includes might not work.");
            } else {
        
                // Write Clang headers
                ResourceWriteData libcZip = libcResource.writeVersioned(resourceFolder,
                        ClangAstParser.class);
        
                zipManager.extract(libcZip);
            }
        }
        */

        // Add all folders inside base folder as system include
        List<String> includes = SpecsIo.getFolders(includesBaseFolder).stream()
                .map(file -> file.getAbsolutePath())
                .collect(Collectors.toList());

        // Sort them alphabetically, include order can be important
        Collections.sort(includes);

        return includes;
    }

    /**
     * Detects if the system has libc/licxx installed.
     * 
     * @param clangExecutable
     * @return
     */
    private boolean hasLibC(File clangExecutable) {

        return false;
        /*
        // If Windows, return false and always use bundled LIBC++
        if (SupportedPlatform.getCurrentPlatform().isWindows()) {
            return false;
        }
        
        File clangTest = SpecsIo.mkdir(SpecsIo.getTempFolder(), "clang_ast_test");
        
        // Test C
        ProcessOutput<String, ClangParserData> outputC = testFile(clangExecutable, clangTest,
                ClangAstResource.TEST_INCLUDES_C);
        boolean foundCIncludes = !outputC.getStdOut().isEmpty();
        
        if (!foundCIncludes) {
            ClavaLog.debug("Could not find C includes, output of test: " + outputC.getStdOut() + "\n----\n"
                    + outputC.getStdErr());
        }
        
        // Test C++
        ProcessOutput<String, ClangParserData> outputCpp = testFile(clangExecutable, clangTest,
                ClangAstResource.TEST_INCLUDES_CPP);
        boolean foundCppIncludes = !outputCpp.getStdOut().isEmpty();
        
        if (!foundCppIncludes) {
            ClavaLog.debug("Could not find C++ includes, output of test: " + outputCpp.getStdOut() + "\n----\n"
                    + outputCpp.getStdErr());
        }
        
        boolean hasLibs = foundCIncludes && foundCppIncludes;
        
        return hasLibs;
        
        // boolean needsLib = !foundCIncludes || !foundCppIncludes;
        //
        // return !needsLib;
        
        // boolean needsLib = Arrays.asList(ClangAstResource.TEST_INCLUDES_C, ClangAstResource.TEST_INCLUDES_CPP)
        // .parallelStream()
        // .map(resource -> testFile(clangExecutable, clangTest, resource))
        // // Check if test fails in any of cases
        // .filter(hasInclude -> !hasInclude)
        // .findAny()
        // .isPresent();
        //
        // if (needsLib) {
        // SpecsLogs.msgLib("Could not find libc/licxx installed in the system");
        // } else {
        // SpecsLogs.msgLib("Detected libc and licxx installed in the system");
        // }
        
        // return !needsLib;
         */
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
        default:
            return null;
        // throw new RuntimeException("LibC/C++ not available for platform '" + platform + "'");
        }
        */
    }

    // private boolean testFile(File clangExecutable, File testFolder, ResourceProvider testResource) {
    private ProcessOutput<String, ClangParserData> testFile(File clangExecutable, File testFolder,
            ResourceProvider testResource) {
        File testFile = testResource.write(testFolder);

        List<String> arguments = Arrays.asList(clangExecutable.getAbsolutePath(), testFile.getAbsolutePath(), "--");
        ClavaContext context = new ClavaContext();

        // LineStreamParserV2 clangStreamParser = ClangStreamParserV2.newInstance();
        ProcessOutput<String, ClangParserData> output = SpecsSystem.runProcess(arguments,
                this::processOutput, inputStream -> processStdErr(inputStream, context));

        return output;
        // boolean foundInclude = !output.getStdOut().isEmpty();

        // return foundInclude;
    }

    // private static void checkInterleavedExecutions() {
    // File consumerOrder = new File("consumer_order.txt");
    // if (!consumerOrder.isFile()) {
    // SpecsLogs.msgInfo("Could not find file 'consumer_order.txt'");
    // return;
    // }
    //
    // List<String> lines = StringLines.getLines(new File("consumer_order.txt"));
    //
    // // if (lines.size() % 2 == 0) {
    // // LoggingUtils.msgWarn("Expected even number of lines, got '" + lines.size() + "'");
    // // return;
    // // }
    // Preconditions.checkArgument(lines.size() % 2 == 0, "Expected even number of lines, got '" + lines.size() + "'");
    //
    // String line1Prefix = "ASTConsumer built ";
    // String line2Prefix = "ASTConsumer destroyed ";
    //
    // for (int i = 0; i < lines.size(); i += 2) {
    // String line1 = lines.get(i);
    // String line2 = lines.get(i + 1);
    //
    // Preconditions.checkArgument(line1.startsWith(line1Prefix));
    // Preconditions.checkArgument(line2.startsWith(line2Prefix));
    //
    // String subString1 = line1.substring(line1Prefix.length());
    // String subString2 = line2.substring(line2Prefix.length());
    //
    // Preconditions.checkArgument(subString1.equals(subString2));
    // }
    //
    // }

}

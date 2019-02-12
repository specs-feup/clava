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

package pt.up.fe.specs.clang;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.streamparser.LineStreamParser;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.astlineparser.AstParser;
import pt.up.fe.specs.clang.parsers.ClangParserData;
import pt.up.fe.specs.clang.parsers.ClangStreamParserV2;
import pt.up.fe.specs.clang.streamparser.StreamKeys;
import pt.up.fe.specs.clang.streamparser.StreamParser;
import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.LegacyToDataStore;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.providers.FileResourceManager;
import pt.up.fe.specs.util.providers.FileResourceProvider;
import pt.up.fe.specs.util.providers.FileResourceProvider.ResourceWriteData;
import pt.up.fe.specs.util.system.ProcessOutput;

public class ClangResources {

    private static final FileResourceManager CLANG_AST_RESOURCES = FileResourceManager
            .fromEnum(ClangAstFileResource.class);

    private final static String CLANG_DUMP_FILENAME = "clangDump.txt";
    private final static String STDERR_DUMP_FILENAME = "stderr.txt";

    private final boolean dumpStdout;
    private final FileResourceManager clangAstResources;

    public ClangResources(boolean dumpStdout) {
        this.dumpStdout = dumpStdout;
        clangAstResources = FileResourceManager.fromEnum(ClangAstFileResource.class);
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

    public File getClangResourceFolder() {
        String tempDir = System.getProperty("java.io.tmpdir");
        // String baseFilename = new JarPath(ClangAstLauncher.class, "clangjar").buildJarPath();
        // File resourceFolder = new File(baseFilename, "clang_ast_exe");
        File resourceFolder = new File(tempDir, "clang_ast_exe");
        return resourceFolder;
    }

    private FileResourceProvider getExecutableResource(SupportedPlatform platform) {

        switch (platform) {
        case WINDOWS:
            return CLANG_AST_RESOURCES.get(ClangAstFileResource.WIN_EXE);
        case CENTOS6:
            return CLANG_AST_RESOURCES.get(ClangAstFileResource.CENTOS6_EXE);
        case LINUX:
            return CLANG_AST_RESOURCES.get(ClangAstFileResource.LINUX_EXE);
        case MAC_OS:
            return CLANG_AST_RESOURCES.get(ClangAstFileResource.MAC_OS_EXE);
        default:
            throw new RuntimeException("Case not defined: '" + platform + "'");
        }
    }

    private List<FileResourceProvider> getWindowsResources() {
        List<FileResourceProvider> windowsResources = new ArrayList<>();

        windowsResources.add(CLANG_AST_RESOURCES.get(ClangAstFileResource.WIN_DLL1));
        windowsResources.add(CLANG_AST_RESOURCES.get(ClangAstFileResource.WIN_DLL2));
        windowsResources.add(CLANG_AST_RESOURCES.get(ClangAstFileResource.WIN_DLL3));

        return windowsResources;
        // clangAstResources.get(resourceEnum)
        //
        // return Arrays.asList(WIN_DLL1, WIN_DLL2, WIN_DLL3);
    }

    public List<String> prepareIncludes(File clangExecutable, boolean usePlatformIncludes) {

        if (usePlatformIncludes) {
            return Collections.emptyList();
        }

        File resourceFolder = getClangResourceFolder();

        File includesBaseFolder = SpecsIo.mkdir(resourceFolder, "clang_includes");
        // ZipResourceManager zipManager = new ZipResourceManager(includesBaseFolder);

        // Create list of include zips
        List<FileResourceProvider> includesZips = new ArrayList<>();
        includesZips.add(CLANG_AST_RESOURCES.get(ClangAstFileResource.BUILTIN_INCLUDES));

        // Check if built-in libc/c++ needs to be included
        if (useBuiltinLibc(clangExecutable, usePlatformIncludes)) {
            includesZips.add(getLibCResource(SupportedPlatform.getCurrentPlatform()));
        }

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
        return !usePlatformIncludes;
        /*
        // If platform includes is disabled, always use built-in headers
        if (!usePlatformIncludes) {
            return true;
        }
        
        // If headers of both libc and libc++ are available, do not use built-in libc
        return !hasLibC(clangExecutable);
        */
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
    private ProcessOutput<List<ClangNode>, DataStore> testFile(File clangExecutable, File testFolder,
            File testFile) {
        // File testFile = testResource.write(testFolder);

        List<String> arguments = Arrays.asList(clangExecutable.getAbsolutePath(), testFile.getAbsolutePath(), "--");
        ClavaContext context = new ClavaContext();

        try (LineStreamParser<ClangParserData> clangStreamParser = ClangStreamParserV2.newInstance(context)) {

            ProcessOutput<List<ClangNode>, DataStore> output = SpecsSystem.runProcess(arguments,
                    this::processOutput,
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

    public List<ClangNode> processOutput(InputStream inputStream) {
        return processOutput(inputStream, new File(CLANG_DUMP_FILENAME));
    }

    public List<ClangNode> processOutput(InputStream inputStream, File clangDumpFilename) {
        File dumpfile = dumpStdout | SpecsSystem.isDebug() ? clangDumpFilename : null;

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

        File dumpfile = SpecsSystem.isDebug() ? stderrDumpFilename : null;

        // TODO: Temporary, needs to be set again, since this will run in a separate thread
        LegacyToDataStore.CLAVA_CONTEXT.set(lineStreamParser.getData().get(ClavaNode.CONTEXT));

        // Parse StdErr from ClangAst
        return new StreamParser(clavaData, dumpfile, lineStreamParser).parse(inputStream);
    }

    private FileResourceProvider getLibCResource(SupportedPlatform platform) {
        return clangAstResources.get(ClangAstFileResource.LIBC_CXX);
    }

}

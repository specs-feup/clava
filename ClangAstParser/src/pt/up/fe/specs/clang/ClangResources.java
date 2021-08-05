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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
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
    private final static String CLANG_FOLDERNAME = "clang_ast_exe";
    private final static String CLANG_INCLUDES_FOLDERNAME = "clang_includes";

    private final boolean dumpStdout;
    private final FileResourceManager clangAstResources;

    private static final AtomicInteger HAS_LIBC = new AtomicInteger(-1);

    public ClangResources(boolean dumpStdout) {
        this.dumpStdout = dumpStdout;
        clangAstResources = FileResourceManager.fromEnum(ClangAstFileResource.class);
    }

    public ClangFiles getClangFiles(String version, boolean usePlatformIncludes) {
        // Check if there is a local version of the Clang files
        ClangFiles localClangFiles = SpecsIo.getJarPath(ClangResources.class)
                .map(jarFolder -> new File(jarFolder, CLANG_FOLDERNAME))
                .filter(File::isDirectory)
                .flatMap(clangFolder -> loadLocalClangFiles(clangFolder, version))
                .orElse(null);

        if (localClangFiles != null) {
            ClavaLog.info("Using local version of ClangAstDumper ("
                    + localClangFiles.getClangExecutable().getAbsolutePath()
                    + "). Option 'use platform includes' will be ignored");
            return localClangFiles;
        }

        File clangExecutable = prepareResources(version);
        List<String> builtinIncludes = prepareIncludes(clangExecutable, usePlatformIncludes);

        return new ClangFiles(clangExecutable, builtinIncludes);
    }

    private Optional<ClangFiles> loadLocalClangFiles(File clangFolder, String version) {
        // Get versioned filename of dumper
        SupportedPlatform platform = SupportedPlatform.getCurrentPlatform();
        FileResourceProvider executableResource = getVersionedResource(getExecutableResource(platform), version);

        // FileResourceProvider executableResource = getVersionedExecutableResource(version, platform);

        var clangDumperExe = new File(clangFolder, executableResource.getFilename());
        if (!clangDumperExe.isFile()) {
            ClavaLog.info("!Found local ClangDumper folder '" + clangFolder.getAbsolutePath()
                    + "', but does not contain required file '"
                    + executableResource.getFilename() + "'. Please update the contents of the folder.");
            return Optional.empty();
        }

        // Get clang includes folder
        var clangIncludesFolder = new File(clangFolder, CLANG_INCLUDES_FOLDERNAME);
        if (!clangIncludesFolder.isDirectory()) {
            ClavaLog.info("!Found local ClangDumper folder '" + clangFolder.getAbsolutePath()
                    + "', but does not contain folder '" + CLANG_INCLUDES_FOLDERNAME
                    + "'. Please update the contents of the folder.");
            return Optional.empty();
        }

        // Get include folders
        var clangIncludes = SpecsIo.getFolders(clangIncludesFolder).stream()
                .map(File::getAbsolutePath)
                .collect(Collectors.toList());

        if (clangIncludes.isEmpty()) {
            ClavaLog.info("!Found local ClangDumper folder '" + clangFolder.getAbsolutePath()
                    + "', but folder '" + CLANG_INCLUDES_FOLDERNAME
                    + "' is empty. Please update the contents of the folder.");
            return Optional.empty();
        }

        return Optional.of(new ClangFiles(clangDumperExe, clangIncludes));
    }

    /**
     *
     * @return path to the executable that was copied
     */
    private File prepareResources(String version) {

        File resourceFolder = getClangResourceFolder();

        SupportedPlatform platform = SupportedPlatform.getCurrentPlatform();
        FileResourceProvider executableResource = getVersionedResource(getExecutableResource(platform), version);
        // FileResourceProvider executableResource = getVersionedExecutableResource(version, platform);

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

    /*
    private FileResourceProvider getVersionedExecutableResource(String version, SupportedPlatform platform) {
        FileResourceProvider executableResource = getExecutableResource(platform);
    
        // If version not defined, use the latest version of the resource
        if (version.isEmpty()) {
            version = executableResource.getVersion();
        }
    
        // ClangAst executable versions are separated by an underscore
        executableResource = executableResource.createResourceVersion("_" + version);
        return executableResource;
    }
    */

    private FileResourceProvider getVersionedResource(FileResourceProvider resource) {
        return getVersionedResource(resource, "");
    }

    private FileResourceProvider getVersionedResource(FileResourceProvider resource, String version) {

        // If version not defined, use the latest version of the resource
        if (version.isEmpty()) {
            version = resource.getVersion();
        }

        // ClangAst executable versions are separated by an underscore
        resource = resource.createResourceVersion("_" + version);
        return resource;
    }

    public static File getClangResourceFolder() {
        return SpecsIo.getTempFolder(CLANG_FOLDERNAME);
    }

    private FileResourceProvider getExecutableResource(SupportedPlatform platform) {

        switch (platform) {
        case WINDOWS:
            return CLANG_AST_RESOURCES.get(ClangAstFileResource.WIN_EXE);
        case LINUX_4:
            return CLANG_AST_RESOURCES.get(ClangAstFileResource.CENTOS_EXE);
        case LINUX_5:
            return CLANG_AST_RESOURCES.get(ClangAstFileResource.LINUX_EXE);
        case LINUX_ARMV7:
            throw new RuntimeException("Platform Linux-ARMV7 not currently supported");
        // return CLANG_AST_RESOURCES.get(ClangAstFileResource.LINUX_ARMV7_EXE);
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

        return windowsResources;
        // clangAstResources.get(resourceEnum)
        //
        // return Arrays.asList(WIN_DLL1, WIN_DLL2, WIN_DLL3);
    }

    private List<String> prepareIncludes(File clangExecutable, boolean usePlatformIncludes) {

        if (usePlatformIncludes) {
            return Collections.emptyList();
        }

        File resourceFolder = getClangResourceFolder();

        File includesBaseFolder = SpecsIo.mkdir(resourceFolder, CLANG_INCLUDES_FOLDERNAME);
        // ZipResourceManager zipManager = new ZipResourceManager(includesBaseFolder);

        // Create list of include zips
        List<FileResourceProvider> includesZips = new ArrayList<>();
        includesZips.add(CLANG_AST_RESOURCES.get(ClangAstFileResource.BUILTIN_INCLUDES));

        // Check if built-in libc/c++ needs to be included
        if (useBuiltinLibc(clangExecutable, usePlatformIncludes)) {
            var libcResource = getVersionedResource(getLibCResource(SupportedPlatform.getCurrentPlatform()));
            includesZips.add(libcResource);
            // includesZips.add(getLibCResource(SupportedPlatform.getCurrentPlatform()));
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
        // If usePlatformIncludes is enabled, never use built-in includes
        if (usePlatformIncludes) {
            return false;
        }

        return !hasLibC(clangExecutable);
        // return !usePlatformIncludes;
        /*
        // If platform includes is disabled, always use built-in headers
        if (!usePlatformIncludes) {
            return true;
        }
        
        // If headers of both libc and libc++ are available, do not use built-in libc
        return !hasLibC(clangExecutable);
        */
    }

    private boolean hasLibC(File clangExecutable) {
        var value = HAS_LIBC.get();

        // Check if initiallized
        if (value == -1) {
            var hasLibC = detectLibC(clangExecutable);
            value = hasLibC ? 1 : 0;
            HAS_LIBC.set(value);
        }

        if (value == 0) {
            return false;
        }

        if (value == 1) {
            return true;
        }

        throw new RuntimeException("Unexpected value: '" + value + "'");

        // Boolean.parseBoolean(CLANG_INCLUDES_FOLDERNAME)
        // var hasLibC = HAS_LIBC.get();
        //
        // if (hasLibC != null) {
        // return hasLibC;
        // }
        //
        // hasLibC = detectLibC(clangExecutable);
        // HAS_LIBC.set(hasLibC);
        // return hasLibC;
    }

    /**
     * Detects if the system has libc/licxx installed.
     *
     * @param clangExecutable
     * @return
     */
    private boolean detectLibC(File clangExecutable) {

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
            ClavaLog.debug("Could not find system libc/libcxx");
        } else {
            ClavaLog.debug("Detected system's libc and libcxx");
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
        switch (platform) {
        case WINDOWS:
            return clangAstResources.get(ClangAstFileResource.LIBC_CXX_WINDOWS);
        default:
            return clangAstResources.get(ClangAstFileResource.LIBC_CXX);
        }
        // return clangAstResources.get(ClangAstFileResource.LIBC_CXX);
    }

    public static File getBuiltinCudaLib() {
        var fileResource = CLANG_AST_RESOURCES.get(ClangAstFileResource.CUDA_LIB);
        var resourceFolder = getClangResourceFolder();
        var cudalibFolder = SpecsIo.mkdir(new File(resourceFolder, "cudalib"));

        // Download includes zips, check if any of them is new
        ResourceWriteData zipFile = fileResource.writeVersioned(resourceFolder, ClangAstParser.class);

        // If a new file has been written, delete includes folder, and extract all zips again
        // Extracting all because zips might have several folders and we are not determining which should be updated
        if (zipFile.isNewFile()) {
            // Clean folder
            SpecsIo.deleteFolderContents(cudalibFolder);

            // Extract zip
            SpecsIo.extractZip(zipFile.getFile(), cudalibFolder);
        }

        // Returnb cuda lib folder
        return cudalibFolder;
    }
}

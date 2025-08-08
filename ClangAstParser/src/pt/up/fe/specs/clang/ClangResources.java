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
 * specific language governing permissions and limitations under the License. under the License.
 */

package pt.up.fe.specs.clang;

import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.dumper.ClangAstDumper;
import pt.up.fe.specs.clang.parsers.TopLevelNodesParser;
import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.exceptions.CaseNotDefinedException;
import pt.up.fe.specs.util.providers.FileResourceManager;
import pt.up.fe.specs.util.providers.FileResourceProvider;
import pt.up.fe.specs.util.providers.FileResourceProvider.ResourceWriteData;
import pt.up.fe.specs.util.system.ProcessOutputAsString;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ClangResources {

    private static final FileResourceManager CLANG_AST_RESOURCES = FileResourceManager
            .fromEnum(ClangAstFileResource.class);

    private final static String CLANG_FOLDERNAME = "clang_ast_exe";
    private final static String CLANG_INCLUDES_FOLDERNAME = "clang_includes";

    private final FileResourceManager clangAstResources;
    private final CodeParser options;

    private final Map<String, ClangFiles> clangFilesCache;

    private static final AtomicInteger HAS_LIBC = new AtomicInteger(-1);

    public ClangResources(CodeParser options) {
        clangAstResources = FileResourceManager.fromEnum(ClangAstFileResource.class);
        this.options = options;
        this.clangFilesCache = new HashMap<>();
    }

    public ClangFiles getClangFiles(String version, LibcMode libcMode) {

        // Create key
        var key = libcMode.name() + "_" + version;

        // Check if cached
        var files = clangFilesCache.get(key);
        if (files != null) {
            return files;
        }

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

            // Store in cache
            clangFilesCache.put(key, localClangFiles);

            return localClangFiles;
        }

        File clangExecutable = prepareResources(version);
        List<String> builtinIncludes = prepareIncludes(clangExecutable, libcMode);

        var newFiles = new ClangFiles(clangExecutable, builtinIncludes);

        // Store in cache
        clangFilesCache.put(key, newFiles);

        return newFiles;
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
     * @return path to the executable that was copied
     */
    private File prepareResources(String version) {

        File resourceFolder = getClangResourceFolder();

        SupportedPlatform platform = SupportedPlatform.getCurrentPlatform();
        FileResourceProvider executableResource = getVersionedResource(getExecutableResource(platform), version);
        // FileResourceProvider executableResource = getVersionedExecutableResource(version, platform);

        // Copy executable
        ResourceWriteData executable = executableResource.writeVersioned(resourceFolder, ClangResources.class);

        // If Windows, copy additional dependencies
        if (platform == SupportedPlatform.WINDOWS) {
            for (FileResourceProvider resource : getWindowsResources()) {
                resource.writeVersioned(resourceFolder, ClangResources.class);
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

    private Optional<FileResourceProvider> getCustomExecutable() {
        // Check if theres is a custom executable
        var customExe = options.get(CodeParser.CUSTOM_CLANG_AST_DUMPER_EXE);

        if (customExe.getName().isBlank()) {
            return Optional.empty();
        }

        if (!customExe.isFile()) {
            SpecsLogs.info("Specified a custom executable but could not find file '" + customExe
                    + "', using built-in executable");

            return Optional.empty();
        }

        SpecsLogs.info("Using custom executable for ClangAstDumper: '" + customExe.getAbsolutePath() + "'");

        return Optional.of(FileResourceProvider.newInstance(customExe));
    }

    private FileResourceProvider getExecutableResource(SupportedPlatform platform) {

        var customExecutable = getCustomExecutable();

        if (customExecutable.isPresent()) {
            return customExecutable.get();
        }

        switch (platform) {
            case WINDOWS:
                return CLANG_AST_RESOURCES.get(ClangAstFileResource.WIN_EXE);
            case LINUX_5:
                if (ClangAstDumper.usePlugin()) {
                    return CLANG_AST_RESOURCES.get(ClangAstFileResource.LINUX_PLUGIN);
                } else {
                    return CLANG_AST_RESOURCES.get(ClangAstFileResource.LINUX_EXE);
                }

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
        windowsResources.add(CLANG_AST_RESOURCES.get(ClangAstFileResource.WIN_LLVM_DLL));

        return windowsResources;
    }

    private List<String> prepareIncludes(File clangExecutable, LibcMode libcMode) {

        // Use no built-ins
        /*
        if (libcMode == LibcMode.SYSTEM) {
            return Collections.emptyList();
        }
         */
        // Should not use basic includes if mode is System?
        // if (usePlatformIncludes) {
        // return Collections.emptyList();
        // }

        File resourceFolder = getClangResourceFolder();

        File includesBaseFolder = SpecsIo.mkdir(resourceFolder, CLANG_INCLUDES_FOLDERNAME);
        // ZipResourceManager zipManager = new ZipResourceManager(includesBaseFolder);

        // Create list of include zips
        List<FileResourceProvider> includesZips = new ArrayList<>();

        // Get libc_libcxx, if required
        if (useBuiltinLibc(clangExecutable, libcMode)) {
            var builtinResource = CLANG_AST_RESOURCES.get(ClangAstFileResource.BUILTIN_INCLUDES);
            includesZips.add(getVersionedResource(builtinResource, builtinResource.getVersion()));

        }

        // Always add OpenMP includes
        includesZips.add(CLANG_AST_RESOURCES.get(ClangAstFileResource.OPENMP_INCLUDES));

        // Download includes zips, check if any of them is new
        List<ResourceWriteData> zipFiles = includesZips.stream()
                .map(resource -> resource.writeVersioned(resourceFolder, ClangResources.class))
                .collect(Collectors.toList());

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
        SpecsLogs.debug(() -> "Includes folders: " + includes);

        // If on linux, make folders and files accessible to all users
        if (SupportedPlatform.getCurrentPlatform().isLinux()) {
            SpecsSystem.runProcess(Arrays.asList("chmod", "-R", "777", resourceFolder.getAbsolutePath()), false, true);
        }

        return includes;
    }

    private boolean useBuiltinLibc(File clangExecutable, LibcMode libcMode) {

        switch (libcMode) {
            case AUTO:
                return !hasLibC(clangExecutable);
            // Builtin and libc/libcxx are now merged in the same zip
            case BUILTIN_AND_LIBC:
            case BASE_BUILTIN_ONLY:
                return true;
            case SYSTEM:
                return false;
            default:
                throw new CaseNotDefinedException(libcMode);
        }
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

            // Invoke dumper
            var output = runClangAstDumper(clangExecutable, testFile);

            // First check if there where no problems running the dumper
            if (output.getReturnValue() != 0) {
                ClavaLog.info("Problems while running dumper to test in libc/libcxx is needed");
                needsLib = true;
                break;
            }

            // Test files where built in such a way so that if a system include is present, it will generate code with a
            // top level nodes, otherwise it generates an empty file
            var topLevelNodesHeader = TopLevelNodesParser.getTopLevelNodesHeader();

            var foundInclude = output.getOutput().contains(topLevelNodesHeader);

            if (!foundInclude) {
                needsLib = true;
                break;
            }

        }

        if (needsLib) {
            ClavaLog.debug("Could not find system libc/libcxx");
        } else {
            ClavaLog.debug("Detected system's libc and libcxx");
        }

        return !needsLib;

    }

    private ProcessOutputAsString runClangAstDumper(File clangExecutable, File testFile) {
        List<String> arguments = Arrays.asList(clangExecutable.getAbsolutePath(), testFile.getAbsolutePath(), "--");
        return SpecsSystem.runProcess(arguments, true, false);
    }

    public static File getBuiltinCudaLib() {
        var fileResource = CLANG_AST_RESOURCES.get(ClangAstFileResource.CUDA_LIB);
        var resourceFolder = getClangResourceFolder();
        var cudalibFolder = SpecsIo.mkdir(new File(resourceFolder, "cudalib"));

        // Download includes zips, check if any of them is new
        ResourceWriteData zipFile = fileResource.writeVersioned(resourceFolder, ClangResources.class);

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

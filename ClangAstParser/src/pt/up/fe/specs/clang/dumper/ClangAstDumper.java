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

package pt.up.fe.specs.clang.dumper;

import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.JOptionsUtils;
import org.suikasoft.jOptions.streamparser.LineStreamParser;
import pt.up.fe.specs.clang.AstDumpCache;
import pt.up.fe.specs.clang.ClangAstKeys;
import pt.up.fe.specs.clang.ClangResources;
import pt.up.fe.specs.clang.LibcMode;
import pt.up.fe.specs.clang.cilk.CilkParser;
import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.clang.parsers.ClangStreamParserV2;
import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.clava.utils.SourceType;
import pt.up.fe.specs.lang.SpecsPlatforms;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.parsing.arguments.ArgumentsParser;
import pt.up.fe.specs.util.system.ProcessOutput;
import pt.up.fe.specs.util.utilities.LineStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Calls the ClangAstDumper executable and returns the dumped information. Clava AST can be built based on this output.
 *
 * @author JoaoBispo
 */
public class ClangAstDumper {

    private final static boolean USE_PLUGIN = false;
    private final static String SYSTEM_HEADER_THRESHOLD_OPTION = "-system-header-threshold=";

    public static boolean usePlugin() {
        return USE_PLUGIN;
    }

    private final static String CLANG_DUMP_FILENAME = "clangDump.txt";
    private final static String STDERR_DUMP_FILENAME = "stderr.txt";
    private static final String DEPENDENCY_DOT_FILENAME = "clangDependencies.dot";

    private static final List<String> CLANG_AST_DUMPER_TEMP_FILES = List.of("includes.txt", CLANG_DUMP_FILENAME,
            // "clavaDump.txt", "nodetypes.txt", "types.txt", "is_temporary.txt", "template_args.txt",
            "clavaDump.txt", "nodetypes.txt", "types.txt", "is_temporary.txt",
            "omp.txt", "invalid_source.txt", "enum_integer_type.txt", "consumer_order.txt",
            "types_with_templates.txt");

    public static List<String> getTempFiles() {
        return CLANG_AST_DUMPER_TEMP_FILES;
    }

    /**
     * TODO: Not implemented yet
     * <p>
     * If true, displays the output of the dumper while it executes. If false, stores the output and only shows it after
     * execution.
     * <p>
     * Usually should be disabled when executing several versions of the parser concurrently.
     */
    private final boolean streamConsoleOutput;

    private final List<File> workingFolders;
    private File lastWorkingFolder;
    private File baseFolder;
    private File clangExecutable;
    private List<String> builtinIncludes;
    private File systemResourceDir;
    private int systemIncludesThreshold;
    private final ClangResources clangResources;

    private final CodeParser parserConfig;

    /**
     * TODO: Replace some of the arguments with reads to CodeParser?
     *
     * @param dumpStdOut
     * @param useCustomResources
     * @param streamConsoleOutput
     * @param clangExecutable
     * @param builtinIncludes
     * @param systemResourceDir
     * @param parserConfig
     */
    public ClangAstDumper(boolean streamConsoleOutput,
                          File clangExecutable, List<String> builtinIncludes, File systemResourceDir,
                          CodeParser parserConfig) {

        this.streamConsoleOutput = streamConsoleOutput;

        this.clangExecutable = clangExecutable;
        this.builtinIncludes = builtinIncludes;
        this.systemResourceDir = systemResourceDir;

        this.workingFolders = new ArrayList<>();
        this.lastWorkingFolder = null;
        this.baseFolder = null;
        this.systemIncludesThreshold = ParallelCodeParser.SYSTEM_INCLUDES_THRESHOLD.getDefault().get();
        this.parserConfig = parserConfig;
        this.clangResources = new ClangResources(parserConfig);
    }

    public File getLastWorkingFolder() {
        return lastWorkingFolder;
    }

    public ClangAstDumper setBaseFolder(File baseFolder) {
        this.baseFolder = baseFolder;
        return this;
    }

    public ClangAstDumper setSystemIncludesThreshold(int systemIncludesThreshold) {
        this.systemIncludesThreshold = systemIncludesThreshold;
        return this;
    }

    public ClangAstData parse(File sourceFile, String id, Standard standard, DataStore config) {

        // Pre-processing before the parsing
        if (config.get(ClangAstKeys.USES_CILK)) {

            // Prepare source file
            sourceFile = new CilkParser().prepareCilkFile(sourceFile);
        }

        return parsePrivate(sourceFile, id, standard, config);
    }

    private ClangAstData parsePrivate(File sourceFile, String id, Standard standard, DataStore config) {
        ClavaLog.debug(() -> "Data store config for single file parser: " + config);
        // A cache hit does not create a working folder. Do not expose the folder from a previous parse as the result
        // of this one.
        lastWorkingFolder = null;

        DataStore localData = JOptionsUtils.loadDataStore(LocalOptionsKeys.getLocalOptionsFilename(), getClass(),
                LocalOptionsKeys.getProvider().getStoreDefinition());

        // Keep this phase free of side effects. The cache key must describe the exact invocation that would be
        // launched, including all defaults and resource paths resolved below.
        List<String> arguments = buildArguments(sourceFile, id, standard, config, localData);

        ClavaLog.debug(() -> "Calling Clang AST Dumper: " + arguments);

        ParsedDump parsedDump;
        boolean showClangDump = config.get(CodeParser.SHOW_CLANG_DUMP);
        if (!showClangDump) {
            AstDumpCache cache = new AstDumpCache(parserConfig.get(CodeParser.DUMPER_FOLDER).toPath(),
                    sourceFile.toPath(), arguments);

            var cachedDump = cache.load(input -> processStdErr(input, config.get(ClavaNode.CONTEXT),
                    OutputStream.nullOutputStream(), false));
            if (cachedDump.isPresent()) {
                parsedDump = cachedDump.get();
                parsedDump.data().set(ClangAstData.HAS_ERRORS, false);
                return materializeTranslationUnit(parsedDump, sourceFile, config);
            }

            parsedDump = cache.capture(cacheOutput -> runDumper(arguments, sourceFile, id, config, cacheOutput),
                    this::getDependencies,
                    result -> !result.data().get(ClangAstData.HAS_ERRORS) && !result.parserHadExceptions()
                            && result.dependenciesAvailable());
        } else {
            // clangDump.txt is a side effect of the process and has no cache representation. In particular, do not
            // publish a cache entry that would make a subsequent SHOW_CLANG_DUMP parse silently lose that output.
            parsedDump = runDumper(arguments, sourceFile, id, config, OutputStream.nullOutputStream());
        }

        return materializeTranslationUnit(parsedDump, sourceFile, config);
    }

    private List<String> buildArguments(File sourceFile, String id, Standard standard, DataStore config,
                                         DataStore localData) {
        List<String> arguments = new ArrayList<>();
        if (USE_PLUGIN && SpecsPlatforms.isLinux()) {
            arguments.add("clang-16");
            arguments.add("-c");

            arguments.add("-Xclang");
            arguments.add("-load");
            arguments.add("-Xclang");
            arguments.add(clangExecutable.getAbsolutePath());
            arguments.add("-Xclang");
            arguments.add("-plugin");
            arguments.add("-Xclang");
            arguments.add("DumpAst");
            arguments.add("-Xclang");
            arguments.add("-plugin-arg-DumpAst");
            arguments.add("-Xclang");
            arguments.add("-file-id=" + id);
            arguments.add("-Xclang");
            arguments.add("-plugin-arg-DumpAst");
            arguments.add("-Xclang");
            arguments.add(SYSTEM_HEADER_THRESHOLD_OPTION + systemIncludesThreshold);
        } else {
            arguments.add(clangExecutable.getAbsolutePath());

            arguments.add(sourceFile.getAbsolutePath());

            arguments.add("-id=" + id);

            arguments.add(SYSTEM_HEADER_THRESHOLD_OPTION + systemIncludesThreshold);

            arguments.add("--");
        }

        var extension = SpecsIo.getExtension(sourceFile);
        boolean isOpenCL = extension.equals("cl");
        boolean isCuda = extension.equals("cu");

        // Compilation of header files always need a standard, but OpenCL compilation fails if there is a standard
        // specified that is not an OpenCL standard.
        if (isOpenCL && !standard.isOpenCL()) {

            // Using OpenCL 2.0 as default
            arguments.add("-std=cl2.0");
        }
        // Set standard to CUDA
        else if (isCuda) {
            // The LLVM 18 driver bundled with clang-dumper rejects '-std=cuda'. The .cu extension already
            // selects CUDA mode, so use a C++ standard for host-side parsing.
            arguments.add(standard.isCxx() ? standard.getFlag() : Standard.CXX17.getFlag());
        } else {
            arguments.add(standard.getFlag());
        }

        // If OpenCL file, add necessary flags
        if (isOpenCL) {
            // OpenCL parsing
            // arguments.add("-x");
            // arguments.add("cl");

            // OpenCL header file
            arguments.add("-include");
            arguments.add("opencl-c.h");

            // Needed for the current OpenCL header file
            arguments.add("-fblocks");

            // arguments.add("-D__OPENCL_C_VERSION__=100");

            // Enable extensions
            arguments.add("-Dcl_khr_int64_base_atomics");
            arguments.add("-Dcl_khr_int64_extended_atomics");
            arguments.add("-Dcl_khr_fp16");
            arguments.add("-Dcl_khr_fp64");

        }
        // If CUDA, add corresponding flags
        else if (isCuda) {
            if (!SpecsPlatforms.isLinux()) {
                ClavaLog.info("We only officially support CUDA parsing in Linux, run at your own risk");
                arguments.add("-fms-compatibility");
                if (SpecsPlatforms.isWindows()) {
                    arguments.add("-D_MSC_VER");
                    arguments.add("-D_LIBCPP_MSVCRT");
                }
            }

            arguments.add("--cuda-gpu-arch=" + parserConfig.get(CodeParser.CUDA_GPU_ARCH));

            var cudaPath = parserConfig.get(CodeParser.CUDA_PATH);
            addCudaPathArgument(arguments, cudaPath);

            // Since we only need parsing, enable host-only
            // Can help with errors such as "__float128 is not supported on this target"
            arguments.add("--cuda-host-only");

        }

        // If header file, add the language flag (-x) that corresponds to the standard
        else if (SourceType.isHeader(sourceFile)) {
            arguments.add("-x");
            arguments.add(standard.isCxx() ? "c++" : "c");
        }

        if (systemResourceDir != null) {
            arguments.add("-resource-dir=" + systemResourceDir.getAbsolutePath());
        }

        // The parser has already resolved the libc policy before creating this per-file configuration.
        if (config.get(ClangAstKeys.LIBC_CXX_MODE) == LibcMode.BUILTIN_AND_LIBC) {
            arguments.add("-nostdinc");
            arguments.add("-nostdinc++");
        }

        List<String> systemIncludes = new ArrayList<>();

        // Add bundled includes according to libc/cxx settings
        systemIncludes.addAll(builtinIncludes);

        // Add custom includes
        systemIncludes.addAll(localData.get(LocalOptionsKeys.SYSTEM_INCLUDES).getStringList());

        // Add local system includes
        for (String systemInclude : systemIncludes) {
            arguments.add("-isystem");
            arguments.add(systemInclude);
        }

        arguments.addAll(ArgumentsParser.newCommandLine().parse(config.get(ClavaOptions.FLAGS)));

        arguments.addAll(config.get(ClavaOptions.FLAGS_LIST));

        // ClangTool does not accept the driver's -MMD/-MF options. Its frontend dependency-dot option is accepted
        // through -Xclang and emits the complete transitive include graph in the per-invocation working directory.
        // Keep the filename stable: the working folder is intentionally not part of the cache key.
        arguments.add("-Xclang");
        arguments.add("-dependency-dot");
        arguments.add("-Xclang");
        arguments.add(DEPENDENCY_DOT_FILENAME);
        arguments.add("-Xclang");
        arguments.add("-sys-header-deps");

        return arguments;
    }

    private ParsedDump runDumper(List<String> arguments, File sourceFile, String id, DataStore config,
                                 OutputStream cacheOutput) {
        try {
            // Create temporary working folder only after the cache lookup has missed. A hit has no working folder.
            lastWorkingFolder = SpecsIo.mkdir(baseFolder, sourceFile.getName() + "_" + id);

            // Ensure folder is empty
            SpecsIo.deleteFolderContents(lastWorkingFolder);

            workingFolders.add(lastWorkingFolder);

            ProcessOutput<String, ParsedDump> output = SpecsSystem.runProcess(arguments, lastWorkingFolder,
                    this::processOutput,
                    inputStream -> this.processStdErr(inputStream, config.get(ClavaNode.CONTEXT), cacheOutput, true));

            if (output.isError()) {
                ClavaLog.debug("Dumper returned an error value: '" + output.getReturnValue() + "'");
            }

            // If exception happened while processing output, throw exception
            output.getOutputException().ifPresent(exception -> {
                throw new RuntimeException("Exception while processing the output streams", exception);
            });

            ParsedDump parsedDump = Objects.requireNonNull(output.getStdErr(),
                    () -> "Did not expect error output to be null");
            parsedDump.data().set(ClangAstData.HAS_ERRORS, output.isError());
            DependencyDot dependencyDot = readDependencyDot(lastWorkingFolder);
            parsedDump = parsedDump.withDependencies(dependencyDot.paths(), dependencyDot.available());

            // If console output streaming is disabled, show output only at the end
            if (!streamConsoleOutput) {
                ClavaLog.info(output.getStdOut());
            }

            if (parsedDump.parserHadExceptions()) {
                SpecsLogs.warn("Exceptions happened while parsing the file '" + sourceFile.getAbsolutePath() + "'");
            }

            return parsedDump;
        } catch (Exception e) {
            throw new RuntimeException("Error while running Clang AST dumper", e);
        }
    }

    private ClangAstData materializeTranslationUnit(ParsedDump parsedDump, File sourceFile, DataStore config) {
        ClangAstData parsedData = parsedDump.data();
        ClangAstParser clangStreamParser = new ClangAstParser(parsedData, SpecsSystem.isDebug(), config);

        TranslationUnit tUnit = clangStreamParser.parseTu(sourceFile);

        parsedData.set(ClangAstData.TRANSLATION_UNIT, tUnit);

        return parsedData;
    }

    private Collection<Path> getDependencies(ParsedDump parsedDump) {
        Set<Path> dependencies = new HashSet<>(parsedDump.dependencies());
        dependencies.add(clangExecutable.toPath());

        Map<String, String> idToFilename = parsedDump.data().get(ClangAstData.ID_TO_FILENAME_MAP);
        if (idToFilename != null) {
            for (String filename : idToFilename.values()) {
                if (filename == null) {
                    continue;
                }

                try {
                    dependencies.add(Path.of(filename));
                } catch (InvalidPathException ignored) {
                    // Pseudo-paths from the dumper are not file dependencies.
                }
            }
        }

        return dependencies;
    }

    private void addCudaPathArgument(List<String> arguments, String cudaPath) {
        var useBuiltinCudaLib = cudaPath.toUpperCase().equals(CodeParser.getBuiltinOption());

        if (useBuiltinCudaLib) {
            File cudaFolder = clangResources.getBuiltinCudaLib();

            ClavaLog.debug("Setting --cuda-path to built-in CUDA folder '"
                    + cudaFolder.getAbsolutePath() + "'");
            arguments.add("--cuda-path=" + cudaFolder.getAbsolutePath());
        } else if (!cudaPath.isBlank()) {
            File cudaFolder = SpecsIo.existingFolder(cudaPath);

            ClavaLog.debug("Setting --cuda-path to folder '" + cudaFolder.getAbsolutePath() + "'");
            arguments.add("--cuda-path=" + cudaFolder.getAbsolutePath());
        }
    }

    private String processOutput(InputStream inputStream) {
        StringBuilder output = new StringBuilder();
        try (LineStream lines = LineStream.newInstance(inputStream, null)) {

            while (lines.hasNextLine()) {
                String nextLine = lines.nextLine();

                // Ignore line about 'invalid argument', it will happen when input source is a header file
                if (streamConsoleOutput) {
                    ClavaLog.info(nextLine);
                }

                output.append(nextLine).append("\n");
            }
        }

        return output.toString();
    }

    private ParsedDump processStdErr(InputStream inputStream, ClavaContext context, OutputStream cacheOutput,
                                     boolean closeInputStream) {
        // Create LineStreamParser
        try (LineStreamParser<ClangAstData> lineStreamParser = ClangStreamParserV2.newInstance(context)) {

            // Set debug
            if (SpecsSystem.isDebug()) {
                lineStreamParser.getData().set(ClangAstData.DEBUG, true);
            }

            // Dump file
            File dumpfile = SpecsSystem.isDebug() ? new File(STDERR_DUMP_FILENAME) : null;

            // Parse input stream
            String linesNotParsed = lineStreamParser.parse(
                    new TeeInputStream(inputStream, cacheOutput, closeInputStream), dumpfile);

            // Add lines not parsed to DataStore
            ClangAstData data = lineStreamParser.getData();
            data.set(ClangAstData.LINES_NOT_PARSED, linesNotParsed);

            // Return data and retain parser exceptions for the cache publication decision.
            return new ParsedDump(data, lineStreamParser.hasExceptions(), Set.of(), true);
        } catch (Exception e) {
            throw new RuntimeException("Error while parsing output of Clang AST dumper", e);
        }

    }

    private record ParsedDump(ClangAstData data, boolean parserHadExceptions, Collection<Path> dependencies,
                              boolean dependenciesAvailable) {

        private ParsedDump withDependencies(Collection<Path> dependencies, boolean dependenciesAvailable) {
            return new ParsedDump(data, parserHadExceptions, dependencies, dependenciesAvailable);
        }
    }

    private record DependencyDot(Collection<Path> paths, boolean available) {
    }

    private DependencyDot readDependencyDot(File workingFolder) {
        Path dependencyDot = workingFolder.toPath().resolve(DEPENDENCY_DOT_FILENAME);
        if (!java.nio.file.Files.isRegularFile(dependencyDot)) {
            SpecsLogs.debug(() -> "Clang dumper did not produce dependency file '" + dependencyDot + "'");
            return new DependencyDot(Set.of(), false);
        }

        try {
            Set<Path> dependencies = new HashSet<>();
            int labels = 0;
            for (String line : java.nio.file.Files.readAllLines(dependencyDot)) {
                Optional<String> label = parseDependencyDotLabel(line);
                if (label.isEmpty()) {
                    continue;
                }

                labels++;
                Optional<Path> path = resolveDependencyPath(label.get(), workingFolder);
                if (path.isEmpty()) {
                    SpecsLogs.debug(() -> "Could not resolve dependency from Clang dependency file line '" + line
                            + "'");
                    return new DependencyDot(Set.of(), false);
                }

                dependencies.add(path.get());
            }

            return new DependencyDot(dependencies, labels > 0);
        } catch (IOException | InvalidPathException e) {
            SpecsLogs.debug(() -> "Could not read Clang dependency file '" + dependencyDot + "': " + e.getMessage());
            return new DependencyDot(Set.of(), false);
        }
    }

    private Optional<String> parseDependencyDotLabel(String line) {
        int start = line.indexOf("label=\"");
        if (start < 0) {
            return Optional.empty();
        }

        StringBuilder escaped = new StringBuilder();
        boolean isEscaped = false;
        for (int i = start + "label=\"".length(); i < line.length(); i++) {
            char current = line.charAt(i);
            if (isEscaped) {
                escaped.append('\\').append(current);
                isEscaped = false;
            } else if (current == '\\') {
                isEscaped = true;
            } else if (current == '"') {
                return Optional.of(decodeDependencyDotLabel(escaped.toString()));
            } else {
                escaped.append(current);
            }
        }

        return Optional.empty();
    }

    private String decodeDependencyDotLabel(String escaped) {
        StringBuilder decoded = new StringBuilder(escaped.length());
        boolean isEscaped = false;
        for (int i = 0; i < escaped.length(); i++) {
            char current = escaped.charAt(i);
            if (!isEscaped) {
                if (current == '\\') {
                    isEscaped = true;
                } else {
                    decoded.append(current);
                }
                continue;
            }

            decoded.append(switch (current) {
                case 'n' -> '\n';
                case 'r' -> '\r';
                case 't' -> '\t';
                default -> current;
            });
            isEscaped = false;
        }

        if (isEscaped) {
            decoded.append('\\');
        }

        return decoded.toString();
    }

    private Optional<Path> resolveDependencyPath(String dependency, File workingFolder) {
        Path path = Path.of(dependency);
        List<Path> candidates = new ArrayList<>();
        if (path.isAbsolute()) {
            candidates.add(path);
        } else {
            candidates.add(workingFolder.toPath().resolve(path));
            // Clang's dependency-dot output strips the leading slash from absolute POSIX paths.
            if (File.separatorChar == '/') {
                candidates.add(Path.of(File.separator).resolve(path));
            }
            candidates.add(path);
        }

        for (Path candidate : candidates) {
            try {
                Path canonical = candidate.toRealPath();
                if (java.nio.file.Files.isRegularFile(canonical)) {
                    return Optional.of(canonical);
                }
            } catch (IOException ignored) {
                // The compiler can report pseudo-paths or files that disappear between parsing and manifest build.
            }
        }

        return Optional.empty();
    }

    /** Copies stderr bytes as they are consumed, without buffering the complete dumper output. */
    private static final class TeeInputStream extends InputStream {
        private final InputStream delegate;
        private final OutputStream copy;
        private final boolean closeDelegate;

        private TeeInputStream(InputStream delegate, OutputStream copy, boolean closeDelegate) {
            this.delegate = delegate;
            this.copy = copy;
            this.closeDelegate = closeDelegate;
        }

        @Override
        public int read() throws IOException {
            int value = delegate.read();
            if (value != -1) {
                copy.write(value);
            }
            return value;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            int count = delegate.read(bytes, offset, length);
            if (count > 0) {
                copy.write(bytes, offset, count);
            }
            return count;
        }

        @Override
        public void close() throws IOException {
            if (closeDelegate) {
                delegate.close();
            }
        }
    }

    /**
     * TODO: Current implementation only shows the last file, show all files
     */
    public String getClangDump() {
        if (workingFolders.isEmpty()) {
            SpecsLogs.msgInfo("No working folders found, returning empty clang dump");
            return "";
        }

        StringBuilder clangDump = new StringBuilder();

        for (File workingFolder : workingFolders) {
            File clangDumpFile = new File(workingFolder, CLANG_DUMP_FILENAME);

            if (!clangDumpFile.isFile()) {
                SpecsLogs.msgInfo("Clang dump file no found: '" + clangDumpFile + "'");
                continue;
            }

            clangDump.append("ClangDump for '" + workingFolder.getName() + "':\n");
            clangDump.append(SpecsIo.read(clangDumpFile));
        }

        return clangDump.toString();
    }

}

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

package pt.up.fe.specs.clang.codeparser.clangparser;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.JOptionsUtils;
import org.suikasoft.jOptions.Interfaces.DataStore;
import org.suikasoft.jOptions.streamparser.LineStreamParser;

import pt.up.fe.specs.clang.ClangAstKeys;
import pt.up.fe.specs.clang.ClangAstParser;
import pt.up.fe.specs.clang.ClangResources;
import pt.up.fe.specs.clang.cilk.CilkParser;
import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.clang.datastore.LocalOptionsKeys;
import pt.up.fe.specs.clang.parsers.ClangParserData;
import pt.up.fe.specs.clang.parsers.ClangStreamParserV2;
import pt.up.fe.specs.clang.streamparserv2.ClangStreamParser;
import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.context.ClavaContext;
import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.clava.utils.SourceType;
import pt.up.fe.specs.lang.SpecsPlatforms;
import pt.up.fe.specs.util.SpecsCheck;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.parsing.arguments.ArgumentsParser;
import pt.up.fe.specs.util.system.ProcessOutput;
import pt.up.fe.specs.util.utilities.LineStream;

/**
 * Implementation of ClangParser that builds a TranslationUnit based on the AST dump of Clang.
 * 
 * <p>
 * This parsed will be deprecated once refactoring of ClavaNodes to DataStore format is done.
 * 
 * @author JoaoBispo
 *
 */
public class AstDumpParser implements ClangParser {

    private final static String CLANG_DUMP_FILENAME = "clangDump.txt";
    private final static String STDERR_DUMP_FILENAME = "stderr.txt";

    // private final static String HEADER_WARNING_PREFIX = "error: invalid argument '";
    // private final static String HEADER_WARNING_SUFFIX = "' not allowed with 'C/ObjC'";

    // private int currentId;
    private final boolean dumpStdOut;
    private final boolean useCustomResources;
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
    // private boolean usePlatformLibc;
    private File clangExecutable;
    private List<String> builtinIncludes;
    private int systemIncludesThreshold;

    private final CodeParser parserConfig;

    // public AstDumpParser() {
    // this(false, false, true);
    // }

    /**
     * TODO: Replace some of the arguments with reads to CodeParser?
     * 
     * @param dumpStdOut
     * @param useCustomResources
     * @param streamConsoleOutput
     * @param clangExecutable
     * @param builtinIncludes
     * @param parserConfig
     */
    public AstDumpParser(boolean dumpStdOut, boolean useCustomResources, boolean streamConsoleOutput,
            File clangExecutable, List<String> builtinIncludes, CodeParser parserConfig) {
        // this.currentId = 0;
        this.dumpStdOut = dumpStdOut;
        this.useCustomResources = useCustomResources;
        this.streamConsoleOutput = streamConsoleOutput;

        this.clangExecutable = clangExecutable;
        this.builtinIncludes = builtinIncludes;

        this.workingFolders = new ArrayList<>();
        this.lastWorkingFolder = null;
        this.baseFolder = null;
        this.systemIncludesThreshold = ParallelCodeParser.SYSTEM_INCLUDES_THRESHOLD.getDefault().get();
        this.parserConfig = parserConfig;
        // this.usePlatformLibc = false;
        // context = new ClavaContext();
    }

    @Override
    public File getLastWorkingFolder() {
        return lastWorkingFolder;
    }

    @Override
    public AstDumpParser setBaseFolder(File baseFolder) {
        this.baseFolder = baseFolder;
        return this;
    }

    public AstDumpParser setSystemIncludesThreshold(int systemIncludesThreshold) {
        this.systemIncludesThreshold = systemIncludesThreshold;
        return this;
    }

    // private int nextId() {
    // // Increment and return
    // currentId++;
    //
    // return currentId;
    // }

    //
    // @Override
    // public TranslationUnit parse(File file, DataStore config) {
    //
    // ClangRootNode clangRootNode = parseSource(file, config);
    //
    // // Parse dump information
    // try (ClavaParser clavaParser = new ClavaParser(clangRootNode)) {
    // return clavaParser.parseTranslationUnit(file);
    // } catch (Exception e) {
    // throw new RuntimeException("Could not parse file '" + file + "'", e);
    // }
    //
    // // if (get(SHOW_CLANG_DUMP)) {
    // // SpecsLogs.msgInfo("Clang Dump:\n" + SpecsIo.read(new File(ClangAstParser.getClangDumpFilename())));
    // // }
    // //
    // // if (get(SHOW_CLANG_AST)) {
    // // SpecsLogs.msgInfo("Clang AST:\n" + ast);
    // // }
    // }

    @Override
    public ClangParserData parse(File sourceFile, String id, Standard standard, DataStore config) {
        // Pre-processing before the parsing
        if (config.get(ClangAstKeys.USES_CILK)) {
            // Prepare source file
            sourceFile = new CilkParser().prepareCilkFile(sourceFile);
        }

        return parsePrivate(sourceFile, id, standard, config);

        // try {
        // return parsePrivate(sourceFile, id, standard, config);
        // } catch (Exception e) {
        // throw e;
        // } finally {
        // if (config.get(ClangAstKeys.USES_CILK)) {
        // // Restore source file
        // new CilkParser(sourceFile).restoreCilkFile();
        // }
        //
        // }
    }

    private ClangParserData parsePrivate(File sourceFile, String id, Standard standard, DataStore config) {

        ClavaLog.debug(() -> "Data store config for Clang AST Dumper: " + config);
        // Create instance of ClangAstParser
        // ClangAstParser clangAstParser = new ClangAstParser(dumpStdOut, useCustomResources);

        DataStore localData = JOptionsUtils.loadDataStore(ClangAstParser.getLocalOptionsFile(), getClass(),
                LocalOptionsKeys.getProvider().getStoreDefinition());

        // Apply local options
        // applyLocalOptions(localData);

        // Get version for the executable
        // String version = config.get(ClangAstKeys.CLANGAST_VERSION);
        // boolean usePlatformIncludes = config.get(ClangAstKeys.USE_PLATFORM_INCLUDES);

        // Copy resources
        // File clangExecutable = clangAstParser.prepareResources(version);

        List<String> arguments = new ArrayList<>();
        arguments.add(clangExecutable.getAbsolutePath());

        arguments.add(sourceFile.getAbsolutePath());

        // int id = nextId();
        arguments.add("-id=" + id);

        arguments.add("-system-header-threshold=" + systemIncludesThreshold);

        arguments.add("--");

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
        else if (isCuda && !standard.isCuda()) {

            arguments.add("-std=cuda");
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
            if (SpecsPlatforms.isWindows()) {
                ClavaLog.info("CUDA parsing is not supported in Windows, run at your own risk");
                arguments.addAll(Arrays.asList("-fms-compatibility", "-D_MSC_VER", "-D_LIBCPP_MSVCRT"));
            }

            arguments.add("--cuda-gpu-arch=" + parserConfig.get(CodeParser.CUDA_GPU_ARCH));

            var cudaPath = parserConfig.get(CodeParser.CUDA_PATH);
            if (!cudaPath.isBlank()) {

                // Check if should use built-in CUDA lib
                File cudaFolder = cudaPath.toUpperCase().equals(CodeParser.getBuiltinOption())
                        ? ClangResources.getBuiltinCudaLib()
                        : SpecsIo.existingFolder(cudaPath);

                arguments.add("--cuda-path=" + cudaFolder.getAbsolutePath());
            }

        }
        // If header file, add the language flag (-x) that corresponds to the standard
        else if (SourceType.isHeader(sourceFile)) {
            arguments.add("-x");
            arguments.add(standard.isCxx() ? "c++" : "c");
        }

        List<String> systemIncludes = new ArrayList<>();

        // Add includes bundled with program
        // (only on Windows, it is expected that a Linux system has its own headers for libc/libc++)
        // if (Platforms.isWindows()) {
        // systemIncludes.addAll(clangAstParser.prepareIncludes(clangExecutable, usePlatformLibc));
        systemIncludes.addAll(builtinIncludes);
        // }

        // Add custom includes
        systemIncludes.addAll(localData.get(LocalOptionsKeys.SYSTEM_INCLUDES).getStringList());

        // Add local system includes
        // for (String systemInclude : localData.get(LocalOptionsKeys.SYSTEM_INCLUDES)) {
        for (String systemInclude : systemIncludes) {
            arguments.add("-isystem");
            arguments.add(systemInclude);
        }

        arguments.addAll(ArgumentsParser.newCommandLine().parse(config.get(ClavaOptions.FLAGS)));
        arguments.addAll(config.get(ClavaOptions.FLAGS_LIST));

        ClavaLog.debug(() -> "Calling Clang AST Dumper: " + arguments.stream().collect(Collectors.joining(" ")));

        ClangParserData parsedData = null;
        ProcessOutput<String, ClangParserData> output = null;

        // ProcessOutputAsString output = SpecsSystem.runProcess(arguments, true, false);
        try (LineStreamParser<ClangParserData> lineStreamParser = ClangStreamParserV2
                .newInstance(config.get(ClavaNode.CONTEXT))) {

            if (SpecsSystem.isDebug()) {
                lineStreamParser.getData().set(ClangParserData.DEBUG, true);
            }

            // Create temporary working folder, in order to support running several dumps in parallel
            // File workingFolder = SpecsIo.mkdir(UUID.randomUUID().toString());
            lastWorkingFolder = SpecsIo.mkdir(baseFolder, sourceFile.getName() + "_" + id);

            // Ensure folder is empty
            SpecsIo.deleteFolderContents(lastWorkingFolder);

            workingFolders.add(lastWorkingFolder);

            output = SpecsSystem.runProcess(arguments, lastWorkingFolder,
                    // inputStream -> clangAstParser.processOutput(inputStream,
                    // new File(lastWorkingFolder, CLANG_DUMP_FILENAME)),
                    inputStream -> this.processOutput(sourceFile, inputStream),
                    // inputStream -> clangAstParser.processStdErr(config, inputStream, lineStreamParser,
                    // new File(lastWorkingFolder, STDERR_DUMP_FILENAME)));
                    inputStream -> this.processStdErr(inputStream, config.get(ClavaNode.CONTEXT)));

            // parsedData = lineStreamParser.getData();
            // parsedData = output.getStdErr() == null ? new ClangParserData() : output.getStdErr();
            // ClavaLog.debug("Process finished");
            // ClavaLog.debug("Stdout: '" + output.getStdOut() + "'");
            // ClavaLog.debug("Stderr: '" + output.getStdErr() + "'");
            // ClavaLog.debug("Return value: '" + output.getReturnValue() + "'");
            if (output.isError()) {
                ClavaLog.debug("Dumper returned an error value: '" + output.getReturnValue() + "'");
            }

            // If exception happened while processing output, throw exception
            output.getOutputException().ifPresent(exception -> {
                throw new RuntimeException("Exception while processing the output streams", exception);
            });

            parsedData = output.getStdErr();
            SpecsCheck.checkNotNull(parsedData, () -> "Did not expect error output to be null");
            parsedData.set(ClangParserData.HAS_ERRORS, output.isError());

            // If console output streaming is disabled, show output only at the end
            if (!streamConsoleOutput) {
                ClavaLog.info(output.getStdOut());
            }

            if (lineStreamParser.hasExceptions()) {
                SpecsLogs.warn("Exceptions happened while parsing the file '" + sourceFile.getAbsolutePath() + "'");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while running Clang AST dumper", e);
        }

        ClangStreamParser clangStreamParser = new ClangStreamParser(parsedData, SpecsSystem.isDebug(), config);

        TranslationUnit tUnit = clangStreamParser.parseTu(sourceFile);

        parsedData.set(ClangParserData.TRANSLATION_UNIT, tUnit);

        return parsedData;
        // App newApp = clangStreamParser.parse();
        //
        // return newApp.getTranslationUnits().get(0);

    }

    private String processOutput(File sourceFile, InputStream inputStream) {
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
            // SpecsLogs.msgWarn("Error while parsing output of Clang AST dumper", e);
            //
            // // Error output
            // ClangParserData errorData = new ClangParserData();
            // errorData.set(ClangParserData.HAS_ERRORS, true);
            // errorData.set(ClangParserData.LINES_NOT_PARSED,
            // "Error while parsing output of Clang AST dumper:\n" + e.toString());
            //
            // return errorData;

            throw new RuntimeException("Error while parsing output of Clang AST dumper", e);
        }

    }

    /*
    @Override
    public TranslationUnit parse(File file, DataStore config) {
    
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
        context.set(ClavaContext.APP, app);
    
        // Add text elements (comments, pragmas) to the tree
        new TextParser(app.getContext()).addElements(app);
    
        return app;
    }
    */
    /*
    private ClangRootNode parseSource(File sourceFile, DataStore config) {
    
        // Create instance of ClangAstParser
        ClangAstParser clangAstParser = new ClangAstParser(dumpStdOut, useCustomResources);
    
        DataStore localData = JOptionsUtils.loadDataStore(ClangAstParser.getLocalOptionsFile(), getClass(),
                LocalOptionsKeys.getProvider().getStoreDefinition());
    
        // Get version for the executable
        String version = config.get(ClangAstKeys.CLANGAST_VERSION);
    
        // Copy resources
        File clangExecutable = clangAstParser.prepareResources(version);
    
        List<String> arguments = new ArrayList<>();
        arguments.add(clangExecutable.getAbsolutePath());
    
        arguments.add(sourceFile.getAbsolutePath());
    
        int id = nextId();
        arguments.add("-id=" + id);
    
        arguments.add("--");
    
        // Add standard
        config.getTry(ClavaOptions.STANDARD).ifPresent(standard -> arguments.add(standard.getFlag()));
        // arguments.add(config.get(ClavaOptions.STANDARD).getFlag());
    
        List<String> systemIncludes = new ArrayList<>();
    
        // Add includes bundled with program
        // (only on Windows, it is expected that a Linux system has its own headers for libc/libc++)
        // if (Platforms.isWindows()) {
        systemIncludes.addAll(clangAstParser.prepareIncludes(clangExecutable));
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
    
            // Create temporary working folder, in order to support running several dumps in parallel
            // File workingFolder = SpecsIo.mkdir(UUID.randomUUID().toString());
            lastWorkingFolder = SpecsIo.mkdir(sourceFile.getName() + "_" + id);
            workingFolders.add(lastWorkingFolder);
    
            output = SpecsSystem.runProcess(arguments, lastWorkingFolder,
                    inputStream -> clangAstParser.processOutput(inputStream,
                            new File(lastWorkingFolder, CLANG_DUMP_FILENAME)),
                    inputStream -> clangAstParser.processStdErr(config, inputStream, lineStreamParser,
                            new File(lastWorkingFolder, STDERR_DUMP_FILENAME)));
    
            parsedData = lineStreamParser.getData();
        } catch (Exception e) {
            throw new RuntimeException("Error while running Clang AST dumper", e);
        }
    
        // Error output has information about types, separate this information from the warnings
        DataStore stderr = output.getStdErr();
    
        ClangStreamParser clangStreamParser = new ClangStreamParser(parsedData, SpecsSystem.isDebug());
        App newApp = clangStreamParser.parse();
    
        // Set app in context
        context.set(ClavaContext.APP, newApp);
    
        if (SpecsSystem.isDebug()) {
            // App newApp = new ClangStreamParser(stderr).parse();
            System.out.println("NEW APP CODE:\n" + newApp.getCode());
        }
    
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
        // ClangAstParser.checkInterleavedExecutions();
    
        // Get clang output
        // String clangOutput = output.getStdOut();
    
        // Always write to a file, to be able to check dump
        // IoUtils.write(new File("clangDump.txt"), clangOutput);
        SpecsIo.write(new File(lastWorkingFolder, "types.txt"), stderr.get(StreamKeys.TYPES));
        // System.out.println("OUTPUT:\n" + output.getOutput());
    
        // Parse Clang output
        // List<ClangNode> clangDump = new AstParser().parse(output.getStdOut());
        List<ClangNode> clangDump = output.getStdOut();
    
        // Add Source Ranges
        ClangAstParser.addSourceRanges(clangDump, stderr);
    
        // Parse Clang types
        List<ClangNode> clangTypes = ClangAstParser
                .removeDuplicates(new AstParser().parse(stderr.get(StreamKeys.TYPES)));
    
        ClangAstParser.addSourceRanges(clangTypes, stderr);
    
        // Parse node-types map
        // This file includes several repetitions and even addresses that will not appear
        // in the dump. However, all nodes in the dump (clangDump and types) should be
        // represented in the map.
        // Map<String, String> nodeToTypes = parseNodeToTypes(IoUtils.read(new File("nodetypes.txt")));
        Map<String, String> nodeToTypes = ClangAstParser
                .parseAddrToAddr(SpecsIo.read(new File(lastWorkingFolder, "nodetypes.txt")));
    
        // Remove top-level nodes that do not have a corresponding type
        // clangDump = cleanup(clangDump, nodeToTypes);
    
        // Make sure all nodes in clangDump have a corresponding type
        // check(clangDump, clangTypes, nodeToTypes);
    
        // Parse includes
        ClangIncludes includes = ClangIncludes.newInstance(SpecsIo.existingFile(lastWorkingFolder, "includes.txt"));
    
        // Get nodes that can have template arguments
        // Set<String> hasTemplateArguments = parseTemplateArguments(SpecsIo.read("template_args.txt"));
    
        // Get nodes that are temporary
        Set<String> isTemporary = ClangAstParser
                .parseIsTemporary(SpecsIo.read(new File(lastWorkingFolder, "is_temporary.txt")));
    
        // Get OpenMP directives
        // Map<String, OMPDirective> ompDirectives = parseOmpDirectives(IoUtils.read("omp.txt"));
        Map<String, OMPDirective> ompDirectives = new HashMap<>();
    
        // Get enum integer types
        Map<String, String> enumToIntegerType = ClangAstParser
                .parseEnumIntegerTypes(SpecsIo.read(new File(lastWorkingFolder, "enum_integer_type.txt")));
    
        // Check if no new nodes should be used
        // Map<String, ClavaNode> newNodes = disableNewParsingMethod ? new HashMap<>()
        // : lineStreamParser.getData().get(ClangParserKeys.CLAVA_NODES);
        ClavaNodes newNodes = parsedData.get(ClangParserData.CLAVA_NODES);
    
        ClangRootData clangRootData = new ClangRootData(config, includes, clangTypes, nodeToTypes,
                isTemporary, ompDirectives, enumToIntegerType, stderr,
                newNodes, clangDump);
    
        return new ClangRootNode(clangRootData, clangDump);
    }
    */
    /**
     * TODO: Current implementation only shows the last file, show all files
     */
    @Override
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

    // @Override
    // public ClangParser setUsePlatformLibc(boolean usePlatformLibc) {
    // this.usePlatformLibc = usePlatformLibc;
    //
    // return this;
    // }

}

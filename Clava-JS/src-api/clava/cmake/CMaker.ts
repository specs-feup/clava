laraImport("lara.cmake.CMakerUtils");
laraImport("lara.cmake.CMakerSources");
laraImport("lara.tool.Tool");
laraImport("lara.tool.ToolUtils");

laraImport("lara.Strings");
laraImport("lara.Platforms");
laraImport("lara.Compilation");
laraImport("lara.util.StringSet");
laraImport("lara.util.ProcessExecutor");

/**
 * Builds CMake configurations.
 */
class CMaker extends Tool {
    name;
    //disableWeaving;
    makeCommand;
    generator;
    minimumVersion;
    cxxFlags;
    cFlags;
    libs;
    sources;
    includeFolders;
    printToConsole;
    _lastMakeOutput;
    compilerined;

    customCMakeCode;

    _MINIMUM_VERSION = "3.10";
    _EXE_VAR = "EXE_NAME";

    static _DEFAULT_BIN_FOLDER = "bin";

    constructor(name, disableWeaving) {
        super("CMAKER", disableWeaving);

        //checkDefined(name, "name", "CMaker");
        if (name === undefined) {
            name = "cmaker-project";
        }

        this.name = name;
        this.makeCommand = "make";
        this.generator = undefined;
        this.minimumVersion = this._MINIMUM_VERSION;
        this.cxxFlags = [];
        this.cFlags = [];
        this.libs = [];
        this.sources = new CMakerSources(this.disableWeaving);
        this.includeFolders = new StringSet();
        this.printToConsole = false;
        this._lastMakeOutput = undefined;
        this.compiler = undefined;

        this.customCMakeCode = undefined;
    }

    /**
     * Custom CMake code that will be appended to the end of the CMake file.
     *
     * @param {string} customCMakeCode - The code to append at the end of the CMake file.
     */
    setCustomCMakeCode(customCMakeCode) {
        this.customCMakeCode = customCMakeCode;

        return this;
    }

    copy() {
        var newCmaker = new CMaker(this.name, this.disableWeaving);

        newCmaker.makeCommand = this.makeCommand;
        newCmaker.generator = this.generator;
        newCmaker.minimumVersion = this.minimumVersion;
        newCmaker.cxxFlags = this.cxxFlags.slice();
        newCmaker.cFlags = this.cFlags.slice();
        newCmaker.libs = this.libs.slice();
        newCmaker.sources = this.sources.copy();
        newCmaker.includeFolders = this.includeFolders.copy();
        newCmaker.printToConsole = this.printToConsole;
        newCmaker._lastMakeOutput = this._lastMakeOutput;
        newCmaker.compiler = this.compiler;

        return newCmaker;
    }

    /**
     * @returns {lara.cmake.CMakerSources} Object that can be used to specify the sources.
     */
    getSources() {
        return this.sources;
    }

    /**
     * Sets the minimum version of the CMake file.
     *
     * @param version String with minimum CMake version
     */
    setMinimumVersion(version) {
        this.minimumVersion = version;

        return this;
    }

    /**
     * Sets the name of the executable.
     *
     * @param name String with the name of the executable.
     */
    setName(name) {
        this.name = name;

        return this;
    }

    /**
     * @param {string|lara.cmake.compilers.CMakeCompiler}
     */
    setCompiler(compiler) {
        if (isString(compiler)) {
            this.compiler = CMakerUtils.getCompiler(compiler);
        } else {
            this.compiler = compiler;
        }

        return this;
    }

    /**
     * Sets if output of external tools (e.g., cmake, make) should appear in the console. By default it is off.
     */
    setPrintToolsOutput(printToolsOutput) {
        this.printToConsole = printToolsOutput;

        return this;
    }

    /**
     * @param {String} generator
     */
    setGenerator(generator) {
        this.generator = generator;

        return this;
    }

    setMakeCommand(makeCommand) {
        this.makeCommand = makeCommand;

        return this;
    }

    /**
     * Adds a variable number of Strings, one for each flag.
     *
     */
    addCxxFlags() {
        var flags = arrayFromArgs(arguments);

        for (var flag of flags) {
            this.cxxFlags.push(flag);
        }

        return this;
    }

    /**
     * Adds a variable number of Strings, one for each flag.
     *
     */
    addCFlags() {
        var flags = arrayFromArgs(arguments);

        for (var flag of flags) {
            this.cFlags.push(flag);
        }

        return this;
    }

    addFlags() {
        var flags = arrayFromArgs(arguments);
        this.addCFlags(flags);
        this.addCxxFlags(flags);

        return this;
    }

    /**
     * Adds link-time libraries (e.g., m for math.h).
     *
     * @param {String...|String[]} arguments - a sequence of String with the name of the link-time libraries, as CMake would accept (e.g., "m").
     */
    addLibs() {
        var libs = arrayFromArgs(arguments);

        //for(var lib of arguments) {
        for (var lib of libs) {
            this.libs.push(lib);
        }

        return this;
    }

    getMakeOutput() {
        if (this._lastMakeOutput === undefined) {
            println("CMaker.getMakeOutput: there is not make output yet");
            return undefined;
        }

        return this._lastMakeOutput.getConsoleOutput();
    }


    /**
     * param includeFolder String representing an include folder
     */
    addIncludeFolder(includeFolder) {
        var parsedFolder = ToolUtils.parsePath(includeFolder);
        this.includeFolders.add(parsedFolder);
        //this.includeFolders.add(Strings.escapeJson(includeFolder));

        return this;
    }

    addCurrentAst() {
        for (var userInclude of Clava.getData().getUserIncludes()) {
            println("[" + this.toolName + "] Adding include: " + userInclude);
            this.addIncludeFolder(userInclude);
        }

        // Write current version of the files to a temporary folder and add them
        var currentAstFolder = Io.getPath(Io.getTempFolder(), "tempfolder_current_ast");

        // Clean folder
        Io.deleteFolderContents(currentAstFolder);

        // Create and populate source folder
        var srcFolder = Io.getPath(currentAstFolder, "src");
        for (var $file of Clava.getProgram().getDescendants("file")) {
            var destFolder = srcFolder;
            //if($file.relativeFolderpath !== undefined) {
            //	destFolder = Io.mkdir(srcFolder, $file.relativeFolderpath);
            //}
            var filepath = $file.write(destFolder.toString());

            if (!$file.isHeader) {
                this.getSources().addSource(filepath);
            }
            //println("Written file:" + filepath);
        }

        // Add src folder as include
        this.addIncludeFolder(srcFolder);

        return this;
    }

    /**
     * @return the name of the executable that will be generated
     */
    _getExecutableName() {
        var executable = this.name;

        if (Platforms.isWindows()) {
            executable += ".exe";
        }

        return executable;
    }

    /**
     * Builds the program currently defined in the CMaker object.
     *
     * @param {String|J#java.io.File} [cmakelistsFolder=<temporary folder>] - The folder where the CMakeList files will be written
     * @param {String|J#java.io.File} [builderFolder=<cmakelistsFolder>/build] - The folder where the program will be built
     * @param {String} cmakeFlags - Additional flags that will be passed to CMake execution
     *
     * @returns {J#java.io.File} file to the executable compiled by the build.
     */
    build(cmakelistsFolder, builderFolder, cmakeFlags) {
        if (cmakelistsFolder === undefined) {
            cmakelistsFolder = Io.newRandomFolder();
        }

        // Create build folder
        if (builderFolder === undefined) {
            builderFolder = Io.getPath(cmakelistsFolder, "build");
        }

        // Generate CMakeLists.txt
        var cmakeFile = Io.getPath(cmakelistsFolder, "CMakeLists.txt");
        Io.writeFile(cmakeFile, this.getCode());

        var builderFolderpath = Io.mkdir(builderFolder).getAbsolutePath();

        // Execute CMake
        var cmakeCmd =
            'cmake "' + cmakeFile.getParentFile().getAbsolutePath() + '"';
        //var cmakeCmd = "cmake \"" + cmakelistsFolder + "\"";
        if (cmakeFlags !== undefined) {
            cmakeCmd += " " + cmakeFlags;
        }

        if (this.generator !== undefined) {
            cmakeCmd += ' -G "' + this.generator + '"';
        }

        if (this.compiler !== undefined) {
            cmakeCmd += " " + this.compiler.getCommandArgs();
        }

        debug(
            "Executing CMake, calling '" +
            cmakeCmd +
            "' at ' " +
            builderFolderpath +
            " '"
        );
        var cmakeOutput = new ProcessExecutor();

        cmakeOutput
            .setPrintToConsole(this.printToConsole)
            .setWorkingDir(builderFolderpath)
            .execute(cmakeCmd);

        if (cmakeOutput.getReturnValue() === 0) {
            debug("CMake output:");
            debug(cmakeOutput.getConsoleOutput());
        } else {
            println(
                "Cmaker.build: Could not generate makefile\n" +
                cmakeOutput.getConsoleOutput()
            );
            return;
        }

        //println("CMake output: " + cmakeOutput);

        // Execute make
        debug(
            "Building, calling '" +
            this.makeCommand +
            "' at ' " +
            builderFolderpath +
            " '"
        );
        this._lastMakeOutput = new ProcessExecutor();
        this._lastMakeOutput
            .setPrintToConsole(this.printToConsole)
            .setWorkingDir(builderFolderpath)
            .execute(this.makeCommand);
        debug("Make output:");
        debugObject(this._lastMakeOutput.getConsoleOutput());
        //println("Make output: " + makeOutput);

        var binPath = Io.getPath(builderFolderpath, CMaker._DEFAULT_BIN_FOLDER);
        var executableFile = Io.getPath(binPath, this._getExecutableName());
        //println("BuildFolderPath:" + builderFolderpath);
        //println("Executable Name:" + this._getExecutableName());
        if (!Io.isFile(executableFile)) {
            println(
                "Cmaker.build: Could not generate executable file '" +
                this._getExecutableName() +
                "', expected to be in the path '" +
                executableFile.getAbsolutePath() +
                "'"
            );
            return;
        }

        return executableFile;
    }

    /*** CODE FUNCTIONS ***/

    /**
     * @return the CMake corresponding to the current configuration
     */
    getCode() {
        var code = "";

        // Minimum version
        code += "cmake_minimum_required(VERSION " + this.minimumVersion + ")\n";

        // Project
        code += "project(" + this.name + ")\n\n";

        // Executable
        code += "set (" + this._EXE_VAR + ' "' + this.name + '")\n\n';

        // Flags
        code += this._getCxxFlagsCode();
        code += this._getCFlagsCode();

        // Directories
        code += this._getProjectDirectoriesCode();

        // Sources
        code += this.sources.getCode() + "\n\n";

        // Include folders
        code += this._getIncludeFoldersCode();

        // Executable
        code += "add_executable(${" + this._EXE_VAR + "} ";
        code += "${" + this.sources.getSourceVariables().join("} ${") + "}";
        code += ")\n\n";

        // Libs
        var extraLibs = Compilation.getExtraLibs();
        this.addLibs(extraLibs);
        code += "target_link_libraries(${" + this._EXE_VAR + "} ";
        code += '"' + this.libs.join('" "') + '")\n\n';

        // binary directories
        code += `
		set_target_properties(\${EXE_NAME}
			PROPERTIES
			ARCHIVE_OUTPUT_DIRECTORY "\${CMAKE_BINARY_DIR}/${CMaker._DEFAULT_BIN_FOLDER}"
			LIBRARY_OUTPUT_DIRECTORY "\${CMAKE_BINARY_DIR}/${CMaker._DEFAULT_BIN_FOLDER}"
			RUNTIME_OUTPUT_DIRECTORY "\${CMAKE_BINARY_DIR}/${CMaker._DEFAULT_BIN_FOLDER}"
		)
	`;

        // Custom code
        if (this.customCMakeCode !== undefined) {
            code += this.customCMakeCode;
        }

        return code;
    }

    _getProjectDirectoriesCode() {
        var code = "";
        var subDirs = Compilation.getExtraProjects();

        for (var subDir of subDirs) {
            code += 'add_subdirectory("' + subDir + '" "' + subDir + '/bin")\n';
        }

        return code;
    }

    _getCxxFlagsCode() {
        if (this.cxxFlags.length === 0) {
            return "";
        }

        var code = 'set (CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} ';

        code += this.cxxFlags.join(" ");
        code += '")\n\n';

        return code;
    }

    _getCFlagsCode() {
        if (this.cFlags.length === 0) {
            return "";
        }

        var code = 'set (CMAKE_C_FLAGS "${CMAKE_C_FLAGS} ';

        code += this.cFlags.join(" ");
        code += '")\n\n';

        return code;
    }

    _getIncludeFoldersCode() {
        // Add user includes
        var includes = [].concat(this.includeFolders.values());

        // Add external includes if weaving is not disabled
        if (!this.disableWeaving) {
            this._addExtraIncludes(includes);
        }

        /*
        var extraIncludes = Compilation.getExtraIncludes();
    	
        for(var extraInclude of extraIncludes) {
            if(Io.isFolder(extraInclude)) {
                debug("[CMAKER] Adding external include '" + extraInclude + "'");
                includes.push(CMakerUtils.parsePath(extraInclude));
            } else {
                println("[CMAKER] Extra include ' " + extraInclude +  " ' is not a folder");
            }
        }
        */

        //if(this.includeFolders.isEmpty()) {
        if (includes.length === 0) {
            return "";
        }

        var code = "include_directories(";

        //code += '"' + this.includeFolders.values().join("\" \"") + '"';
        code += '"' + includes.join('" "') + '"';

        code += ")\n\n";
        return code;
        //return "include_directories(<absolute path to srcFolder>)";
    }

    _addExtraIncludes(includes) {
        var extraIncludes = Compilation.getExtraIncludes();

        for (var extraInclude of extraIncludes) {
            if (Io.isFolder(extraInclude)) {
                debug("[CMAKER] Adding external include '" + extraInclude + "'");
                includes.push(ToolUtils.parsePath(extraInclude));
            } else {
                println(
                    "[CMAKER] Extra include ' " + extraInclude + " ' is not a folder"
                );
            }
        }
    }
}
/*
.setMinimumVersion("3.0.2") // Could have a standard minimum version
    .addSources(Io.getPaths(srcFolder, "*.cpp"))
    .addCxxFlags("-O3", "-std=c++11")
    .addLibs("stdc++")
    .addIncludes(srcFolder);
    .setVariable(name, value)
    cmaker.getCMake()
    cmaker.build(Io.getPath(srcFolder, "build")); 
	
    .addTaggedSources("tag", sources)
    */

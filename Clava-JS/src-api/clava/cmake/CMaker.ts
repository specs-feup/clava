import Io from "lara-js/api/lara/Io.js";
import Platforms from "lara-js/api/lara/Platforms.js";
import {
  arrayFromArgs,
  debug,
  debugObject,
} from "lara-js/api/lara/core/LaraCore.js";
import { JavaClasses } from "lara-js/api/lara/util/JavaTypes.js";
import ProcessExecutor from "lara-js/api/lara/util/ProcessExecutor.js";
import { FileJp } from "../../Joinpoints.js";
import Clava from "../Clava.js";
import CMakerSources from "./CMakerSources.js";
import CMakerUtils from "./CMakerUtils.js";
import CMakeCompiler from "./compilers/CMakeCompiler.js";
import BenchmarkCompilationEngine from "lara-js/api/lara/benchmark/BenchmarkCompilationEngine.js";

/**
 * Builds CMake configurations.
 *
 * @example
 * new Cmaker()
 *   .setMinimumVersion("3.0.2") // Could have a standard minimum version
 *   .addSources(Io.getPaths(srcFolder, "*.cpp"))
 *   .addCxxFlags("-O3", "-std=c++11")
 *   .addLibs("stdc++")
 *   .addIncludes(srcFolder);
 *   .setVariable(name, value)
 * cmaker.getCMake()
 * cmaker.build(Io.getPath(srcFolder, "build"));
 */
export default class CMaker extends BenchmarkCompilationEngine {
  private static MINIMUM_VERSION: string = "3.10";
  private static EXE_VAR: string = "EXE_NAME";
  private static DEFAULT_BIN_FOLDER: string = "bin";

  makeCommand: string = "make";
  generator: string | undefined = undefined;
  minimumVersion = CMaker.MINIMUM_VERSION;
  cxxFlags: string[] = [];
  cFlags: string[] = [];
  libs: string[] = [];
  sources: CMakerSources;
  includeFolders: Set<string> = new Set();
  printToConsole: boolean = false;
  lastMakeOutput: ProcessExecutor | undefined = undefined;
  compiler: CMakeCompiler | undefined = undefined;

  customCMakeCode: string | undefined = undefined;

  constructor(
    name: string = "cmaker-project",
    disableWeaving: boolean = false
  ) {
    super(name, disableWeaving);

    this.sources = new CMakerSources(this.disableWeaving);
  }

  /**
   * Custom CMake code that will be appended to the end of the CMake file.
   *
   * @param customCMakeCode - The code to append at the end of the CMake file.
   */
  setCustomCMakeCode(customCMakeCode: string) {
    this.customCMakeCode = customCMakeCode;

    return this;
  }

  copy() {
    const newCmaker = new CMaker(this.toolName, this.disableWeaving);

    newCmaker.makeCommand = this.makeCommand;
    newCmaker.generator = this.generator;
    newCmaker.minimumVersion = this.minimumVersion;
    newCmaker.cxxFlags = this.cxxFlags.slice();
    newCmaker.cFlags = this.cFlags.slice();
    newCmaker.libs = this.libs.slice();
    newCmaker.sources = this.sources.copy();
    newCmaker.includeFolders = structuredClone(this.includeFolders);
    newCmaker.printToConsole = this.printToConsole;
    newCmaker.lastMakeOutput = this.lastMakeOutput;
    newCmaker.compiler = this.compiler;

    return newCmaker;
  }

  /**
   * @returns Object that can be used to specify the sources.
   */
  getSources(): CMakerSources {
    return this.sources;
  }

  /**
   * Sets the minimum version of the CMake file.
   *
   * @param version - String with minimum CMake version
   */
  setMinimumVersion(version: string) {
    this.minimumVersion = version;

    return this;
  }

  /**
   * Sets the name of the executable.
   *
   * @param name - String with the name of the executable.
   */
  setName(name: string) {
    this.toolName = name;

    return this;
  }

  setCompiler(
    compiler: Parameters<typeof CMakerUtils.getCompiler>[0] | CMakeCompiler
  ) {
    if (typeof compiler === "string") {
      this.compiler = CMakerUtils.getCompiler(compiler);
    } else {
      this.compiler = compiler;
    }

    return this;
  }

  /**
   * Sets if output of external tools (e.g., cmake, make) should appear in the console. By default it is off.
   */
  setPrintToolsOutput(printToolsOutput: boolean = false) {
    this.printToConsole = printToolsOutput;

    return this;
  }

  setGenerator(generator: string) {
    this.generator = generator;

    return this;
  }

  setMakeCommand(makeCommand: string) {
    this.makeCommand = makeCommand;

    return this;
  }

  /**
   * Adds a variable number of Strings, one for each flag.
   *
   */
  addCxxFlags(...args: string[]) {
    const flags = arrayFromArgs(args) as string[];

    for (const flag of flags) {
      this.cxxFlags.push(flag);
    }

    return this;
  }

  /**
   * Adds a variable number of Strings, one for each flag.
   *
   */
  addCFlags(...args: string[]) {
    const flags = arrayFromArgs(args) as string[];

    for (const flag of flags) {
      this.cFlags.push(flag);
    }

    return this;
  }

  addFlags(...args: string[]) {
    const flags = arrayFromArgs(args) as string[];
    this.addCFlags(...flags);
    this.addCxxFlags(...flags);

    return this;
  }

  /**
   * Adds link-time libraries (e.g., m for math.h).
   *
   * @param arguments - a sequence of String with the name of the link-time libraries, as CMake would accept (e.g., "m").
   */
  addLibs(...args: string[]) {
    const libs = arrayFromArgs(args) as string[];

    this.libs.push(...libs);

    return this;
  }

  getMakeOutput(): string | undefined {
    if (this.lastMakeOutput === undefined) {
      console.log("CMaker.getMakeOutput: there is not make output yet");
      return undefined;
    }

    return this.lastMakeOutput.getConsoleOutput();
  }

  /**
   * @param includeFolder - String representing an include folder
   */
  addIncludeFolder(includeFolder: string | JavaClasses.File) {
    const parsedFolder = CMakerUtils.parsePath(includeFolder);
    this.includeFolders.add(parsedFolder);

    return this;
  }

  addCurrentAst() {
    for (const userInclude of Clava.getData().getUserIncludes()) {
      console.log("[" + this.toolName + "] Adding include: " + userInclude);
      this.addIncludeFolder(userInclude);
    }

    // Write current version of the files to a temporary folder and add them
    const currentAstFolder = Io.getPath(
      Io.getTempFolder(),
      "tempfolder_current_ast"
    );

    // Clean folder
    Io.deleteFolderContents(currentAstFolder);

    // Create and populate source folder
    const srcFolder = Io.getPath(currentAstFolder, "src");
    for (const $jp of Clava.getProgram().getDescendants("file")) {
      const $file = $jp as FileJp;

      const destFolder = srcFolder;
      const filepath = $file.write(destFolder.toString());

      if (!$file.isHeader) {
        this.getSources().addSource(filepath);
      }
    }

    // Add src folder as include
    this.addIncludeFolder(srcFolder);

    return this;
  }

  /**
   * @returns The name of the executable that will be generated
   */
  private getExecutableName() {
    let executable = this.toolName;

    if (Platforms.isWindows()) {
      executable += ".exe";
    }

    return executable;
  }

  /**
   * Builds the program currently defined in the CMaker object.
   *
   * @param cmakelistsFolder - The folder where the CMakeList files will be written
   * @param builderFolder - The folder where the program will be built
   * @param cmakeFlags - Additional flags that will be passed to CMake execution
   *
   * @returns File to the executable compiled by the build.
   */
  build(
    cmakelistsFolder: string | JavaClasses.File = Io.newRandomFolder(),
    builderFolder: string | JavaClasses.File = Io.getPath(
      cmakelistsFolder,
      "build"
    ),
    cmakeFlags?: string
  ): JavaClasses.File | undefined {
    // Generate CMakeLists.txt
    const cmakeFile = Io.getPath(cmakelistsFolder, "CMakeLists.txt");
    Io.writeFile(cmakeFile, this.getCode());

    const builderFolderpath = Io.mkdir(builderFolder).getAbsolutePath();

    // Execute CMake
    let cmakeCmd =
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
    const cmakeOutput = new ProcessExecutor();

    cmakeOutput
      .setPrintToConsole(this.printToConsole)
      .setWorkingDir(builderFolderpath)
      .execute(cmakeCmd);

    const consoleOutput = cmakeOutput.getConsoleOutput();
    if (cmakeOutput.getReturnValue() === 0 && consoleOutput != undefined) {
      debug("CMake output:");
      debug(consoleOutput);
    } else {
      console.log(
        "Cmaker.build: Could not generate makefile\n" + consoleOutput
      );
      return;
    }

    // Execute make
    debug(
      `Building, calling '${this.makeCommand}' at ' ${builderFolderpath} '`
    );
    this.lastMakeOutput = new ProcessExecutor();
    this.lastMakeOutput
      .setPrintToConsole(this.printToConsole)
      .setWorkingDir(builderFolderpath)
      .execute(this.makeCommand);
    debug("Make output:");
    debugObject(this.lastMakeOutput.getConsoleOutput());

    const binPath = Io.getPath(builderFolderpath, CMaker.DEFAULT_BIN_FOLDER);
    const executableFile = Io.getPath(binPath, this.getExecutableName());

    if (!Io.isFile(executableFile)) {
      console.log(
        `Cmaker.build: Could not generate executable file '${this.getExecutableName()}', expected to be in the path '${executableFile.getAbsolutePath()}'`
      );
      return;
    }

    return executableFile;
  }

  /*** CODE FUNCTIONS ***/

  /**
   * @returns The CMake corresponding to the current configuration
   */
  getCode() {
    let code = "";

    // Minimum version
    code += "cmake_minimum_required(VERSION " + this.minimumVersion + ")\n";

    // Project
    code += "project(" + this.toolName + ")\n\n";

    // Executable
    code += "set (" + CMaker.EXE_VAR + ' "' + this.toolName + '")\n\n';

    // Flags
    code += this.getCxxFlagsCode();
    code += this.getCFlagsCode();

    // Directories
    code += this.getProjectDirectoriesCode();

    // Sources
    code += this.sources.getCode() + "\n\n";

    // Include folders
    code += this.getIncludeFoldersCode();

    // Executable
    code += "add_executable(${" + CMaker.EXE_VAR + "} ";
    code += "${" + this.sources.getSourceVariables().join("} ${") + "}";
    code += ")\n\n";

    // Libs
    this.addLibs(...Clava.getProgram().extraLibs);
    code += "target_link_libraries(${" + CMaker.EXE_VAR + "} ";
    code += '"' + this.libs.join('" "') + '")\n\n';

    // binary directories
    code += `
        set_target_properties(\${EXE_NAME}
            PROPERTIES
            ARCHIVE_OUTPUT_DIRECTORY "\${CMAKE_BINARY_DIR}/${CMaker.DEFAULT_BIN_FOLDER}"
            LIBRARY_OUTPUT_DIRECTORY "\${CMAKE_BINARY_DIR}/${CMaker.DEFAULT_BIN_FOLDER}"
            RUNTIME_OUTPUT_DIRECTORY "\${CMAKE_BINARY_DIR}/${CMaker.DEFAULT_BIN_FOLDER}"
        )
    `;

    // Custom code
    if (this.customCMakeCode !== undefined) {
      code += this.customCMakeCode;
    }

    return code;
  }

  private getProjectDirectoriesCode() {
    const subDirs = Clava.getProgram().extraProjects;

    return subDirs.reduce((acc, subDir) => {
      return acc + `add_subdirectory("${subDir}" "${subDir}/bin")\n`;
    }, "");
  }

  private getCxxFlagsCode() {
    if (this.cxxFlags.length === 0) {
      return "";
    }

    return `set (CMAKE_CXX_FLAGS "\${CMAKE_CXX_FLAGS} ${this.cxxFlags.join(
      " "
    )}")\n\n`;
  }

  private getCFlagsCode() {
    if (this.cFlags.length === 0) {
      return "";
    }

    return `set (CMAKE_C_FLAGS "\${CMAKE_C_FLAGS} ${this.cFlags.join(
      " "
    )}")\n\n`;
  }

  private getIncludeFoldersCode() {
    // Add user includes
    const includes = Array.from(this.includeFolders.values());

    // Add external includes if weaving is not disabled
    if (!this.disableWeaving) {
      this.addExtraIncludes(includes);
    }

    if (includes.length === 0) {
      return "";
    }

    return `include_directories("${includes.join('" "')}")\n\n`;
  }

  private addExtraIncludes(includes: string[]) {
    const extraIncludes = Clava.getProgram().extraIncludes;

    for (const extraInclude of extraIncludes) {
      if (Io.isFolder(extraInclude)) {
        debug(`[CMAKER] Adding external include '${extraInclude}'`);
        includes.push(CMakerUtils.parsePath(extraInclude));
      } else {
        console.log(
          `[CMAKER] Extra include ' ${extraInclude} ' is not a folder`
        );
      }
    }
  }
}

laraImport("lara.util.StringSet");
laraImport("lara.Io");
laraImport("lara.Strings");
laraImport("lara.util.ProcessExecutor");
laraImport("weaver.Query");

/**
 * @class
 */
class HeaderEnums {
  /**
   * 
   * @param {string} headerFilename 
   * @param {ClangEnum[]} clangEnums 
   */
  constructor(headerFilename, clangEnums) {
    this.headerFilename = headerFilename;
    this.enumMap = {};
    this.currentEnumSet = new StringSet();
    this.enumsInsideClass = new StringSet();

    for (const clangEnum of clangEnums) {
      this.enumMap[clangEnum.getCompleteEnumName()] = clangEnum;
      this.currentEnumSet.add(clangEnum.getCompleteEnumName());

      const className = clangEnum.getClassName();
      if (className !== undefined) {
        this.enumsInsideClass.add(clangEnum.getEnumName());
      }
    }
  }

  getName() {
    return this.headerFilename;
  }

  /**
   * 
   * @param {ClangEnum} $enum 
   * @returns 
   */
  _getEnumName($enum) {
    // Check if enum needs to be prefixed by class
    if (!this.enumsInsideClass.has($enum.name)) {
      return $enum.name;
    }

    // Get class
    const $class = $enum.ancestor("class");

    return `${$class.name}_${$enum.name}`;
  }

  /**
   * 
   * @param {string} llvmFolder 
   */
  process(llvmFolder) {
    const compilerCmd = "clang";
    const headerFile = Io.getPath(llvmFolder, this.headerFilename);

    // Preprocess header
    const exec = new ProcessExecutor();
    // Clean folder and set working directory

    exec.setPrintToConsole(false);
    const preprocessedFile = exec.execute(
      compilerCmd,
      "-E",
      headerFile.getAbsolutePath()
    );

    debug(
      "Processed header '" +
        headerFile.getAbsolutePath() +
        "':\n" +
        preprocessedFile
    );

    const headerLines = Strings.asLines(preprocessedFile);

    for (const enumName in this.enumMap) {
      const clangEnum = this.enumMap[enumName];

      // Extract enums
      clangEnum.setEnumValues(headerLines);
    }
  }

  /**
   * 
   * @param {string} outputFolder 
   */
  generateCode(outputFolder) {
    const headerFile = Io.getPath(this.headerFilename);

    const filename = "enums_" + headerFile.getName().replace(".", "_") + ".cpp";

    let code = '#include "ClangEnums.h"\n\n';

    // Generate code for each of the enums
    for (const enumName in this.enumMap) {
      const enumCode = this.enumMap[enumName].getCode();

      if (enumCode === undefined) {
        println(
          "Skipped code generation for " + this.headerFilename + "::" + enumName
        );
        continue;
      }

      code += enumCode + "\n";
    }

    const outputFile = Io.getPath(outputFolder, filename);
    println("Generating header file '" + outputFile.getAbsolutePath() + "'");
    Io.writeFile(Io.getPath(outputFolder, filename), code);
  }

  generateJavaEnums(outputFolder) {
    const headerFile = Io.getPath(this.headerFilename);

    // Dump the names of each enum
    for (const enumName in this.enumMap) {
      const enumValues = this.enumMap[enumName].getEnumValues();

      const filename =
        "enums_" +
        headerFile.getName().replace(".", "_") +
        "_" +
        enumName +
        ".txt";

      const code = enumValues.join(",\n") + ";";

      const outputFile = Io.getPath(outputFolder, filename);
      println(
        "Generating Java enum values file '" +
          outputFile.getAbsolutePath() +
          "'"
      );
      Io.writeFile(Io.getPath(outputFolder, filename), code);
    }
  }
}

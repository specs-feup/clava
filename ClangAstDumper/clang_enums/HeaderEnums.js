laraImport("lara.util.StringSet");
laraImport("lara.Io");
laraImport("lara.Strings");
laraImport("lara.util.ProcessExecutor");
laraImport("weaver.Query");

/**
 * @class
 */
class HeaderEnums {
  constructor(headerFilename, clangEnums) {
    this.headerFilename = headerFilename;
    this.enumMap = {};
    this.currentEnumSet = new StringSet();
    this.enumsInsideClass = new StringSet();

    for (var clangEnum of clangEnums) {
      this.enumMap[clangEnum.getCompleteEnumName()] = clangEnum;
      this.currentEnumSet.add(clangEnum.getCompleteEnumName());

      var className = clangEnum.getClassName();
      if (className !== undefined) {
        this.enumsInsideClass.add(clangEnum.getEnumName());
      }
    }
  }

  getName() {
    return this.headerFilename;
  }

  _getEnumName($enum) {
    // Check if enum needs to be prefixed by class
    if (!this.enumsInsideClass.has($enum.name)) {
      return $enum.name;
    }

    // Get class
    var $class = $enum.ancestor("class");

    return $class.name + "_" + $enum.name;
  }

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

    //println("Header: " + headerLines);

    for (const enumName in this.enumMap) {
      const clangEnum = this.enumMap[enumName];
      //println("Enum name: " + enumName);
      //printlnObject(clangEnum);

      // Extract enums
      //const enumValues = clangEnum.extractor(enumName, headerLines);
      //println("Values: " + enumValues);
      clangEnum.setEnumValues(headerLines);
      /*
      var filename =
        this.headerFilename.replace(".", "_") + "_" + enumName + ".txt";
      var file = Io.getPath(filename);
      println("Saving enum values to file '" + file.getAbsolutePath() + "'");
      Io.writeFile(filename, enumValues.join("\n"));
	  */
    }
    //println("Enums:");
    //printlnObject(this.enumMap);
    /*
    // Get enum name
    var enumName = this._getEnumName($enum);

    // Check if valid enum
    //if(!this.currentEnumSet.has($enum.name)) {
    if (!this.currentEnumSet.has(enumName)) {
      return;
    }

    //println("Adding enum '"+$enum.name+"'");
    println("Adding enum '" + enumName + "'");

    // Set enums
    var enumValues = $enum.enumerators.map(($enumValue) => $enumValue.name);
    //this.enumMap[$enum.name].setEnumValues(enumValues);
    this.enumMap[enumName].setEnumValues(enumValues);

    // Save enums, for referece
    //var filename = this.headerFilename.replace(".", "_") + "_" + $enum.name +  ".txt";
    var filename =
      this.headerFilename.replace(".", "_") + "_" + enumName + ".txt";
    var file = Io.getPath(filename);
    println("Saving enum values to file '" + file.getAbsolutePath() + "'");
    Io.writeFile(filename, enumValues.join("\n"));
	*/
  }

  generateCode(outputFolder) {
    const headerFile = Io.getPath(this.headerFilename);

    //var filename = "enums_" + this.headerFilename.replace(".", "_") + ".cpp";
    var filename = "enums_" + headerFile.getName().replace(".", "_") + ".cpp";

    var code = '#include "ClangEnums.h"\n\n';

    // Generate code for each of the enums
    for (var enumName in this.enumMap) {
      var enumCode = this.enumMap[enumName].getCode();

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
    for (var enumName in this.enumMap) {
      var enumValues = this.enumMap[enumName].getEnumValues();

      var filename =
        "enums_" +
        headerFile.getName().replace(".", "_") +
        "_" +
        enumName +
        ".txt";

      var code = enumValues.join(",\n") + ";";

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

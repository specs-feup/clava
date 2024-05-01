import fs from "fs";
import path from "path";
import { spawnSync } from "child_process";

/**
 * @class
 */
export default class HeaderEnums {
  /**
   *
   * @param {string} headerFilename
   * @param {ClangEnum[]} clangEnums
   */
  constructor(headerFilename, clangEnums) {
    this.headerFilename = headerFilename;
    this.enumMap = {};
    this.currentEnumSet = new Set();
    this.enumsInsideClass = new Set();

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
   * @param {string} llvmFolder
   */
  process(compilerCmd, llvmFolder) {
    const headerFile = path.join(llvmFolder, this.headerFilename);

    // Clean folder and set working directory
    const result = spawnSync(
      compilerCmd,
      [
        "-E",
        path.resolve(headerFile),
        "-isystem",
        path.join(llvmFolder, "../../../include"),
      ],
      {
        stdio: ["ignore", "pipe", "ignore"],
        maxBuffer: 1024 * 1024 * 1024,
      }
    );

    if (result.error) {
      throw new Error(
        "Could not process header '" +
          path.resolve(headerFile) +
          "': " +
          result.error
      );
    }

    const { stdout } = result;

    // Get stdout from spawn without writing to the console
    let preprocessedFile = String(stdout);

    console.debug("Processed header '" + path.resolve(headerFile) + "':");

    const headerLines = preprocessedFile.split("\n");

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
    const headerFile = path.basename(this.headerFilename);

    const filename = "enums_" + headerFile.replace(".", "_") + ".cpp";

    let code = '#include "ClangEnums.h"\n\n';

    // Generate code for each of the enums
    for (const enumName in this.enumMap) {
      const enumCode = this.enumMap[enumName].getCode();

      if (enumCode === undefined) {
        console.log(
          "Skipped code generation for " + this.headerFilename + "::" + enumName
        );
        continue;
      }

      code += enumCode + "\n";
    }

    const outputFile = path.join(outputFolder, filename);
    console.log("Generating header file '" + path.resolve(outputFile) + "'");
    fs.writeFileSync(path.join(outputFolder, filename), code);
  }

  generateJavaEnums(outputFolder) {
    const headerFile = path.basename(this.headerFilename);

    // Dump the names of each enum
    for (const enumName in this.enumMap) {
      const enumValues = this.enumMap[enumName].getEnumValues();

      const filename =
        "enums_" +
        headerFile.replace(".", "_") +
        "_" +
        enumName +
        ".txt";

      const code = enumValues.join(",\n") + ";";

      const outputFile = path.join(outputFolder, filename);
      console.log(
        "Generating Java enum values file '" + path.resolve(outputFile) + "'"
      );
      fs.writeFileSync(path.join(outputFolder, filename), code);
    }
  }
}

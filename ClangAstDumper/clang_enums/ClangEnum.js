import { simpleExtractor } from "./Extractors.js";

export default class ClangEnum {
  /**
   *
   * @param {string} name Name of the enum
   * @param {string} cppVarName Original name of the enum in the C++ source files
   * @param {(string) => string} mapper
   * @param {string[]} excludeArray
   * @param {string | undefined} className
   */
  constructor(
    name,
    cppVarName,
    mapper = (el) => el,
    excludeArray = [],
    className = undefined
  ) {
    mapper ??= (el) => el;
    excludeArray ??= [];

    this.name = name;
    this.cppVarName = cppVarName;
    this.mapper = mapper;
    this.excludeSet = new Set(excludeArray);
    this.className = className;
    this.enumValues = undefined;
    this.extractor = simpleExtractor;
    this.occurence = 1;
  }

  /**
   *
   * @param {number} occurence
   * @returns
   */
  setOccurence(occurence) {
    this.occurence = occurence;
    return this;
  }

  getEnumName() {
    return this.name;
  }

  getCompleteEnumName() {
    if (this.className === undefined) {
      return this.name;
    }

    return this.className + "_" + this.name;
  }

  getClassName() {
    return this.className;
  }

  /**
   *
   * @param {string[]} headerLines
   */
  setEnumValues(headerLines) {
    this.enumValues = this.extractor(this.name, headerLines, this.occurence);
  }

  getCode() {
    if (this.enumValues === undefined) {
      console.log("No enum values set for enum '" + this.name + "'");
      return undefined;
    }

    let code = "";

    code += "const std::string clava::" + this.cppVarName + "[] = {\n";

    for (let enumValue of this.enumValues) {
      if (this.excludeSet.has(enumValue.toString())) {
        console.log("Excluded enum '" + enumValue + "'");
        continue;
      }

      // Map enum value
      enumValue = this.mapper(enumValue);

      code += `        "${enumValue}",\n`;
    }

    code += "};\n";

    return code;
  }

  getEnumValues() {
    return this.enumValues
      .filter((e) => !this.excludeSet.has(e.toString()))
      .map(this.mapper);
  }
}

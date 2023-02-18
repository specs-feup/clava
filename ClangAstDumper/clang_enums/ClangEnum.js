laraImport("weaver.Query");
laraImport("Extractors");

/**
 * @class
 */
class ClangEnum {
  constructor(name, cppVarName, mapper, excludeArray, className) {
    this.name = name;
    this.cppVarName = cppVarName;

    if (mapper === undefined) {
      mapper = (element) => element;
    }
    this.mapper = mapper;

    if (excludeArray === undefined) {
      excludeArray = [];
    }
    this.excludeSet = new StringSet(excludeArray);
    //println("Exclude set: " + this.excludeSet.values());

    this.className = className;

    this.enumValues = undefined;

    /*
    if (extractor == undefined) {
      extractor = Extractors.simpleExtractor;
    }
    this.extractor = extractor;
	*/
    this.extractor = Extractors.simpleExtractor;
    this.occurence = 1;
  }

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

  /*
ClangEnum.prototype.getStartingNode = function() {
	if(this.className === undefined) {
		return Query.root();
	}
	
	var startingClass = Query.search("class", this.className).first();
	
	if(startingClass === undefined) {
		throw new Error("Clang enum specifies class '"+this.className+"', but it could not be found");
	}
	
	return startingClass;
}
*/

  setEnumValues(headerLines) {
    this.enumValues = this.extractor(this.name, headerLines, this.occurence);
  }

  /*
  setEnumValues(enumValues) {
    if (this.enumValues !== undefined) {
      println("Setting enum values again for enum " + this.name);
    }

    this.enumValues = enumValues;
  }
  */

  getCode() {
    if (this.enumValues === undefined) {
      println("No enum values set for enum '" + this.name + "'");
      return undefined;
    }

    var code = "";

    code += "extern const std::string clava::" + this.cppVarName + "[] = {\n";

    for (var enumValue of this.enumValues) {
      //println("Enum: " + enumValue);
      if (this.excludeSet.has(enumValue.toString())) {
        println("Excluded enum '" + enumValue + "'");
        continue;
      }

      // Map enum value
      enumValue = this.mapper(enumValue);

      code += '        "' + enumValue + '",\n';
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

class Extractors {
  static simpleExtractor(enumName, lines, occurence) {
    const regex = "enum\\s+(class\\s+)?" + enumName + "\\s.*{";
    const enumNameRegex = new RegExp(regex);
    //const enumNameRegex = new RegExp("\\s"+enumName+"\\s");

    const enums = [];
    let searching = true;
    let completed = false;
    let currentOccurence = 0;

    for (var line of lines) {
      if (searching) {
        // Find enum
        const matchResult = line.match(enumNameRegex);
        if (matchResult == null) {
          continue;
        }

        // Found enum, increment occurence
        currentOccurence++;

        // If not correct occurent, continue
        if (currentOccurence !== occurence) {
          continue;
        }

        // Found occurence, finish search and fallthrough, updating line
        searching = false;

        const indexOfBracket = line.indexOf("{");
        line = line.substring(indexOfBracket + 1);

        //continue;
      }

      // Collect enums until } is found
      line = line.trim();

      if (line.startsWith("}")) {
        // Finished, set flag
        completed = true;
        break;
      }

      // Continue if empty or comment
      if (line == "" || line.startsWith("#") || line.startsWith("+")) {
        continue;
      }

      // Split by ,
      const enumNames = line
        .split(",")
        // Trim string
        .map((s) => s.trim())
        // Remove empty strings
        .filter((s) => s.length > 0)
        // Remove =, } if present
        .map((s) => Extractors._processEnum(s));
      /*
      // Remove , if present
      if (line.endsWith(",")) {
        line = line.substring(0, line.length - 1);
      }
*/
      // Get enum

      //println("Enum: " + line)
      for (const enumName of enumNames) {
        enums.push(enumName);
      }

      if (line.includes("}")) {
        // Finished, set flag
        completed = true;
        break;
      }
    }

    if (!completed) {
      throw new Error("Could not find } for enum " + enumName);
    }

    return enums;
  }

  static _processEnum(enumName) {
    // Check for =
    const indexOfEquals = enumName.indexOf("=");
    if (indexOfEquals !== -1) {
      enumName = enumName.substring(0, indexOfEquals).trim();
    }

    // Check for }
    const indexOfBraket = enumName.indexOf("}");
    if (indexOfBraket !== -1) {
      enumName = enumName.substring(0, indexOfBraket).trim();
    }

    return enumName;
  }
}

laraImport("clava.Clava");
laraImport("weaver.Query");
laraImport("lara.util.StringSet");

/**
 * Utility methods related with the source code.
 *
 * @class
 */
class ClavaCode {
  /**
   * Writes the code corresponding to the current AST as a single file.
   *
   */
  static toSingleFile(fileOrBaseFolder, optionalFile) {
    if (fileOrBaseFolder === undefined) {
      fileOrBaseFolder = Clava.getWeavingFolder();
      var extension = Clava.isCxx() ? "cpp" : "c";
      optionalFile = "main." + extension;
    }

    const singleFileCode = ClavaCode.toSingleFileCode();

    var outputFile = Io.getPath(fileOrBaseFolder, optionalFile);

    Io.writeFile(outputFile, singleFileCode);

    // Copy includes
    var baseFolder = outputFile.getParentFile();
    for (var $include of Clava.getAvailableIncludes()) {
      var outputFile = Io.getPath(baseFolder, $include.name);
      Io.writeFile(outputFile, Io.readFile($include.filepath));
    }
  }

  /**
   * Generates code for a single fime corresponding to the current AST.
   *
   * @return {String} the code of the current AST as a single file.
   */
  static toSingleFileCode() {
    const staticVerification = true;

    var includes = new StringSet();
    var bodyCode = "";

    for (const $file of Query.search("file")) {
      if ($file.isHeader) {
        continue;
      }

      // Copy includes
      //copyIncludes($file);

      // Deal with static declarations
      var codeChanged = ClavaCode._renameStaticDeclarations(
        $file,
        staticVerification
      );
      if (codeChanged) {
        bodyCode += $file.code + "\n";
        println(
          "Generated file '" +
            $file.filepath +
            "' from AST, macros have disappeared"
        );
      } else {
        bodyCode += Io.readFile($file.filepath) + "\n";
      }

      //bodyCode += Io.readFile($file.filepath);

      // Collects all includes from input files, in order to put them at the beginning of the file
      for (const $child of $file.astChildren) {
        if ($child.astName === "IncludeDecl") {
          includes.add($child.code);
        }
      }
    }

    var singleFileCode = includes.values().join("\n") + "\n" + bodyCode;

    return singleFileCode;
  }

  static _renameStaticDeclarations($file, staticVerification) {
    if (!staticVerification) {
      return false;
    }

    var changedCode = false;

    // Look for static declarations
    for (var child of $file.astChildren) {
      //println("Storage class:" + child.storageClass);
      if (child.astName === "FunctionDecl" && child.storageClass === "static") {
        var newName = child.name + "_static_rename";
        child.name = newName;
        changedCode = true;
      }

      if (child.astName === "DeclStmt" && child.storageClass === "static") {
        println(child.code);
        throw "Not yet supported for static variable declarations";
      }
    }

    return changedCode;
  }

  /**
   * Tries to statically detect if a statement is executed only once.
   *
   * Restrictions:
   * - Does not take into account runtime execution problems, such as exceptions;
   * - Does not detect if the function is called indirectly through a function pointer;
   *
   * @return true if it could be detected, within the restrictions, that the statement is only executed once.
   */
  static isExecutedOnce($statement) {
    if (!$statement.getInstanceOf("statement")) {
      throw (
        "isExecutedOnce(): function expects a statement, received '" +
        $statement.joinPointType +
        "'"
      );
    }

    // Go back until it finds the function body
    var $currentScope = $statement.getAncestor("scope");

    while ($currentScope !== undefined) {
      var $scopeOwner = $currentScope.owner;

      // If finds a scope that is part of a loop or if/else, return false immediately
      if ($scopeOwner.getInstanceOf("loop") || $scopeOwner.getInstanceOf("if")) {
        debug(
          "ClavaCode.isExecutedOnce: failed because scope is part of loop or if"
        );
        return false;
      }

      // If function, check if main function
      if ($scopeOwner.getInstanceOf("function")) {
        var $function = $scopeOwner;

        // If main, passes check
        if ($function.name === "main") {
          return true;
        }

        // Verify if function is called only once
        var calls = $function.calls;
        if (calls.length !== 1) {
          debug(
            "ClavaCode.isExecutedOnce: failed because function '" +
              $function.name +
              "' is called " +
              calls.length +
              " times"
          );
          return false;
        }

        var $singleCall = calls[0];

        // Recursively call the function on the call statement
        return ClavaCode.isExecutedOnce($singleCall.getAncestor("statement"));
      }

      $currentScope = $currentScope.getAncestor("scope");
    }

    // Could not find the scope of the statement. Is it outside of a function?
    debug("ClavaCode.isExecutedOnce: failed because scope could not be found");
    return false;
  }

  /**
   * Returns the function definitions in the program with the given name.
   * @param {string} functionName - The name of the function to find
   * @param {boolean} isSingleFunction -  If true, ensures there is a single definition with the given name
   * @return {$function[]|$function} An array of function definitions, or a single function is 'isSingleFunction' is true
   */
  static getFunctionDefinition(functionName, isSingleFunction) {
    // Locate function
    var functionSearch = Clava.getProgram()
      .getDescendants("function")
      //.filter($f => $f.name.equals('main'))[0];
      .filter(function ($f) {
        return $f.name.equals(functionName) && $f.hasDefinition;
      });

    // If single function false, return search results
    if (!isSingleFunction) {
      return functionSearch;
    }

    if (functionSearch.length === 0) {
      throw "Could not find main function";
    }

    if (functionSearch.length > 1) {
      throw (
        "Found more than one definition for function with name '" +
        functionName +
        "'"
      );
    }

    return functionSearch[0];
  }
}

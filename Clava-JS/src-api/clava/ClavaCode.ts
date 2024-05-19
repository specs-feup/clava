import Io from "lara-js/api/lara/Io.js";
import { debug } from "lara-js/api/lara/core/LaraCore.js";
import { JavaClasses } from "lara-js/api/lara/util/JavaTypes.js";
import Query from "lara-js/api/weaver/Query.js";
import {
  FileJp,
  FunctionJp,
  If,
  Loop,
  Scope,
  Statement,
  StorageClass,
  Vardecl,
} from "../Joinpoints.js";
import Clava from "./Clava.js";

/**
 * Utility methods related with the source code.
 *
 */
export default class ClavaCode {
  /**
   * Writes the code corresponding to the current AST as a single file.
   *
   */
  static toSingleFile(
    fileOrBaseFolder?: string | JavaClasses.File,
    optionalFile?: string | JavaClasses.File
  ) {
    if (fileOrBaseFolder === undefined) {
      fileOrBaseFolder = Clava.getWeavingFolder();
      const extension = Clava.isCxx() ? "cpp" : "c";
      optionalFile = "main." + extension;
    }

    const singleFileCode = ClavaCode.toSingleFileCode();

    let outputFile = Io.getPath(fileOrBaseFolder, optionalFile);

    Io.writeFile(outputFile, singleFileCode);

    // Copy includes
    const baseFolder = outputFile.getParentFile();
    for (const $include of Clava.getAvailableIncludes()) {
      outputFile = Io.getPath(baseFolder, $include.name);
      Io.writeFile(outputFile, Io.readFile($include.filepath));
    }
  }

  /**
   * Generates code for a single fime corresponding to the current AST.
   *
   * @returns The code of the current AST as a single file.
   */
  static toSingleFileCode(): string {
    const staticVerification = true;

    const includes = new Set<string>();
    let bodyCode = "";

    for (const $f of Query.search(FileJp)) {
      const $file = $f as FileJp;

      if ($file.isHeader) {
        continue;
      }

      // Deal with static declarations
      const codeChanged = ClavaCode.renameStaticDeclarations(
        $file,
        staticVerification
      );
      if (codeChanged) {
        bodyCode += $file.code + "\n";
        console.log(
          "Generated file '" +
            $file.filepath +
            "' from AST, macros have disappeared"
        );
      } else {
        bodyCode += Io.readFile($file.filepath) + "\n";
      }

      // Collects all includes from input files, in order to put them at the beginning of the file
      for (const $child of $file.astChildren) {
        if ($child.astName === "IncludeDecl") {
          includes.add($child.code);
        }
      }
    }

    const singleFileCode = Array.from(includes).join("\n") + "\n" + bodyCode;

    return singleFileCode;
  }

  private static renameStaticDeclarations(
    $file: FileJp,
    staticVerification: boolean
  ) {
    if (!staticVerification) {
      return false;
    }

    let changedCode = false;

    // Look for static declarations
    for (const child of $file.children) {
      if (
        child instanceof FunctionJp &&
        child.storageClass === StorageClass.STATIC
      ) {
        const newName = child.name + "_static_rename";
        child.name = newName;
        changedCode = true;
      }

      if (
        child instanceof Vardecl &&
        child.storageClass === StorageClass.STATIC
      ) {
        console.log(child.code);
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
   * @returns True if it could be detected, within the restrictions, that the statement is only executed once.
   */
  static isExecutedOnce($statement: Statement | undefined): boolean {
    // Go back until it finds the function body
    let $currentScope: Scope | undefined = undefined;

    if ($statement !== undefined) {
      $currentScope = $statement.getAncestor("scope") as Scope | undefined;
    }

    while ($currentScope !== undefined) {
      const $scopeOwner = $currentScope.owner;

      // If finds a scope that is part of a loop or if/else, return false immediately
      if ($scopeOwner instanceof Loop || $scopeOwner instanceof If) {
        debug(
          "ClavaCode.isExecutedOnce: failed because scope is part of loop or if"
        );
        return false;
      }

      // If function, check if main function
      if ($scopeOwner instanceof FunctionJp) {
        const $function = $scopeOwner;

        // If main, passes check
        if ($function.name === "main") {
          return true;
        }

        // Verify if function is called only once
        const calls = $function.calls;
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

        const $singleCall = calls[0];

        // Recursively call the function on the call statement
        return ClavaCode.isExecutedOnce(
          $singleCall.getAncestor("statement") as Statement | undefined
        );
      }

      $currentScope = $currentScope.getAncestor("scope") as Scope | undefined;
    }

    // Could not find the scope of the statement. Is it outside of a function?
    debug("ClavaCode.isExecutedOnce: failed because scope could not be found");
    return false;
  }

  /**
   * Returns the function definitions in the program with the given name.
   *
   * @param functionName - The name of the function to find
   * @param isSingleFunction -  If true, ensures there is a single definition with the given name
   * @returns An array of function definitions, or a single function is 'isSingleFunction' is true
   */
  static getFunctionDefinition(
    functionName: string,
    isSingleFunction: boolean
  ): FunctionJp | FunctionJp[] {
    // Locate function
    const functionSearch = Clava.getProgram()
      .getDescendants("function")
      .filter(($f) => {
        const $function = $f as FunctionJp;
        return $function.name === functionName && $function.hasDefinition;
      }) as FunctionJp[];

    // If single function false, return search results
    if (!isSingleFunction) {
      return functionSearch;
    }

    if (functionSearch.length === 0) {
      throw `Could not find function with name '${functionName}'`;
    }

    if (functionSearch.length > 1) {
      throw `Found more than one definition for function with name '${functionName}'`;
    }

    return functionSearch[0];
  }
}

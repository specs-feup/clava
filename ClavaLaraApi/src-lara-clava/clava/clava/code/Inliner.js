laraImport("clava.ClavaJoinPoints");
laraImport("lara.util.StringSet");

class Inliner {
  #options;
  #variableIndex;

  /**
   *
   * @param  {object} options - Object with options. Supported options: 'prefix' (default: "__inline"), the prefix that will be used in the name of variables inserted by the Inliner
   */
  constructor(options) {
    this.#options = options ?? {};
    this.#options["prefix"] ??= "__inline";
    this.#variableIndex = 0;
  }

  #getInlinedVarName(originalVarName) {
    const prefix = this.#options["prefix"];
    return `${prefix}_${this.#variableIndex}_${originalVarName}`;
  }

  #hasCycle($function, _path = new Set()) {
    if (_path.has($function.name)) {
      return true;
    }

    _path.add($function.name);
    for (const $exprStmt of $function.descendants("exprStmt")) {
      const $expr = $exprStmt.expr;
      if (
        !(
          $expr.instanceOf("binaryOp") &&
          $expr.isAssignment &&
          $expr.right.instanceOf("call")
        ) &&
        !$expr.instanceOf("call")
      ) {
        continue;
      }

      const $call = $expr.instanceOf("call") ? $expr : $expr.right;
      const $callee = $call.definition;
      if ($callee == undefined) {
        continue;
      }
      if (this.#hasCycle($callee, _path)) {
        return true;
      }
    }

    _path.delete($function.name);
    return false;
  }

  inlineFunctionTree($function, _visited = new Set()) {
    if (this.#hasCycle($function)) {
      return false;
    }

    if (_visited.has($function.name)) {
      return true;
    }
    _visited.add($function.name);

    for (const $exprStmt of $function.descendants("exprStmt")) {
      const $expr = $exprStmt.expr;
      if (
        !(
          $expr.instanceOf("binaryOp") &&
          $expr.isAssignment &&
          $expr.right.instanceOf("call")
        ) &&
        !$expr.instanceOf("call")
      ) {
        continue;
      }

      const $call = $expr.instanceOf("call") ? $expr : $expr.right;
      const $callee = $call.definition;
      if ($callee == undefined) {
        continue;
      }

      // Ignore functions that are part of the system headers
      if ($call.function.isInSystemHeader) {
        continue;
      }

      // If any of the parameters of the function is an array, return.
      // Not supported yet
      /*
      if ($callee.params.filter(($param) => $param.type.isArray).length > 0) {
        debug(
          `Inliner: could not inline call to function ${$callee.name}@${$callee.location} since array parameters are not supported`
        );
        continue;
      }
      */

      this.inlineFunctionTree($callee, _visited);
      this.inline($exprStmt);
    }

    return true;
  }

  #getInitStmts($varDecl, $expr) {
    const type = $varDecl.type;

    // If array, how to deal with this?
    /*
    if (type.isArray) {
      const arrayDims = $varDecl.type.arrayDims;
      const lastDim = arrayDims[arrayDims.length - 1];

      if (lastDim < 0) {
        throw new Error(
          "Could not inline code with static array '" +
            $varDecl.code +
            "' whose last dimension is unknown (" +
            lastDim +
            ")"
        );
      }

      println("Vardecl: " + $varDecl.code);
      println("Vardecl type: " + $varDecl.type);
      println("Array dims: " + $varDecl.type.arrayDims);
      println("Array size: " + $varDecl.type.arraySize);
      
      const $assign = ClavaJoinPoints.assign(
        ClavaJoinPoints.varRef($varDecl),
        $expr
      );

      return [ClavaJoinPoints.exprStmt($assign)];
    }
    */

    const $assign = ClavaJoinPoints.assign(
      ClavaJoinPoints.varRef($varDecl),
      $expr
    );

    return [ClavaJoinPoints.exprStmt($assign)];
  }

  inline($exprStmt) {
    let $target;
    let $call;

    if ($exprStmt.expr.instanceOf("binaryOp") && $exprStmt.expr.isAssignment) {
      $target = $exprStmt.expr.left;
      $call = $exprStmt.expr.right;
    } else if ($exprStmt.expr.instanceOf("call")) {
      $call = $exprStmt.expr;
    }

    if (!$call.function.isImplementation) {
      return;
    }

    let args = $call.args;
    if (!Array.isArray(args)) {
      args = [args];
    }

    const $function = $call.function;

    if ($function.descendants("returnStmt").length > 1) {
      throw new Error(
        `'${$function.name}' cannot be inlined: more than one return statement`
      );
    }

    const params = $function.params;

    const newVariableMap = new Map();
    const paramDeclStmts = [];

    // TODO: args can be greater than params, if varargs. How to deal with this?
    for (let i = 0; i < params.length; i++) {
      const $arg = args[i];
      const $param = params[i];

      // Arrays cannot be assigned
      // If param is array or pointer, there is no need to add declaration,
      // simply rename the param to the name of the arg
      //if ($param.type.isArray || $param.type.isPointer) {
      if ($param.type.isArray) {
        /*
        if (!$arg.instanceOf("varref")) {
          throw new Error(
            `Expected varref, found '${$arg.joinPointType}' (${$arg.code}) at ${$arg.location}`
          );
        }
        */

        // If varref, has decl
        //newVariableMap.set($param.name, $arg.decl);

        // If varref, use expression as-is, with some parenthesis around to guarantee order of operations
        //const $adaptedArg = this.#adaptArg($arg);
        /*
        const $adaptedArg = $arg.instanceOf("parenExpr")
          ? $arg
          : ClavaJoinPoints.parenthesis($arg);
*/
        //newVariableMap.set($param.name, $adaptedArg);
        newVariableMap.set($param.name, $arg);
      } else {
        const newName = this.#getInlinedVarName($param.name);
        const $varDecl = ClavaJoinPoints.varDeclNoInit(newName, $param.type);
        const $varDeclStmt = ClavaJoinPoints.declStmt($varDecl);

        const $initStmts = this.#getInitStmts($varDecl, $arg);

        newVariableMap.set($param.name, $varDecl);
        paramDeclStmts.push($varDeclStmt, ...$initStmts);
      }
    }

    for (const stmt of $function.body.descendants("declStmt")) {
      const $varDecl = stmt.decls[0];
      if (!$varDecl.instanceOf("vardecl")) {
        continue;
      }

      const newName = this.#getInlinedVarName($varDecl.name);
      const $newDecl = ClavaJoinPoints.varDeclNoInit(newName, $varDecl.type);
      newVariableMap.set($varDecl.name, $newDecl);
    }

    // Replace decl stmts of old vardecls with vardecls of new names (params are not included)
    const $newNodes = $function.body.copy();
    for (const $declStmt of $newNodes.descendants("declStmt")) {
      const $varDecl = $declStmt.decls[0];
      if (!$varDecl.instanceOf("vardecl")) {
        continue;
      }

      const $newVarDecl = $declStmt.replaceWith(
        ClavaJoinPoints.declStmt(newVariableMap.get($varDecl.name))
      );
    }

    // Update varrefs
    for (const $varRef of $newNodes.descendants("varref")) {
      if ($varRef.kind === "function_call") {
        continue;
      }

      const $varDecl = $varRef.decl;

      // If global variable, will not be in the variable map
      // TODO: Add extern to the target Translation Unit, in case it is not already declared
      if ($varDecl !== undefined && $varDecl.isGlobal) {
        /*
        println(
          "Add global declaration before this function: " +
            $call.ancestor("function").id
        );
        */

        // Copy vardecl to work over it
        const $varDeclNoInit = $varDecl.copy();

        // Remove initialization
        $varDeclNoInit.removeInit(false);

        // Change storage class to extern
        $varDeclNoInit.storageClass = "extern";

        $call.ancestor("function").insertBefore($varDeclNoInit);
        continue;
      }

      // Verify if there is a mapping
      const newVar = newVariableMap.get($varDecl.name);
      if (newVar === undefined) {
        throw new Error(
          "Could not find variable " +
            $varDecl.name +
            "@" +
            $varRef.location +
            " in variable map"
        );
      }

      // If vardecl, map contains reference to old vardecl, create a varref from the new vardecl
      if (newVar.instanceOf("vardecl")) {
        //const newVarDecl = newVariableMap.get(newVar.name);
        //$varRef.replaceWith(ClavaJoinPoints.varRef(newVarDecl));
        $varRef.replaceWith(ClavaJoinPoints.varRef(newVar));
      }
      // If expression, simply replace varref with the expression
      else if (newVar.instanceOf("expression")) {
        const $adaptedVar =
          // If varref, does not need parenthesis
          newVar.instanceOf("varref")
            ? newVar
            : // For other expressions, if parent is already a parenthesis, does not need to add a new one
            $varRef.parent.instanceOf("parenExpr")
            ? newVar
            : // Add parenthesis
              ClavaJoinPoints.parenthesis(newVar);

        $varRef.replaceWith($adaptedVar);
      } else {
        throw new Error(
          "Not defined when newVar is of type '" + newVar.joinPointType + "'"
        );
      }

      // Replace old varrefs with new expressions

      // Take into account the case where the node is a vardecl, and when it is an expression

      // Create varref from vardecl, to create a connection between decl and expr
      /*
      $varRef.replaceWith(
        ClavaJoinPoints.varRef(newVariableMap.get($varRef.decl.name))
      );
      */
    }

    // Update varrefs inside types
    for (const $jp of $newNodes.descendants("joinpoint")) {
      // If no type, ignore

      if (!$jp.hasType) {
        continue;
      }

      const type = $jp.type;

      const updatedType = this.#updateType(type, $call, newVariableMap);

      if (updatedType !== type) {
        $jp.type = updatedType;
      }
    }

    // Remove/replace return statements

    if ($exprStmt.expr.instanceOf("binaryOp") && $exprStmt.expr.isAssignment) {
      for (const $returnStmt of $newNodes.descendants("returnStmt")) {
        if (
          $returnStmt.returnExpr !== null &&
          $returnStmt.returnExpr !== undefined
        ) {
          $returnStmt.replaceWith(
            ClavaJoinPoints.exprStmt(
              ClavaJoinPoints.assign($target, $returnStmt.returnExpr)
            )
          );
        } else {
          $returnStmt.detach();
        }
      }
    } else if ($exprStmt.expr.instanceOf("call")) {
      for (const $returnStmt of $newNodes.descendants("returnStmt")) {
        // Replace the return with a nop (i.e. empty statement), in case there is a label before. Otherwise, just remove return
        const left = $returnStmt.siblingsLeft;
        if (left.length > 0 && left[left.length - 1].instanceOf("labelStmt")) {
          $returnStmt.replaceWith(ClavaJoinPoints.emptyStmt());
        } else {
          $returnStmt.detach();
        }
      }
    }

    // For any calls inside $newNodes, add forward declarations before the function, if they have definition
    // TODO: this should be done for calls of functions that are on this file. For other files, the corresponding include
    // should be added
    const $parentFunction = $call.ancestor("function");
    const addedDeclarations = new StringSet();
    for (const $newCall of Query.searchFrom($newNodes, "call")) {
      // Ignore if only declaration exists (include should be added instead)
      //if (!$newCall.function.isImplementation) {
      //  continue;
      //}

      // Ignore functions that are part of the system headers
      if ($newCall.function.isInSystemHeader) {
        continue;
      }

      if (addedDeclarations.has($newCall.function.id)) {
        continue;
      }

      addedDeclarations.add($newCall.function.id);

      const $newFunctionDecl = ClavaJoinPoints.functionDeclFromType(
        $newCall.function.name,
        $newCall.function.functionType
      );

      $parentFunction.insertBefore($newFunctionDecl);
    }

    // Let the function body be on its own scope
    // If the function uses local labels they must appear at the beginning of the scope
    const inlinedScope =
      paramDeclStmts.length === 0
        ? $newNodes
        : ClavaJoinPoints.scope([...paramDeclStmts, $newNodes]);

    $exprStmt.replaceWith(inlinedScope);

    this.#variableIndex++;
  }

  #updateType(type, $call, newVariableMap) {
    // Default result
    // TODO: Replace with just the node, can always test if it is itself
    // Since any type node can be shared, any change must be made in copies
    //let result = type;

    // If pointer type, check pointee
    if (type.instanceOf("pointerType")) {
      //println("UPDATE POINTER TYPE");
      const original = type.pointee;
      const updated = this.#updateType(original, $call, newVariableMap);

      if (original !== updated) {
        const newType = type.copy();
        newType.pointee = updated;
        return newType;
      }
    }

    if (type.instanceOf("parenType")) {
      //println("UPDATE PAREN TYPE");
      const original = type.innerType;
      const updated = this.#updateType(original, $call, newVariableMap);

      if (original !== updated) {
        const newType = type.copy();
        newType.innerType = updated;
        return newType;
      }
    }

    if (type.instanceOf("variableArrayType")) {
      // Has to track changes both for element type and its own array expression
      // Either was, has to update this type
      let hasChanges = false;

      // Element type
      const original = type.elementType;
      const updated = this.#updateType(original, $call, newVariableMap);

      if (original !== updated) {
        hasChanges = true;
      }

      //println("UPDATE VARIABLE ARRAY TYPE");
      let $sizeExprCopy = type.sizeExpr.copy();

      // Update any children of sizeExpr
      //println("UPDATE SIZEEXPR CHILDREN");
      //println("SIZE EXPR COPY: " + $sizeExprCopy.ast);
      for (const $varRef of Query.searchFrom($sizeExprCopy, "varref")) {
        //println("VARREF: " + $varRef.code);
        const $newVarref = this.#updateVarRef($varRef, $call, newVariableMap);
        //println("HAS CHANGED: " + ($newVarref !== $varRef));
        if ($newVarref !== $varRef) {
          hasChanges = true;
          $varRef.replaceWith($newVarref);
        }
      }

      // Update top expr, if needed
      //println("UPDATE TOP EXPR");
      const $newVarref = this.#updateVarRef(
        $sizeExprCopy,
        $call,
        newVariableMap
      );

      if ($newVarref !== $sizeExprCopy) {
        hasChanges = true;
        //$sizeExprCopy = $newVarref;
      }
      //println("HAS CHANGES: " + hasChanges);
      if (hasChanges) {
        //println("CHANGES");
        //println("OLD: " + type.sizeExpr.code);
        //println("NEW: " + $sizeExprCopy.code);
        const newType = type.copy();
        newType.elementType = updated;
        newType.sizeExpr = $newVarref;

        return newType;
      }
    }

    // By default, return type with no changes
    return type;
  }

  #updateVarRef($varRef, $call, newVariableMap) {
    //if ($varRef.kind === "function_call") {
    //  return $varRef;
    //}

    const $varDecl = $varRef.decl;

    // If global variable, will not be in the variable map
    // TODO: Add extern to the target Translation Unit, in case it is not already declared
    if ($varDecl !== undefined && $varDecl.isGlobal) {
      /*
      println(
        "Add global declaration before this function: " +
          $call.ancestor("function").id
      );
      */

      // Copy vardecl to work over it
      const $varDeclNoInit = $varDecl.copy();

      // Remove initialization
      $varDeclNoInit.removeInit(false);

      // Change storage class to extern
      $varDeclNoInit.storageClass = "extern";

      $call.ancestor("function").insertBefore($varDeclNoInit);
      return $varRef;
    }

    const newVar = newVariableMap.get($varDecl.name);

    // If not found, just return
    if (newVar === undefined) {
      return $varRef;
    }

    // If vardecl, create a new varref
    if (newVar.instanceOf("vardecl")) {
      return ClavaJoinPoints.varRef(newVar);
    }

    // If expression, return expression
    if (newVar.instanceOf("expression")) {
      return newVar;
    }

    throw new Error(
      "Case not supported, newVar of type '" + newVar.joinPointType + "'"
    );

    //return ClavaJoinPoints.varRef(newVariableMap.get($varRef.decl.name));
  }

  /*
  #adaptArg($arg) {
    println("Type of arg: " + $arg.joinPointType);

    if ($arg.instanceOf("unaryOp")) {
      // If address of, can be removed
      println("ARG OP: " + $arg.operator);
      return $arg;
    }

    if ($arg.instanceOf("parenExpr")) {
      return ClavaJoinPoints.parenthesis(this.#adaptArg($arg.subExpr));
    }

    return $arg;
  }
  */
}

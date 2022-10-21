laraImport("clava.ClavaJoinPoints");

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
        if (!$arg.instanceOf("varref")) {
          throw new Error(
            "Expected varref, found '" + $arg.joinPointType + "'"
          );
        }

        // If varref, has decl
        newVariableMap.set($param.name, $arg.decl);
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

    const $newNodes = $function.body.copy();
    for (const $declStmt of $newNodes.descendants("declStmt")) {
      const $varDecl = $declStmt.decls[0];
      if (!$varDecl.instanceOf("vardecl")) {
        continue;
      }

      $declStmt.replaceWith(
        ClavaJoinPoints.declStmt(newVariableMap.get($varDecl.name))
      );
    }

    for (const $varRef of $newNodes.descendants("varref")) {
      if ($varRef.kind === "function_call") {
        continue;
      }

      const $varDecl = $varRef.decl;

      // If global variable, will not be in the variable map
      // TODO: Add extern to the target Translation Unit, in case it is not already declared
      if ($varDecl.isGlobal) {
        /*
        println(
          "Add global declaration before this function: " +
            $call.ancestor("function").id
        );
        */

        // Copy vardecl to work over it
        const $varDeclNoInit = $varDecl.copy();

        // Remove initialization
        $varDeclNoInit.removeInit();

        // Change storage class to extern
        $varDeclNoInit.storageClass = "extern";

        $call.ancestor("function").insertBefore($varDeclNoInit);
        continue;
      }

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

      $varRef.replaceWith(
        ClavaJoinPoints.varRef(newVariableMap.get($varRef.decl.name))
      );
    }

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
        if ($returnStmt.expr !== null && $returnStmt.expr !== undefined) {
          $returnStmt.replaceWith(ClavaJoinPoints.exprStmt($returnStmt.expr));
        } else {
          $returnStmt.detach();
        }
      }
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
}

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
      this.inlineFunctionTree($callee, _visited);
      this.inline($exprStmt);
    }

    return true;
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
    for (let i = 0; i < args.length; i++) {
      const $arg = args[i];
      const $param = params[i];

      const newName = this.#getInlinedVarName($param.name);
      const $varDecl = ClavaJoinPoints.varDeclNoInit(newName, $param.type);
      const $varDeclStmt = ClavaJoinPoints.declStmt($varDecl);
      const $init = ClavaJoinPoints.exprStmt(
        ClavaJoinPoints.assign(ClavaJoinPoints.varRef($varDecl), $arg.copy())
      );

      newVariableMap.set($param.name, $varDecl);
      paramDeclStmts.push($varDeclStmt, $init);
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

    $exprStmt.replaceWith(
      ClavaJoinPoints.scope([...paramDeclStmts, ...$newNodes.children])
    );

    this.#variableIndex++;
  }
}

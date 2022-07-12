laraImport("clava.ClavaJoinPoints");

class Inliner {
  #variablePrefix;
  #variableIndex;
  constructor() {
    this.#variableIndex = 0;
    this.#variablePrefix = "__inline";
  }

  #getInlinedVarName(originalVarName) {
    return `${this.#variablePrefix}_${this.#variableIndex}_${originalVarName}`;
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

    let args = $call.args;
    if (!Array.isArray(args)) {
      args = [args];
    }

    const $function = $call.function;
    const params = $function.params;

    const newVariableMap = new Map();
    const paramDeclStmts = [];
    for (let i = 0; i < args.length; i++) {
      const $arg = args[i];
      const $param = params[i];

      const newName = this.#getInlinedVarName($param.name);
      const $varDecl = ClavaJoinPoints.varDecl(newName, $arg.copy());

      newVariableMap.set($param.name, $varDecl);
      paramDeclStmts.push(ClavaJoinPoints.declStmt($varDecl));
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

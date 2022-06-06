laraImport("lara.pass.Pass");
laraImport("clava.ClavaJoinPoints");
laraImport("clava.code.StatementDecomposer");
laraImport("clava.code.ForToWhileStmt");
laraImport("clava.code.DoToWhileStmt");

class SimplifyLoops extends Pass {
  #statementDecomposer;

  constructor(statementDecomposer) {
    super("SimplifyLoops");
    this.#statementDecomposer = statementDecomposer;
  }

  _apply_impl($jp) {
    if (
      $jp.instanceOf("loop") &&
      ($jp.kind === "for" || $jp.kind === "dowhile")
    ) {
      let $currentNode = $jp;
      if ($jp.kind === "for") {
        const $forToWhileScope = ForToWhileStmt($jp);
        const [_, $while] = $forToWhileScope.children;
        $currentNode = $while;
      } else if ($jp.kind === "dowhile") {
        $currentNode = DoToWhileStmt($jp);
      }

      const $loopCond = $currentNode.cond;
      const decomposeResult = this.#statementDecomposer.decomposeExpr(
        $loopCond.expr
      );

      for (const stmt of decomposeResult.precedingStmts) {
        $currentNode.insertBefore(stmt);
      }
      for (const stmt of decomposeResult.succeedingStmts) {
        $currentNode.insertAfter(stmt);
      }
      for (const stmt of decomposeResult.succeedingStmts.slice().reverse()) {
        $currentNode.body.insertBegin(stmt);
      }
      $currentNode.cond.replaceWith(
        ClavaJoinPoints.exprStmt(decomposeResult.$resultExpr)
      );

      return new PassResult(this.name, {
        appliedPass: true,
        insertedLiteralCode: false,
        location: $jp.location,
      });
    }

    return new PassResult(this.name, {
      appliedPass: false,
      insertedLiteralCode: false,
      location: $jp.location,
    });
  }
}

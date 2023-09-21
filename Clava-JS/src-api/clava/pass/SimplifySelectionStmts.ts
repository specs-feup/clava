laraImport("lara.pass.Pass");
laraImport("clava.code.StatementDecomposer");
laraImport("weaver.Query");
laraImport("lara.pass.results.PassResult");

// TODO: Refactor to use the SimplePass pattern
class SimplifySelectionStmts extends Pass {
  #statementDecomposer;

  constructor(statementDecomposer) {
    super();
    this.#statementDecomposer = statementDecomposer;
  }

  /**
   * @return {string} Name of the pass
   * @override
   */
  get name() {
    return "SimplifySelectionStmts";
  }

  _apply_impl($jp) {
    let appliedPass = false;
    for (const $if of this.#findStmts($jp)) {
      appliedPass = true;
      this.#transform($if);
    }

    return new PassResult(this, $jp, { appliedPass: appliedPass });
  }

  #findStmts($jp) {
    return Query.searchFromInclusive($jp, "if");
  }

  #transform($ifStmt) {
    const $ifCond = $ifStmt.cond;
    const decomposeResult = this.#statementDecomposer.decomposeExpr($ifCond);

    for (const stmt of decomposeResult.precedingStmts) {
      $ifStmt.insertBefore(stmt);
    }
    for (const stmt of decomposeResult.succeedingStmts.slice().reverse()) {
      $ifStmt.then.insertBegin(stmt);
      $ifStmt.else.insertBegin(stmt);
    }
    $ifStmt.cond.replaceWith(decomposeResult.$resultExpr);
  }
}

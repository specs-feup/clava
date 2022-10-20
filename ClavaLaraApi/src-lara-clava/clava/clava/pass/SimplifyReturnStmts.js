laraImport("lara.pass.Pass");
laraImport("clava.code.StatementDecomposer");
laraImport("weaver.Query");

class SimplifyReturnStmts extends Pass {
  #statementDecomposer;

  constructor(statementDecomposer) {
    super("SimplifyReturnStmts");
    this.#statementDecomposer = statementDecomposer;
  }

  _apply_impl($jp) {
    let appliedPass = false;
    for (const $returnStmt of this.#findStmts($jp)) {
      appliedPass = true;
      this.#transform($returnStmt);
    }

    return new PassResult(this.name, {
      appliedPass,
      insertedLiteralCode: false,
      location: $jp.location,
    });
  }

  #findStmts($jp) {
    return Query.searchFromInclusive($jp, "returnStmt");
  }

  #transform($returnStmt) {
    const decomposeResult = this.#statementDecomposer.decompose($returnStmt);

    // Returns a list of stmts, replace with current return
    for (const stmt of decomposeResult) {
      $returnStmt.insertBefore(stmt);
    }

    $returnStmt.detach();
  }
}

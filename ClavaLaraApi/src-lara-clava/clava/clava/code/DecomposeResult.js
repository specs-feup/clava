class DecomposeResult {
  constructor(
    precedingStmts,
    $resultExpr,
    succeedingStmts = []
    //    declaresVar = false
  ) {
    this.precedingStmts = precedingStmts;
    this.$resultExpr = $resultExpr;
    this.succeedingStmts = succeedingStmts;
    // True if any of the statement declares a new variable
    //  this.declaresVar = declaresVar;
  }

  /**
   * Represents the statements to be placed before the use of the expression
   * @deprecated use `precedingStmts` instead
   */
  get stmts() {
    return this.precedingStmts;
  }

  toString() {
    const precedingCode = this.precedingStmts
      .map((stmt) => stmt.code)
      .join(" ");
    return `${precedingCode} -> ${this.$resultExpr.code}`;
  }
}

import { Expression, Statement } from "../../Joinpoints.js";

export default class DecomposeResult {
  precedingStmts: Statement[];
  $resultExpr: Expression;
  succeedingStmts: Statement[];

  constructor(
    precedingStmts: Statement[],
    $resultExpr: Expression,
    succeedingStmts: Statement[] = []
  ) {
    this.precedingStmts = precedingStmts;
    this.$resultExpr = $resultExpr;
    this.succeedingStmts = succeedingStmts;
  }

  /**
   * Represents the statements to be placed before the use of the expression
   * @deprecated use `precedingStmts` instead
   */
  get stmts() {
    return this.precedingStmts;
  }

  toString(): string {
    const precedingCode = this.precedingStmts
      .map((stmt) => stmt.code)
      .join(" ");
    return `${precedingCode} -> ${this.$resultExpr.code}`;
  }
}

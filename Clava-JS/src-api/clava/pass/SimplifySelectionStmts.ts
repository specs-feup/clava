import Pass from "@specs-feup/lara/api/lara/pass/Pass.js";
import PassResult from "@specs-feup/lara/api/lara/pass/results/PassResult.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";
import { If, Joinpoint } from "../../Joinpoints.js";
import StatementDecomposer from "../code/StatementDecomposer.js";

// TODO: Refactor to use the SimplePass pattern
export default class SimplifySelectionStmts extends Pass {
  protected _name = "SimplifySelectionStmts";
  private statementDecomposer: StatementDecomposer;

  constructor(statementDecomposer: StatementDecomposer) {
    super();
    this.statementDecomposer = statementDecomposer;
  }

  protected _apply_impl($jp: Joinpoint): PassResult {
    let appliedPass = false;
    for (const $if of Query.searchFromInclusive($jp, If)) {
      appliedPass = true;
      this.transform($if);
    }

    return new PassResult(this, $jp, {
      appliedPass: appliedPass,
      insertedLiteralCode: false,
    });
  }

  private transform($ifStmt: If): void {
    const $ifCond = $ifStmt.cond;
    const decomposeResult = this.statementDecomposer.decomposeExpr($ifCond);

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

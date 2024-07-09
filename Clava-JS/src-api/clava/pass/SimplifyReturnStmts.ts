import Pass from "lara-js/api/lara/pass/Pass.js";
import PassResult from "lara-js/api/lara/pass/results/PassResult.js";
import Query from "lara-js/api/weaver/Query.js";
import { Joinpoint, ReturnStmt } from "../../Joinpoints.js";
import StatementDecomposer from "../code/StatementDecomposer.js";

// TODO: Refactor to use the SimplePass pattern
export default class SimplifyReturnStmts extends Pass {
  protected _name = "SimplifyReturnStmts";
  private statementDecomposer;

  constructor(statementDecomposer: StatementDecomposer) {
    super();
    this.statementDecomposer = statementDecomposer;
  }

  protected _apply_impl($jp: Joinpoint): PassResult {
    let appliedPass = false;
    for (const $returnStmt of Query.searchFromInclusive($jp, ReturnStmt)) {
      const transformed = this.transform($returnStmt);
      // If any change, mark as applied
      if (transformed) {
        appliedPass = true;
      }
    }

    return new PassResult(this, $jp, {
      appliedPass: appliedPass,
      insertedLiteralCode: false,
    });
  }

  /**
   *
   * @param $returnStmt -
   * @returns true if there were changes, false otherwise
   */
  private transform($returnStmt: ReturnStmt): boolean {
    const decomposeResult = this.statementDecomposer.decompose($returnStmt);

    if (decomposeResult.length === 0) {
      return false;
    }

    // Returns a list of stmts, replace with current return
    for (const stmt of decomposeResult) {
      $returnStmt.insertBefore(stmt);
    }

    $returnStmt.detach();

    return true;
  }
}

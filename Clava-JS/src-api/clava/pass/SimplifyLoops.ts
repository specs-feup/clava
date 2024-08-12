import Pass from "lara-js/api/lara/pass/Pass.js";
import PassResult from "lara-js/api/lara/pass/results/PassResult.js";
import { DeclStmt, ExprStmt, Joinpoint, Loop } from "../../Joinpoints.js";
import ClavaJoinPoints from "../ClavaJoinPoints.js";
import DoToWhileStmt from "../code/DoToWhileStmt.js";
import ForToWhileStmt from "../code/ForToWhileStmt.js";
import StatementDecomposer from "../code/StatementDecomposer.js";

export default class SimplifyLoops extends Pass {
  protected _name = "SimplifyLoops";

  private statementDecomposer;
  private options;
  private label_suffix = 0;

  /**
   *
   * @param statementDecomposer -
   * @param options - Object with options. Supported options: 'forToWhile' (default: true), transforms for loops into while loops
   */
  constructor(
    statementDecomposer: StatementDecomposer,
    options = { forToWhile: true }
  ) {
    super();
    this.statementDecomposer = statementDecomposer;
    this.options = options;
  }

  protected _apply_impl($jp: Joinpoint): PassResult {
    let appliedPass = false;
    for (const $loop of this._findLoops($jp)) {
      appliedPass = true;
      if (this.options.forToWhile) {
        const $whileLoop = this.makeWhileLoop($loop);
        this.transform($whileLoop);
      }
    }

    return new PassResult(this, $jp, {
      appliedPass: appliedPass,
      insertedLiteralCode: true,
    });
  }

  protected *_findLoops($jp: Joinpoint): Generator<Loop> {
    for (const child of $jp.children) {
      yield* this._findLoops(child);
    }
    if (
      $jp instanceof Loop &&
      ($jp.kind === "for" || $jp.kind === "dowhile" || $jp.kind === "while")
    ) {
      yield $jp;
    }
  }

  private makeWhileLoop($loop: Loop): Loop {
    if ($loop.kind === "for") {
      const $forToWhileScope = ForToWhileStmt($loop, this.label_suffix++);
      return $forToWhileScope.children[1] as Loop;
    } else if ($loop.kind === "dowhile") {
      return DoToWhileStmt($loop, this.label_suffix++);
    } else {
      return $loop;
    }
  }

  private transform($whileLoop: Loop): void {
    const $loopCond = $whileLoop.cond as ExprStmt;
    const decomposeResult = this.statementDecomposer.decomposeExpr(
      $loopCond.expr
    );

    for (const stmt of decomposeResult.precedingStmts) {
      $whileLoop.insertBefore(stmt);
    }
    for (const stmt of decomposeResult.succeedingStmts) {
      $whileLoop.insertAfter(stmt);
    }
    for (const stmt of decomposeResult.succeedingStmts.slice().reverse()) {
      $whileLoop.body.insertBegin(stmt);
    }
    for (const stmt of decomposeResult.precedingStmts.filter(
      ($stmt) => !($stmt instanceof DeclStmt)
    )) {
      $whileLoop.body.insertEnd(stmt);
    }
    $whileLoop.cond.replaceWith(
      ClavaJoinPoints.exprStmt(decomposeResult.$resultExpr)
    );
  }
}

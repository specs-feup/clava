laraImport("lara.pass.Pass");
laraImport("clava.ClavaJoinPoints");
laraImport("clava.code.StatementDecomposer");
laraImport("clava.code.ForToWhileStmt");
laraImport("clava.code.DoToWhileStmt");
laraImport("weaver.Query");

class SimplifyLoops extends Pass {
  #statementDecomposer;
  #label_suffix = 0;

  constructor(statementDecomposer) {
    super("SimplifyLoops");
    this.#statementDecomposer = statementDecomposer;
  }

  _apply_impl($jp) {
    let appliedPass = false;
    for (const $loop of this._findLoops($jp)) {
      appliedPass = true;
      const $whileLoop = this.#makeWhileLoop($loop);
      this.#transform($whileLoop);
    }

    return new PassResult(this.name, {
      appliedPass,
      insertedLiteralCode: false,
      location: $jp.location,
    });
  }

  *_findLoops($jp) {
    for (const child of $jp.children) {
      yield* this._findLoops(child);
    }
    if (
      $jp.instanceOf("loop") &&
      ($jp.kind === "for" || $jp.kind === "dowhile" || $jp.kind === "while")
    ) {
      yield $jp;
    }
  }

  #makeWhileLoop($loop) {
    if ($loop.kind === "for") {
      const $forToWhileScope = ForToWhileStmt($loop, this.#label_suffix++);
      const [_, $while] = $forToWhileScope.children;
      return $while;
    } else if ($loop.kind === "dowhile") {
      return DoToWhileStmt($loop, this.#label_suffix++);
    } else {
      return $loop;
    }
  }

  #transform($whileLoop) {
    const $loopCond = $whileLoop.cond;
    const decomposeResult = this.#statementDecomposer.decomposeExpr(
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
    $whileLoop.cond.replaceWith(
      ClavaJoinPoints.exprStmt(decomposeResult.$resultExpr)
    );
  }
}

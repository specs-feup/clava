laraImport("clava.code.DecomposeResult");
laraImport("clava.ClavaJoinPoints");
laraImport("lara.Check");

/**
 * Decomposes complex statements into several simpler ones.
 */
class StatementDecomposer {
  /**
   * @param {string} [tempPrefix="decomp_"]
   * @param {number} [startIndex=0]
   */
  constructor(tempPrefix = "decomp_", startIndex = 0) {
    this.tempPrefix = tempPrefix ?? "decomp_";
    this.startIndex = startIndex ?? 0;

    Check.isString(this.tempPrefix);
    Check.isNumber(this.startIndex);
  }

  #throwNotImplemented(generalType, specificType) {
    throw new Error(
      `StatementDecomposer not implemented for ${generalType} of type '${specificType}'`
    );
  }

  #newTempVarname() {
    const varName = `${this.tempPrefix}${this.startIndex}`;
    this.startIndex++;
    return varName;
  }

  #isValidNode($jp) {
    // If preceeding statement is a CaseStmt or a LabelStmt it might generate invalid code if a declaration is inserted
    // Add empty statement after the label to avoid this situation
    if ($jp.instanceOf("statement") && !$jp.instanceOf("emptyStmt")) {
      const $leftStmt = $jp.leftJp;

      if (
        $leftStmt !== undefined &&
        ($leftStmt.instanceOf("case") || $leftStmt.instanceOf("labelStmt"))
      ) {
        debug(
          `StatementDecomposer: statement just before label, inserting empty statement after as a precaution`
        );

        $leftStmt.insertAfter(ClavaJoinPoints.emptyStmt());

        return true;
      }

      return true;
    }

    const parentStmt = $jp.ancestor("statement");

    if (parentStmt !== undefined) {
      return this.#isValidNode(parentStmt);
    }

    return true;
  }

  /**
   * If the given statement can be decomposed in two or more statements, replaces the statement with the decomposition.
   *
   * @param {$statement} $stmt - A statement that will be decomposed.
   */
  decomposeAndReplace($stmt) {
    const stmts = this.decompose($stmt);

    // No statements to replace
    if (stmts.length === 0) {
      return;
    }

    // Insert all statements in order, before original statement
    for (const $newStmt of stmts) {
      $stmt.insertBefore($newStmt);
    }

    // Remove original statement
    $stmt.detach();
  }

  /**
   * @param {$statement} $stmt - A statement that will be decomposed.
   * @return {$statement[]} An array with the new statements, or an empty array if no decomposition could be made
   */
  decompose($stmt) {
    try {
      return this.decomposeStmt($stmt);
    } catch (e) {
      println(`StatementDecomposer: ${e}`);
      return [];
    }
  }

  decomposeStmt($stmt) {
    // Unsupported
    if ($stmt.instanceOf("scope") || $stmt.joinPointType === "statement") {
      debug(
        `StatementDecomposer: skipping scope or generic statement join point`
      );
      return [];
    }

    if (!this.#isValidNode($stmt)) {
      return [];
    }

    if ($stmt.instanceOf("exprStmt")) {
      return this.decomposeExprStmt($stmt);
    }

    if ($stmt.instanceOf("returnStmt")) {
      return this.decomposeReturnStmt($stmt);
    }

    if ($stmt.instanceOf("declStmt")) {
      return this.decomposeDeclStmt($stmt);
    }

    debug(
      `StatementDecomposer: not implemented for statement of type ${$stmt.joinPointType}`
    );
    return [];
  }

  decomposeExprStmt($stmt) {
    // Statement represents an expression
    const $expr = $stmt.expr;

    const { precedingStmts, succeedingStmts } = this.decomposeExpr($expr);

    return [...precedingStmts, ...succeedingStmts];
  }

  decomposeReturnStmt($stmt) {
    // Return may contain an expression
    const $expr = $stmt.returnExpr;

    if ($expr === undefined) {
      return [];
    }

    const { precedingStmts, $resultExpr } = this.decomposeExpr($expr);
    const $newReturnStmt = ClavaJoinPoints.returnStmt($resultExpr);

    return [...precedingStmts, $newReturnStmt];
  }

  decomposeDeclStmt($stmt) {
    // declStmt can have one or more declarations
    const $decls = $stmt.decls;

    return $decls.flatMap(($decl) => this.#decomposeDecl($decl));
  }

  #decomposeDecl($decl) {
    if (!$decl.instanceOf("vardecl")) {
      debug(
        `StatementDecomposer.decomposeDeclStmt: not implemented for decl of type ${$decl.joinPointType}`
      );
      return [ClavaJoinPoints.declStmt($decl)];
    }

    // If vardecl has init, decompose expression
    if ($decl.hasInit) {
      const decomposeResult = this.decomposeExpr($decl.init);
      //expr = newStmts.concat(decomposeResult.stmts);
      $decl.init = decomposeResult.$resultExpr;
      return [
        ...decomposeResult.precedingStmts,
        ClavaJoinPoints.declStmt($decl),
        ...decomposeResult.succeedingStmts,
      ];
    }

    return [ClavaJoinPoints.declStmt($decl)];
  }

  /**
   * @return {DecomposeResult}
   */
  decomposeExpr($expr) {
    if (!this.#isValidNode($expr)) {
      return new DecomposeResult([], $expr);
    }

    if ($expr.instanceOf("binaryOp")) {
      return this.decomposeBinaryOp($expr);
    }

    if ($expr.instanceOf("unaryOp")) {
      return this.decomposeUnaryOp($expr);
    }

    if ($expr.instanceOf("ternaryOp")) {
      return this.decomposeTernaryOp($expr);
    }

    if ($expr.instanceOf("call")) {
      return this.decomposeCall($expr);
    }

    if ($expr.numChildren === 0) {
      //if($expr.instanceOf("varref") || $expr.instanceOf("literal")) {
      return new DecomposeResult([], $expr);
    }

    debug(
      `StatementDecomposer: decomposition not implemented for type ${$expr.joinPointType}. Returning '${$expr.code}'as is`
    );
    return new DecomposeResult([], $expr);
  }

  decomposeCall($call) {
    const argResults = $call.args.map(($arg) => this.decomposeExpr($arg));

    const precedingStmts = argResults.flatMap((res) => res.precedingStmts);
    const succeedingStmts = argResults.flatMap((res) => res.succeedingStmts);

    const newArgs = argResults.map((res) => res.$resultExpr);
    const $newCall = this.#copyCall($call, newArgs);
    //const $newCall = ClavaJoinPoints.call($call.function, ...newArgs);
    const tempVarname = this.#newTempVarname();
    const tempVarDecl = ClavaJoinPoints.varDeclNoInit(tempVarname, $call.type);
    const tempVarAssign = ClavaJoinPoints.assign(
      ClavaJoinPoints.varRef(tempVarDecl),
      $newCall
    );

    return new DecomposeResult(
      [...precedingStmts, tempVarDecl.stmt, tempVarAssign.stmt],
      ClavaJoinPoints.varRef(tempVarDecl),
      succeedingStmts
    );
  }

  #copyCall($call, newArgs) {
    // Instance method
    if ($call.instanceOf("memberCall")) {
      // Copy node
      const $newCall = $call.copy();

      // Update args
      // TODO: use a kind of .setArgs that replaces all
      for (let i = 0; i < newArgs.length; i++) {
        $newCall.setArg(i, newArgs[i]);
      }

      return $newCall;
    }

    // Default
    return ClavaJoinPoints.call($call.function, ...newArgs);
  }

  decomposeBinaryOp($binaryOp) {
    if ($binaryOp.isAssignment) {
      return this.decomposeAssignment($binaryOp);
    }

    // Apply decompose to both sides
    const leftResult = this.decomposeExpr($binaryOp.left);
    const rightResult = this.decomposeExpr($binaryOp.right);

    // Create operation with result of decomposition
    const $newExpr = ClavaJoinPoints.binaryOp(
      $binaryOp.kind,
      leftResult.$resultExpr,
      rightResult.$resultExpr,
      $binaryOp.type
    );

    // Create declaration statement with result to new temporary variable
    const tempVarname = this.#newTempVarname();
    const tempVarDecl = ClavaJoinPoints.varDeclNoInit(
      tempVarname,
      $binaryOp.type
    );
    const tempVarAssign = ClavaJoinPoints.assign(
      ClavaJoinPoints.varRef(tempVarDecl),
      $newExpr
    );

    const precedingStmts = [
      ...leftResult.precedingStmts,
      ...rightResult.precedingStmts,
      tempVarDecl.stmt,
      tempVarAssign.stmt,
    ];
    const succeedingStmts = [
      ...leftResult.succeedingStmts,
      ...rightResult.succeedingStmts,
    ];

    return new DecomposeResult(
      precedingStmts,
      ClavaJoinPoints.varRef(tempVarDecl),
      succeedingStmts
    );

    //this.#throwNotImplemented("binary operators", kind);
  }

  decomposeAssignment($assign) {
    // Get statements of right hand-side
    const rightResult = this.decomposeExpr($assign.right);

    const $newAssign =
      $assign.operator === "="
        ? ClavaJoinPoints.assign($assign.left, rightResult.$resultExpr)
        : ClavaJoinPoints.compoundAssign(
            $assign.operator,
            $assign.left,
            rightResult.$resultExpr
          );
    const $assignExpr = ClavaJoinPoints.exprStmt($newAssign);

    return new DecomposeResult(
      [...rightResult.precedingStmts, $assignExpr],
      $assign.left,
      rightResult.succeedingStmts
    );
  }

  decomposeTernaryOp($ternaryOp) {
    const condResult = this.decomposeExpr($ternaryOp.cond);
    const trueResult = this.decomposeExpr($ternaryOp.trueExpr);
    const falseResult = this.decomposeExpr($ternaryOp.falseExpr);

    const tempVarname = this.#newTempVarname();
    const tempVarDecl = ClavaJoinPoints.varDeclNoInit(
      tempVarname,
      $ternaryOp.type
    );

    // assign the value of the new temp variable with an if-else statement
    // to maintain the semantics of only evaluating the expression that
    // falls on the right side of the ternary.
    // we do not want side-effects to be executed without regard to the branch
    // taken
    const $thenBody = ClavaJoinPoints.scope([
      ...condResult.succeedingStmts,
      ...trueResult.precedingStmts,
      ClavaJoinPoints.assign(
        ClavaJoinPoints.varRef(tempVarDecl),
        trueResult.$resultExpr
      ),
      ...trueResult.succeedingStmts,
    ]);

    const $elseBody = ClavaJoinPoints.scope([
      ...condResult.succeedingStmts,
      ...falseResult.precedingStmts,
      ClavaJoinPoints.assign(
        ClavaJoinPoints.varRef(tempVarDecl),
        falseResult.$resultExpr
      ),
      ...falseResult.succeedingStmts,
    ]);

    const $ifStmt = ClavaJoinPoints.ifStmt(
      condResult.$resultExpr,
      $thenBody,
      $elseBody
    );

    const precedingStmts = [
      tempVarDecl.stmt,
      ...condResult.precedingStmts,
      $ifStmt,
    ];

    return new DecomposeResult(
      precedingStmts,
      ClavaJoinPoints.varRef(tempVarDecl)
    );
  }

  decomposeUnaryOp($unaryOp) {
    const kind = $unaryOp.kind;
    // only decompose increment / decrement operations, separating the change
    // from the result of the change
    if (
      kind !== "post_dec" &&
      kind !== "post_inc" &&
      kind !== "pre_dec" &&
      kind !== "pre_inc"
    ) {
      return new DecomposeResult([], $unaryOp, []);
    }

    if (kind === "post_dec" || kind === "post_inc") {
      const $innerExpr = $unaryOp.operand.copy();
      const succeedingStmts = [ClavaJoinPoints.exprStmt($unaryOp)];
      return new DecomposeResult([], $innerExpr, succeedingStmts);
    }

    if (kind === "pre_dec" || kind === "pre_inc") {
      const $innerExpr = $unaryOp.operand.copy();
      const precedingStmts = [ClavaJoinPoints.exprStmt($unaryOp)];
      return new DecomposeResult(precedingStmts, $innerExpr, []);
    }
  }
}

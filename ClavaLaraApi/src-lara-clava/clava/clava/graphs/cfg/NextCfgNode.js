laraImport("lara.Check");

class NextCfgNode {
  /**
   * The AST node to process
   */
  #entryPoint;

  /**
   * Maps stmts to graph nodes
   */
  #nodes;

  /**
   * The end node of the graph
   */
  #endNode;

  constructor($entryPoint, nodes, endNode) {
    checkDefined($entryPoint);
    this.#entryPoint = $entryPoint;
    this.#nodes = nodes;
    this.#endNode = endNode;
  }

  /**
   *
   * @param {$stmt} $stmt
   *
   * @returns the next graph node that executes unconditionally after the given stmt, or end node if no statement is executed
   */
  nextExecutedNode($stmt) {
    const afterStmt = this.nextExecutedStmt($stmt);

    // If after statement is undefined, return end node
    if (afterStmt === undefined) {
      return this.#endNode;
    }

    // Get node corresponding to the after statement
    const afterNode = this.#nodes.get(afterStmt.astId);

    // If the statement does not have an associated node, this means the next node is out of scope and should be considered the end node
    if (afterNode === undefined) {
      return this.#endNode;
    }

    return afterNode;
  }

  /**
   * @return the next stmt that executes unconditionally after the given stmt, of undefined if no statement is executed
   */
  nextExecutedStmt($stmt) {
    // By definition, there is no statement executed after the entry point
    if ($stmt.equals(this.#entryPoint)) {
      return undefined;
    }

    Check.isJoinPoint($stmt, "statement");

    // If stmt is a scope, there are several special cases
    if ($stmt.getInstanceOf("scope")) {
      return this.#nextExecutedStmtAfterScope($stmt);
    }

    const rightStmts = $stmt.siblingsRight;

    // If there are statements to the right, the rightmost non-case statement is the next to be executed
    if (rightStmts.length > 0) {
      for (const sibling of rightStmts) {
        if (!sibling.getInstanceOf("case"))
          return sibling;
      }
    }

    // When there are no more statements, return what's next for the parent
    const $parent = $stmt.parent;

    if ($parent.getInstanceOf("statement")) {
      return this.nextExecutedStmt($parent);
    }
    // There are no more statements
    else if ($parent.getInstanceOf("function")) {
      return undefined;
    } else {
      throw new Error(
        "Case not defined for nodes with parent of type " +
          $parent.joinPointType
      );
    }
  }

  /**
   * @return the the next stmt that executes unconditionally after the given scope, of undefined if no statement is executed
   */
  #nextExecutedStmtAfterScope($scope) {
    // Before returning what's next to the scope of the statement, there are some special cases

    // Check if scope is a then/else of an if
    const $scopeParent = $scope.parent;
    if ($scopeParent.getInstanceOf("if")) {
      // Next stmt is what comes next of if
      return this.nextExecutedStmt($scopeParent);
    }

    // Check if scope is the body of a loop
    if ($scopeParent.getInstanceOf("loop")) {
      // Next stmt is what comes next of if

      switch ($scopeParent.kind) {
        case "while":
        case "dowhile":
          if ($scopeParent.cond === undefined) {
            throw new Error(
              "Not implemented when for loops do not have a condition statement"
            );
          }
          return $scopeParent.cond;
        case "for":
          if ($scopeParent.step === undefined) {
            throw new Error(
              "Not implemented when for loops do not have a step statement"
            );
          }
          return $scopeParent.step;
        default:
          throw new Error("Case not defined for loops of kind " + $loop.kind);
      }
    }

    // Special cases handled, check scope siblings
    const rightStmts = $scope.siblingsRight;

    // If there are statements, return next of parent
    if (rightStmts.length > 0) {
      return rightStmts[0];
    }

    // There are no statements, check parent
    const scopeParent = $scope.parent;

    // If scope parent is not a statement, there is no next statement
    if (!$scopeParent.getInstanceOf("statement")) {
      return undefined;
    }

    // Return next statement of parent statement
    return this.nextExecutedStmt($scope.parent);
  }
}

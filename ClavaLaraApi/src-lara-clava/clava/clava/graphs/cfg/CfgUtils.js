laraImport("clava.graphs.cfg.CfgNodeType");
laraImport("lara.Check");

class CfgUtils {
  /**
   * @return {boolean} true if the statement is considered a leader
   */
  static isLeader($stmt) {
    const graphNodeType = CfgUtils.getNodeType($stmt);

    return graphNodeType !== undefined;
  }

  /**
   * Returns the type of graph node based on the type of the leader statement. If this statement is not a leader, returns undefined
   */
  static getNodeType($stmt) {
    // If stmt
    if ($stmt.instanceOf("if")) {
      return CfgNodeType.IF;
    }

    // Loop stmt
    if ($stmt.instanceOf("loop")) {
      return CfgNodeType.LOOP;
    }

    // Stmt is part of loop header
    if ($stmt.isInsideLoopHeader) {
      const $loop = $stmt.parent;
      isJoinPoint($loop, "loop");

      if ($stmt.equals($loop.init)) {
        return CfgNodeType.INIT;
      } else if ($stmt.equals($loop.cond)) {
        return CfgNodeType.COND;
      } else if ($stmt.equals($loop.step)) {
        return CfgNodeType.STEP;
      } else {
        throw new Error(
          "Statement is in the header of loop at " +
            $loop.location +
            " but could not identify what part of the header: " +
            $stmt.code
        );
      }
    }

    // For stmt
    //if($stmt.instanceOf("loop") && $stmt.kind === "for") {
    //	return CfgNodeType.FOR;
    //}

    // Body stmt
    /*
		if($stmt.instanceOf("body")) {
			return CfgNodeType.BODY;
		}
		*/
    /*
		if($stmt.instanceOf("wrapperStmt")) {
			if($stmt.code.trim() === "//SCOPE_START")
				return CfgNodeType.SCOPE_START
			if($stmt.code.trim() === "//SCOPE_END")
				return CfgNodeType.SCOPE_END
			if($stmt.code.trim() === "//FOR_START")
				return CfgNodeType.FOR_START
			if($stmt.code.trim() === "//FOR_END")
				return CfgNodeType.FOR_END
			if($stmt.code.trim() === "//IF_START")
				return CfgNodeType.IF_START
			if($stmt.code.trim() === "//IF_END")
				return CfgNodeType.IF_END
			
		}
		*/
    /*
		if($stmt.instanceOf("body")) {
			const parent = $stmt.parent
			if(parent.instanceOf("function"))
				return CfgNodeType.SCOPE_DATA
		}
		*/
    // Scope stmt
    if ($stmt.instanceOf("scope")) {
      const parent = $stmt.parent;
      if (parent.instanceOf("if")) {
        if ($stmt.equals(parent.then)) {
          return CfgNodeType.THEN;
        } else if ($stmt.equals(parent.else)) {
          return CfgNodeType.ELSE;
        }
      }

      return CfgNodeType.SCOPE;
    }

    // If is the first statement of a scope and is not any of the other type of statements,
    // consider the beginning of an INST_LIST
    const $stmtParent = $stmt.parent;
    if (
      $stmtParent.instanceOf("scope") &&
      $stmt.equals($stmtParent.firstStmt)
    ) {
      return CfgNodeType.INST_LIST;
    }

    const left = $stmt.siblingsLeft;
    //println("NODE TYPE "+left)

    if (left.length > 0) {
      const lastLeft = left[left.length - 1];

      const leftNodeType = CfgUtils.getNodeType(lastLeft);
      if (
        leftNodeType !== undefined &&
        leftNodeType !== CfgNodeType.INST_LIST
      ) {
        return CfgNodeType.INST_LIST;
      }
    }

    return undefined;
    //return CfgNodeType.UNDEFINED;
    //throw new Error(`_getNodeType() not defined for statements of type '${$stmt.joinPointType}'`);
  }

  /**
   * @return the the next stmt that executes unconditionally after the given stmt, of undefined if no statement is executed
   */
  static nextExecutedStmt($stmt) {
    Check.isJoinPoint($stmt, "statement");

    // If stmt is a scope, there are several special cases
    if ($stmt.instanceOf("scope")) {
      return CfgUtils.#nextExecutedStmtAfterScope($stmt);
    }

    const rightStmts = $stmt.siblingsRight;

    // If there are statements to the right, the rightmost is the next to be executed
    if (rightStmts.length > 0) {
      return rightStmts[0];
    }

    // When there are no more statements, return what's next for the parent
    const $parent = $stmt.parent;

    if ($parent.instanceOf("statement")) {
      return CfgUtils.nextExecutedStmt($parent);
    }
    // There are no more statements
    else if ($parent.instanceOf("function")) {
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
  static #nextExecutedStmtAfterScope($scope) {
    // Before returning what's next to the scope of the statement, there are some special cases

    // Check if scope is a then/else of an if
    const $scopeParent = $scope.parent;
    if ($scopeParent.instanceOf("if")) {
      // Next stmt is what comes next of if
      return CfgUtils.nextExecutedStmt($scopeParent);
    }

    // Check if scope is the body of a loop
    if ($scopeParent.instanceOf("loop")) {
      // Next stmt is what comes next of if
      //return CfgUtils.nextExecutedStmt($scopeParent);

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

    // If there are no statements, return next of parent
    if (rightStmts.length === 0) {
      // If parent is not a statement, there is no next statement
      if (!$scope.parent.instanceOf("statement")) {
        return undefined;
      }
      return CfgUtils.nextExecutedStmt($scope.parent);
    }

    return rightStmts[0];
  }

  static getTarget(node, edgeType) {
    let target = undefined;

    for (const edge of node.connectedEdges()) {
      // Only targets of this node
      if (edge.source() !== node) {
        continue;
      }

      if (edge.data().type === edgeType) {
        if (target !== undefined) {
          throw new Error(
            "Found duplicated edge of type '" +
              edgeType +
              "' in node " +
              node.data()
          );
        }

        target = edge.target();
      }
    }

    return target;
  }
}

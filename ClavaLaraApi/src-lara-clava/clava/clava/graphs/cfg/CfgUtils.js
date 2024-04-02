laraImport("clava.graphs.cfg.CfgNodeType");
laraImport("lara.Check");

class CfgUtils {
  /**
   * @param {joinpoint} $stmt the statement join point
   * @return {boolean} true if the statement is considered a leader
   */
  static isLeader($stmt) {
    const graphNodeType = CfgUtils.getNodeType($stmt);

    return graphNodeType !== undefined;
  }

  /**
   * Returns the type of graph node based on the type of the leader statement. If this statement is not a leader, returns undefined
   * @param {joinpoint} $stmt the statement join point
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

    // Break stmt
    if ($stmt.instanceOf("break")) {
      return CfgNodeType.BREAK;
    }

    // Continue stmt
    if ($stmt.instanceOf("continue")) {
      return CfgNodeType.CONTINUE;
    }

    // Switch stmt
    if ($stmt.instanceOf("switch")) {
      return CfgNodeType.SWITCH;
    }

    // Case stmt
    if ($stmt.instanceOf("case")) {
      return CfgNodeType.CASE;
    }

    // Goto stmt
    if ($stmt.instanceOf("gotoStmt")) {
      return CfgNodeType.GOTO;
    }

    // Label stmt
    if ($stmt.instanceOf("labelStmt")) {
      return CfgNodeType.LABEL;
    }

    // Return stmt
    if ($stmt.instanceOf("returnStmt")) {
      return CfgNodeType.RETURN;
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
    //console.log("NODE TYPE "+left)

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

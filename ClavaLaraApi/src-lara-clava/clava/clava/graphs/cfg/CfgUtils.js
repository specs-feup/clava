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

    //Case stmt
    if ($stmt.instanceOf("case")) {
      return CfgNodeType.CASE;
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

  /**
   * Returns all the statements of a switch
   * @param {joinpoint} $switchStmt the switch statement join point
   */
  static getSwitchStmts($switchStmt) {
    return $switchStmt.children[1].children;
  }

  /**
   * @param {joinpoint} $switchStmt the switch statement join point
   * @param {Map} nodes maps statements to graph nodes
   * @return {boolean} true if the default case appears in the middle of the switch statement, rather than at the end 
   */
  static getCaseStmtIndex($caseStmt, nodes) {
    const $switchStmts = this.getSwitchStmts($caseStmt.ancestor("switch"));
    let caseIndex = undefined;

    for(let i=0; i < $switchStmts.length; i++) {
      const stmtAstId = $switchStmts[i].astId;
      const stmtNode = nodes.get($switchStmts[i].astId);

      if (stmtNode.data().type === CfgNodeType.CASE && stmtAstId === $caseStmt.astId){
        caseIndex = i;
        break;
      }
    }
    return caseIndex;
  }

  /**
   * Returns the case statement that  @param $caseStmt
   * @param {joinpoint} $caseStmt is the case statement join point
   * @param {Map} nodes maps statements to graph nodes
   */
  static getNextCaseStmt($caseStmt, nodes) {
    const $switchStmts = this.getSwitchStmts($caseStmt.ancestor("switch"));
    const caseIndex = this.getCaseStmtIndex($caseStmt, nodes);

    for (let i=caseIndex + 1; i < $switchStmts.length; i++) {
      const currentStmtNode = nodes.get($switchStmts[i].astId);

      if (currentStmtNode.data().type ===  CfgNodeType.CASE)
        return $switchStmts[i];
    }

    // The considered case statement is the final case of the corresponding switch
    return undefined;
  }
}

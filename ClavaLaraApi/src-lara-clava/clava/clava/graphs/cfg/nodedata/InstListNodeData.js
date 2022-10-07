laraImport("clava.graphs.cfg.CfgNodeData");
laraImport("clava.graphs.cfg.CfgNodeType");
laraImport("clava.graphs.cfg.CfgUtils");

class InstListNodeData extends CfgNodeData {
  #stmts;

  constructor($stmt, id, $entryPoint) {
    super(CfgNodeType.INST_LIST, $stmt, id);

    this.#stmts = [];

    // Given statement is start of the list
    this.#stmts.push($stmt);

    // Add non-leader statements corresponding to this list, unless this node is the starting point
    const rightNodes = !$stmt.equals($entryPoint) ? $stmt.siblingsRight : [];

    for (const $right of rightNodes) {
      if (!CfgUtils.isLeader($right)) {
        this.#stmts.push($right);
      } else {
        break;
      }
    }
  }

  /**
   * Returns all the statements of this instruction list.
   */
  get stmts() {
    return this.#stmts;
  }

  set stmts(stmts) {
    this.#stmts = stmts;

    // When setting statements, the base statement changes to the first of the new list
    this.nodeStmt = this.#stmts.length > 0 ? this.#stmts[0] : undefined;
  }

  getLastStmt() {
    if (this.#stmts.length === 0) {
      return undefined;
    }

    return this.#stmts[this.#stmts.length - 1];
  }

  toString() {
    let code = "";

    for (const $stmt of this.#stmts) {
      code += $stmt.code + "\n";
    }

    return code;
  }
}

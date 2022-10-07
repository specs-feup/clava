laraImport("clava.graphs.cfg.CfgNodeData");
laraImport("clava.graphs.cfg.CfgNodeType");

class HeaderData extends CfgNodeData {
  //#stmt

  constructor($stmt, nodeType, id) {
    super(nodeType, $stmt, id);

    //this.#stmt = $stmt
  }

  /*
	getStmt() {
		return this.#stmt;
	}
    */

  toString() {
    return this.name + ": " + this.nodeStmt.code;
  }

  isBranch() {
    if (this.type === CfgNodeType.COND) {
      return true;
    }

    return false;
  }
}

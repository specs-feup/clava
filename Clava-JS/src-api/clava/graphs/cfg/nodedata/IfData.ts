laraImport("clava.graphs.cfg.CfgNodeData");
laraImport("clava.graphs.cfg.CfgNodeType");

class IfData extends CfgNodeData {
  //#stmt

  constructor($stmt, id) {
    super(CfgNodeType.IF, $stmt, id);

    //this.#stmt = $stmt
  }

  get if() {
    return this.nodeStmt;
    //return this.#stmt;
  }

  toString() {
    return "if(" + this.if.cond.code + ")";
  }

  isBranch() {
    return true;
  }
}

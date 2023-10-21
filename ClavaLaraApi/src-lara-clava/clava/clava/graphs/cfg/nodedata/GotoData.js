laraImport("clava.graphs.cfg.CfgNodeData");
laraImport("clava.graphs.cfg.CfgNodeType");

class GotoData extends CfgNodeData {
  //#stmt

  constructor($stmt, id) {
    super(CfgNodeType.GOTO, $stmt, id);

    //this.#stmt = $stmt
  }

  toString() {
    return this.nodeStmt.code;
  }

}

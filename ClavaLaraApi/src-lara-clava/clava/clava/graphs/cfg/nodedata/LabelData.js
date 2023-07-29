laraImport("clava.graphs.cfg.CfgNodeData");
laraImport("clava.graphs.cfg.CfgNodeType");

class LabelData extends CfgNodeData {
  //#stmt

  constructor($stmt, id) {
    super(CfgNodeType.LABEL, $stmt, id);

    //this.#stmt = $stmt
  }

  toString() {
    return this.nodeStmt.code;
  }

}

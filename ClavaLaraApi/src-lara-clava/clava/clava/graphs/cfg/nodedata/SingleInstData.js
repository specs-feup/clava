laraImport("clava.graphs.cfg.CfgNodeData");
laraImport("clava.graphs.cfg.CfgNodeType");

class SingleInstData extends CfgNodeData {

  constructor($stmt, id) {
    super(CfgNodeType.SINGLE_INST, $stmt, id);
  }

  toString() {
    return this.nodeStmt.code;
  }
}

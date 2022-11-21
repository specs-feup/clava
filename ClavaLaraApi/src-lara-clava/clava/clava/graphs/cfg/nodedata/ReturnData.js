laraImport("clava.graphs.cfg.CfgNodeData");
laraImport("clava.graphs.cfg.CfgNodeType");

class ReturnData extends CfgNodeData {
  constructor($stmt, id) {
    super(CfgNodeType.RETURN, $stmt, id);
  }

  get returnStmt() {
    return this.nodeStmt;
  }

  toString() {
    const returnExpr = this.returnStmt.returnExpr;
    return (
      "return" + (returnExpr === undefined ? "" : " " + returnExpr.code) + ";"
    );
  }
}

laraImport("clava.graphs.cfg.CfgNodeData");
laraImport("clava.graphs.cfg.CfgNodeType");

class CaseData extends CfgNodeData {
  //#stmt

  constructor($stmt, id) {
    super(CfgNodeType.CASE, $stmt, id);

    //this.#stmt = $stmt
  }

  get case() {
    return this.nodeStmt;
    //return this.#stmt;
  }

  toString() {
    return this.case.isDefault ? "default" : "case(" + this.case.children[0].code + ")";
  }

  isBranch() {
    return true;
  }
}

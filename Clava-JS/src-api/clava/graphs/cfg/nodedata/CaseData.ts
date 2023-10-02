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
    if (this.case.isDefault)
      return "default";
    return "case " + this.case.values.map(value => value.code).join(" || ");
  }

  isBranch() {
    return true;
  }
}

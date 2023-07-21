laraImport("clava.graphs.cfg.CfgNodeData");
laraImport("clava.graphs.cfg.CfgNodeType");

class SwitchData extends CfgNodeData {
  //#stmt

  constructor($stmt, id) {
    super(CfgNodeType.SWITCH, $stmt, id);

    //this.#stmt = $stmt
  }

  get switch() {
    return this.nodeStmt;
    //return this.#stmt;
  }

  toString() {
    return "switch(" + this.switch.condition.code + ")";
  }

  isBranch() {
    return true;
  }
}

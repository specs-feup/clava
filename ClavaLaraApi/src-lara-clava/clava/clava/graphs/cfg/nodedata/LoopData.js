laraImport("clava.graphs.cfg.CfgNodeData");
laraImport("clava.graphs.cfg.CfgNodeType");

class LoopData extends CfgNodeData {
  //#stmt

  constructor($stmt, id) {
    super(CfgNodeType.LOOP, $stmt, id);

    //this.#stmt = $stmt
  }

  get loop() {
    return this.nodeStmt;
  }

  toString() {
    return "Loop: " + this.loop.kind;
  }
}

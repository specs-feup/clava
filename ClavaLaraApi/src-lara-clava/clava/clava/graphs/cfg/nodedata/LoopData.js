laraImport("clava.graphs.cfg.CfgNodeData");
laraImport("clava.graphs.cfg.CfgNodeType");

class LoopData extends CfgNodeData {

    //#stmt

    constructor($stmt){
        super(CfgNodeType.LOOP, $stmt)

        //this.#stmt = $stmt

    }

	get loop() {
		return this.nodeStmt;
	}

    toString() {
        return "Loop: " + this.loop.kind;
    }


}
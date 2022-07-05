laraImport("clava.graphs.cfg.CfgNodeData");
laraImport("clava.graphs.cfg.CfgNodeType");

class IfData extends CfgNodeData {

    //#stmt

    constructor($stmt){
        super(CfgNodeType.IF, $stmt)

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
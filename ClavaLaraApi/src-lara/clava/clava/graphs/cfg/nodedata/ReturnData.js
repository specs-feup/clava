import CfgNodeData from "../CfgNodeData.js";
import CfgNodeType from "../CfgNodeType.js";
export default class ReturnData extends CfgNodeData {
    constructor($stmt, id) {
        super(CfgNodeType.RETURN, $stmt, id);
    }
    get returnStmt() {
        return this.nodeStmt;
    }
    toString() {
        const returnExpr = this.returnStmt?.returnExpr;
        return ("return" + (returnExpr === undefined ? "" : " " + returnExpr.code) + ";");
    }
}
//# sourceMappingURL=ReturnData.js.map
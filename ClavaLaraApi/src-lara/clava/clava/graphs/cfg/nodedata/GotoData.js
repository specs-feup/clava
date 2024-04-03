import CfgNodeData from "../CfgNodeData.js";
import CfgNodeType from "../CfgNodeType.js";
export default class GotoData extends CfgNodeData {
    constructor($stmt, id) {
        super(CfgNodeType.GOTO, $stmt, id);
    }
    get goto() {
        return this.nodeStmt;
    }
    toString() {
        if (this.goto === undefined) {
            return super.toString();
        }
        return this.goto.code;
    }
}
//# sourceMappingURL=GotoData.js.map
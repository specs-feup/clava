import CfgNodeData from "../CfgNodeData.js";
import CfgNodeType from "../CfgNodeType.js";
export default class HeaderData extends CfgNodeData {
    constructor($stmt, nodeType, id) {
        super(nodeType, $stmt, id);
    }
    toString() {
        if (this.nodeStmt === undefined) {
            return super.toString();
        }
        return this.name + ": " + this.nodeStmt.code;
    }
    isBranch() {
        if (this.type === CfgNodeType.COND) {
            return true;
        }
        return false;
    }
}
//# sourceMappingURL=HeaderData.js.map
import CfgNodeData from "../CfgNodeData.js";
import CfgNodeType from "../CfgNodeType.js";
export default class SwitchData extends CfgNodeData {
    constructor($stmt, id) {
        super(CfgNodeType.SWITCH, $stmt, id);
    }
    get switch() {
        return this.nodeStmt;
    }
    toString() {
        if (this.switch === undefined) {
            return super.toString();
        }
        return "switch(" + this.switch.condition.code + ")";
    }
    isBranch() {
        return true;
    }
}
//# sourceMappingURL=SwitchData.js.map
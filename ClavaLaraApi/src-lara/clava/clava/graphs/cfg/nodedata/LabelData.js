import CfgNodeData from "../CfgNodeData.js";
import CfgNodeType from "../CfgNodeType.js";
export default class LabelData extends CfgNodeData {
    constructor($stmt, id) {
        super(CfgNodeType.LABEL, $stmt, id);
    }
    get label() {
        return this.nodeStmt;
    }
    toString() {
        if (this.label === undefined) {
            return super.toString();
        }
        return this.label.code;
    }
}
//# sourceMappingURL=LabelData.js.map
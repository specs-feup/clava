import CfgNodeData from "../CfgNodeData.js";
import CfgNodeType from "../CfgNodeType.js";
export default class CaseData extends CfgNodeData {
    constructor($stmt, id) {
        super(CfgNodeType.CASE, $stmt, id);
    }
    get case() {
        return this.nodeStmt;
    }
    toString() {
        if (this.case === undefined) {
            return super.toString();
        }
        if (this.case.isDefault) {
            return "default";
        }
        return "case " + this.case.values.map((value) => value.code).join(" || ");
    }
    isBranch() {
        return true;
    }
}
//# sourceMappingURL=CaseData.js.map
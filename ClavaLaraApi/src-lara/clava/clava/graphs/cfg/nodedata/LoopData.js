import CfgNodeData from "../CfgNodeData.js";
import CfgNodeType from "../CfgNodeType.js";
export default class LoopData extends CfgNodeData {
    constructor($stmt, id) {
        super(CfgNodeType.LOOP, $stmt, id);
    }
    get loop() {
        return this.nodeStmt;
    }
    toString() {
        if (this.loop === undefined) {
            return super.toString();
        }
        return `Loop: ${this.loop.kind}`;
    }
}
//# sourceMappingURL=LoopData.js.map
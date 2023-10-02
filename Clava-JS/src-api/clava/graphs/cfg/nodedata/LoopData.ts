import { Loop } from "../../../../Joinpoints.js";
import CfgNodeData from "../CfgNodeData.js";
import CfgNodeType from "../CfgNodeType.js";

export default class LoopData extends CfgNodeData<Loop> {
  constructor($stmt?: Loop, id?: string) {
    super(CfgNodeType.LOOP, $stmt, id);
  }

  get loop() {
    return this.nodeStmt;
  }

  toString(): string {
    if (this.loop === undefined) {
      return super.toString();
    }

    return `Loop: ${this.loop.kind}`;
  }
}

import { Loop } from "../../../../Joinpoints.ts";
import CfgNodeData from "../CfgNodeData.ts";
import CfgNodeType from "../CfgNodeType.ts";

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

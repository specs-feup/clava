import { GotoStmt } from "../../../../Joinpoints.ts";
import CfgNodeData from "../CfgNodeData.ts";
import CfgNodeType from "../CfgNodeType.ts";

export default class GotoData extends CfgNodeData<GotoStmt> {
  constructor($stmt?: GotoStmt, id?: string) {
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

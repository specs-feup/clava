import { GotoStmt } from "../../../../Joinpoints.js";
import CfgNodeData from "../CfgNodeData.js";
import CfgNodeType from "../CfgNodeType.js";

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

import { If } from "../../../../Joinpoints.js";
import CfgNodeData from "../CfgNodeData.js";
import CfgNodeType from "../CfgNodeType.js";

export default class IfData extends CfgNodeData<If> {
  constructor($stmt?: If, id?: string) {
    super(CfgNodeType.IF, $stmt, id);
  }

  get if() {
    return this.nodeStmt;
  }

  toString() {
    if (this.if === undefined) {
      return super.toString();
    }

    return "if(" + this.if.cond.code + ")";
  }

  isBranch() {
    return true;
  }
}

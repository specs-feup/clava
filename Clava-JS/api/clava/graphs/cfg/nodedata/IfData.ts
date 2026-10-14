import { If } from "../../../../Joinpoints.ts";
import CfgNodeData from "../CfgNodeData.ts";
import CfgNodeType from "../CfgNodeType.ts";

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

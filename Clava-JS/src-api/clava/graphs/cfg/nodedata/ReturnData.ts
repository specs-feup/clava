import { ReturnStmt } from "../../../../Joinpoints.js";
import CfgNodeData from "../CfgNodeData.js";
import CfgNodeType from "../CfgNodeType.js";

export default class ReturnData extends CfgNodeData<ReturnStmt> {
  constructor($stmt?: ReturnStmt, id?: string) {
    super(CfgNodeType.RETURN, $stmt, id);
  }

  get returnStmt() {
    return this.nodeStmt;
  }

  toString() {
    const returnExpr = this.returnStmt?.returnExpr;
    return (
      "return" + (returnExpr === undefined ? "" : " " + returnExpr.code) + ";"
    );
  }
}

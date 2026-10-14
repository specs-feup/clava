import { ReturnStmt } from "../../../../Joinpoints.ts";
import CfgNodeData from "../CfgNodeData.ts";
import CfgNodeType from "../CfgNodeType.ts";

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

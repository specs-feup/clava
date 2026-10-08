import { Statement } from "../../../../Joinpoints.ts";
import CfgNodeData from "../CfgNodeData.ts";
import CfgNodeType from "../CfgNodeType.ts";

export default class HeaderData extends CfgNodeData<Statement> {
  constructor($stmt: Statement | undefined, nodeType: CfgNodeType, id: string | undefined) {
    super(nodeType, $stmt, id);
  }

  toString(): string {
    if (this.nodeStmt === undefined) {
      return super.toString();
    }

    return this.name + ": " + this.nodeStmt.code;
  }

  isBranch(): boolean {
    if (this.type === CfgNodeType.COND) {
      return true;
    }

    return false;
  }
}

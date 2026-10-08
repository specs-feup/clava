import { LabelStmt } from "../../../../Joinpoints.ts";
import CfgNodeData from "../CfgNodeData.ts";
import CfgNodeType from "../CfgNodeType.ts";

export default class LabelData extends CfgNodeData<LabelStmt> {
  constructor($stmt?: LabelStmt, id?: string) {
    super(CfgNodeType.LABEL, $stmt, id);
  }

  get label() {
    return this.nodeStmt;
  }

  toString() {
    if (this.label === undefined) {
      return super.toString();
    }

    return this.label.code;
  }
}

import { LabelStmt } from "../../../../Joinpoints.js";
import CfgNodeData from "../CfgNodeData.js";
import CfgNodeType from "../CfgNodeType.js";

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

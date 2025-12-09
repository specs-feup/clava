import { Case } from "../../../../Joinpoints.js";
import CfgNodeData from "../CfgNodeData.js";
import CfgNodeType from "../CfgNodeType.js";

export default class CaseData extends CfgNodeData<Case> {
  constructor($stmt?: Case, id?: string) {
    super(CfgNodeType.CASE, $stmt, id);
  }

  get case() {
    return this.nodeStmt;
  }

  toString(): string {
    if (this.case === undefined) {
      return super.toString();
    }

    if (this.case.isDefault) {
      return "default";
    }

    return "case " + this.case.values.map((value) => value.code).join(" || ");
  }

  isBranch(): true {
    return true;
  }
}

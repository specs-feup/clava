import { Switch } from "../../../../Joinpoints.ts";
import CfgNodeData from "../CfgNodeData.ts";
import CfgNodeType from "../CfgNodeType.ts";

export default class SwitchData extends CfgNodeData<Switch> {
  constructor($stmt?: Switch, id?: string) {
    super(CfgNodeType.SWITCH, $stmt, id);
  }

  get switch() {
    return this.nodeStmt;
  }

  toString() {
    if (this.switch === undefined) {
      return super.toString();
    }

    return "switch(" + this.switch.condition.code + ")";
  }

  isBranch() {
    return true;
  }
}

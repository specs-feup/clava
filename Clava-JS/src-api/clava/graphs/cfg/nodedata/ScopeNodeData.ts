import { Scope } from "../../../../Joinpoints.js";
import CfgNodeData from "../CfgNodeData.js";
import CfgNodeType from "../CfgNodeType.js";

export default class ScopeNodeData extends CfgNodeData<Scope> {
  private scopeStmt;

  constructor($scope: Scope, nodeType = CfgNodeType.SCOPE, id?: string) {
    super(nodeType, $scope, id);

    this.scopeStmt = $scope;
  }

  get scope() {
    return this.scopeStmt;
  }
}

import { Scope } from "../../../../Joinpoints.ts";
import CfgNodeData from "../CfgNodeData.ts";
import CfgNodeType from "../CfgNodeType.ts";

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

import CfgNodeData from "../CfgNodeData.js";
import CfgNodeType from "../CfgNodeType.js";
export default class ScopeNodeData extends CfgNodeData {
    scopeStmt;
    constructor($scope, nodeType = CfgNodeType.SCOPE, id) {
        super(nodeType, $scope, id);
        this.scopeStmt = $scope;
    }
    get scope() {
        return this.scopeStmt;
    }
}
//# sourceMappingURL=ScopeNodeData.js.map
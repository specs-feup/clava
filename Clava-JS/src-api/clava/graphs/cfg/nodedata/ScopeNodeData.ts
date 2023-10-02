laraImport("clava.graphs.cfg.CfgNodeData");
laraImport("clava.graphs.cfg.CfgNodeType");

class ScopeNodeData extends CfgNodeData {
  #scope;

  constructor($scope, nodeType = CfgNodeType.SCOPE, id) {
    super(nodeType, $scope, id);

    isJoinPoint($scope, "scope");

    this.#scope = $scope;

    // This is just a marker node, so it has no statements
    /*
        for(const $stmt of $scope.stmts) {
            if(!CfgUtils.isLeader($stmt)) {
                this.addStmt($stmt);
            } else {
                break;
            }            
        }
        */
  }

  get scope() {
    return this.#scope;
  }

  /*
    toString() {
    return this.name;
    }
    */
}

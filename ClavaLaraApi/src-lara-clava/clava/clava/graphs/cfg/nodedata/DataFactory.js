laraImport("clava.graphs.cfg.CfgNodeType");
laraImport("clava.graphs.cfg.CfgNodeData");
laraImport("clava.graphs.cfg.nodedata.InstListNodeData");
laraImport("clava.graphs.cfg.nodedata.ScopeNodeData");
laraImport("clava.graphs.cfg.nodedata.LoopData");
laraImport("clava.graphs.cfg.nodedata.HeaderData");
laraImport("clava.graphs.cfg.nodedata.IfData");
laraImport("clava.graphs.cfg.nodedata.SwitchData");
laraImport("clava.graphs.cfg.nodedata.CaseData");
laraImport("clava.graphs.cfg.nodedata.ReturnData");

class DataFactory {
  #entryPoint;

  constructor($entryPoint) {
    checkDefined($entryPoint);

    this.#entryPoint = $entryPoint;
  }

  newData(cfgNodeType, $stmt, id) {
    switch (cfgNodeType) {
      case CfgNodeType.INST_LIST:
        return new InstListNodeData($stmt, id, this.#entryPoint);
      case CfgNodeType.THEN:
      case CfgNodeType.ELSE:
      case CfgNodeType.SCOPE:
        return new ScopeNodeData($stmt, cfgNodeType, id);
      case CfgNodeType.INIT:
      case CfgNodeType.COND:
      case CfgNodeType.STEP:
        return new HeaderData($stmt, cfgNodeType, id);
      case CfgNodeType.IF:
        return new IfData($stmt, id);
      case CfgNodeType.LOOP:
        return new LoopData($stmt, id);
      case CfgNodeType.SWITCH:
        return new SwitchData($stmt, id);
      case CfgNodeType.CASE:
        return new CaseData($stmt, id);
      case CfgNodeType.RETURN:
        return new ReturnData($stmt, id);
      default:
        return new CfgNodeData(cfgNodeType, $stmt, id);
    }
  }
}

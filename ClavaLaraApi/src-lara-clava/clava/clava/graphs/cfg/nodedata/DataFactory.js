laraImport("clava.graphs.cfg.CfgNodeType");
laraImport("clava.graphs.cfg.CfgNodeData");
laraImport("clava.graphs.cfg.nodedata.InstListNodeData");
laraImport("clava.graphs.cfg.nodedata.ScopeNodeData");
laraImport("clava.graphs.cfg.nodedata.LoopData");
laraImport("clava.graphs.cfg.nodedata.HeaderData");
laraImport("clava.graphs.cfg.nodedata.IfData");

class DataFactory {

	static newData(cfgNodeType, $stmt, id) {

		switch(cfgNodeType) {
			case CfgNodeType.INST_LIST:
				return new InstListNodeData($stmt); 
			case CfgNodeType.THEN:
			case CfgNodeType.ELSE:
			case CfgNodeType.SCOPE:
				return new ScopeNodeData($stmt, cfgNodeType);	
			case CfgNodeType.INIT:
			case CfgNodeType.COND:
			case CfgNodeType.STEP:
				return new HeaderData($stmt, cfgNodeType);
			case CfgNodeType.IF:
				return new IfData($stmt);
			case CfgNodeType.LOOP:
				return new LoopData($stmt);
			default:		
				return new CfgNodeData(cfgNodeType, $stmt, id);		
		}
	}

}
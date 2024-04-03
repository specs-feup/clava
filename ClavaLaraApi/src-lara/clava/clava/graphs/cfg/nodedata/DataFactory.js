import { Case, GotoStmt, If, LabelStmt, Loop, ReturnStmt, Scope, Switch, } from "../../../../Joinpoints.js";
import CfgNodeData from "../CfgNodeData.js";
import CfgNodeType from "../CfgNodeType.js";
import CaseData from "./CaseData.js";
import GotoData from "./GotoData.js";
import HeaderData from "./HeaderData.js";
import IfData from "./IfData.js";
import InstListNodeData from "./InstListNodeData.js";
import LabelData from "./LabelData.js";
import LoopData from "./LoopData.js";
import ReturnData from "./ReturnData.js";
import ScopeNodeData from "./ScopeNodeData.js";
import SwitchData from "./SwitchData.js";
export default class DataFactory {
    entryPoint;
    constructor($entryPoint) {
        this.entryPoint = $entryPoint;
    }
    newData(cfgNodeType, $stmt, id, splitInstList) {
        switch (cfgNodeType) {
            case CfgNodeType.INST_LIST:
                return new InstListNodeData($stmt, id, this.entryPoint, splitInstList);
            case CfgNodeType.THEN:
            case CfgNodeType.ELSE:
            case CfgNodeType.SCOPE:
                if (!($stmt instanceof Scope)) {
                    throw new Error("Expected statement to be a Scope");
                }
                return new ScopeNodeData($stmt, cfgNodeType, id);
            case CfgNodeType.INIT:
            case CfgNodeType.COND:
            case CfgNodeType.STEP:
                return new HeaderData($stmt, cfgNodeType, id);
            case CfgNodeType.IF:
                if (!($stmt instanceof If)) {
                    throw new Error("Expected statement to be an If statement");
                }
                return new IfData($stmt, id);
            case CfgNodeType.LOOP:
                if (!($stmt instanceof Loop)) {
                    throw new Error("Expected statement to be a Loop");
                }
                return new LoopData($stmt, id);
            case CfgNodeType.SWITCH:
                if (!($stmt instanceof Switch)) {
                    throw new Error("Expected statement to be a Switch");
                }
                return new SwitchData($stmt, id);
            case CfgNodeType.CASE:
                if (!($stmt instanceof Case)) {
                    throw new Error("Expected statement to be a Case");
                }
                return new CaseData($stmt, id);
            case CfgNodeType.GOTO:
                if (!($stmt instanceof GotoStmt)) {
                    throw new Error("Expected statement to be a GotoStmt");
                }
                return new GotoData($stmt, id);
            case CfgNodeType.LABEL:
                if (!($stmt instanceof LabelStmt)) {
                    throw new Error("Expected statement to be a LabelStmt");
                }
                return new LabelData($stmt, id);
            case CfgNodeType.RETURN:
                if (!($stmt instanceof ReturnStmt)) {
                    throw new Error("Expected statement to be a Return Statement");
                }
                return new ReturnData($stmt, id);
            default:
                return new CfgNodeData(cfgNodeType, $stmt, id);
        }
    }
}
//# sourceMappingURL=DataFactory.js.map
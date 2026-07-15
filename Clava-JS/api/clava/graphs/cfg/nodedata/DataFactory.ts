import {
  Case,
  GotoStmt,
  If,
  LabelStmt,
  Loop,
  ReturnStmt,
  Scope,
  Statement,
  Switch,
} from "../../../../Joinpoints.ts";
import CfgNodeData from "../CfgNodeData.ts";
import CfgNodeType from "../CfgNodeType.ts";
import CaseData from "./CaseData.ts";
import GotoData from "./GotoData.ts";
import HeaderData from "./HeaderData.ts";
import IfData from "./IfData.ts";
import InstListNodeData from "./InstListNodeData.ts";
import LabelData from "./LabelData.ts";
import LoopData from "./LoopData.ts";
import ReturnData from "./ReturnData.ts";
import ScopeNodeData from "./ScopeNodeData.ts";
import SwitchData from "./SwitchData.ts";

export default class DataFactory {
  private entryPoint: Statement;

  constructor($entryPoint: Statement) {
    this.entryPoint = $entryPoint;
  }

  newData(
    cfgNodeType: CfgNodeType,
    $stmt: Statement | undefined,
    id: string | undefined,
    splitInstList: boolean
  ) {
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

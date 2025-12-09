import { Statement } from "../../../../Joinpoints.js";
import CfgNodeData from "../CfgNodeData.js";
import CfgNodeType from "../CfgNodeType.js";
import CfgUtils from "../CfgUtils.js";

type T = Statement;

export default class InstListNodeData extends CfgNodeData<T> {
  private stmtArray: T[] = [];

  constructor(
    $stmt: T | undefined,
    id: string | undefined,
    $entryPoint: T,
    splitInstList: boolean
  ) {
    super(CfgNodeType.INST_LIST, $stmt, id);

    // Given statement is start of the list
    if ($stmt !== undefined) {
      this.stmtArray.push($stmt);

      if (!splitInstList) {
        // Add non-leader statements corresponding to this list, unless this node is the starting point
        const rightNodes = !$stmt.equals($entryPoint)
          ? $stmt.siblingsRight
          : [];

        for (const $jp of rightNodes) {
          const $right = $jp as Statement;
          if (!CfgUtils.isLeader($right)) {
            this.stmtArray.push($right);
          } else {
            break;
          }
        }
      }
    }
  }

  /**
   * Returns all the statements of this instruction list.
   */
  get stmts() {
    return this.stmtArray;
  }

  set stmts(stmts) {
    this.stmtArray = stmts;

    // When setting statements, the base statement changes to the first of the new list
    this.nodeStmt = this.stmtArray.length > 0 ? this.stmtArray[0] : undefined;
  }

  getLastStmt() {
    if (this.stmtArray.length === 0) {
      return undefined;
    }

    return this.stmtArray[this.stmtArray.length - 1];
  }

  toString() {
    let code = "";

    for (const $stmt of this.stmtArray) {
      code += $stmt.code + "\n";
    }

    return code;
  }
}

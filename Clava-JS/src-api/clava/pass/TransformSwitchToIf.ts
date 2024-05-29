import Query from "lara-js/api/weaver/Query.js";
import ClavaJoinPoints from "../ClavaJoinPoints.js";
import SimplePass from "lara-js/api/lara/pass/SimplePass.js";
import PassResult from "lara-js/api/lara/pass/results/PassResult.js";
import {
  Break,
  Case,
  GotoStmt,
  If,
  Joinpoint,
  Statement,
  Switch,
} from "../../Joinpoints.js";
import { LaraJoinPoint } from "lara-js/api/LaraJoinPoint.js";

/**
 * Transforms a switch statement into an if statement.
 *
 * This means that code like this:
 *
 * ```c
 *	int num = 1, a;
 *
 *	switch (num) {
 *	    case 1:
 *          a = 10;
 *	   default:
 *	   	    a = 30;
 *	        break;
 *	   case 2:
 *	       a = 20;
 *	       break;
 *     case 3 ... 7:
 *         a = 30;
 *	}
 * ```
 *
 * Will be transformed into:
 *
 * ```c
 *  int num = 1, a;
 *
 *  if (num == 1)
 *      goto case_1;
 *  else if (num == 2)
 *      goto case_2;
 *  else if (num >= 3 && num <= 7)
 *       goto case_3_7;
 *  else
 *       goto case_default;
 *
 *  case_1:
 *      a = 10;
 *  case_default:
 *      a = 80;
 *      goto switch_exit;
 *  case_2:
 *      a = 20;
 *      goto switch_exit;
 *  case_3_7:
 *      a = 30;
 *
 *  switch_exit:
 *  ;
 * ```
 */
export default class TransformSwitchToIf extends SimplePass {
  /**
   * Name of the pass
   */
  protected _name: string = "TransformSwitchToIf";

  /**
   * Maps each case statement id to the corresponding label statement
   */
  private caseLabels: Map<string, Statement> = new Map();

  /**
   * A list with the corresponding if statement for each case in the switch statement. For the default case, the list keeps its goto statement
   */
  private caseIfStmts: (If | GotoStmt)[] = [];

  /**
   * If true, uses deterministic ids for the labels (e.g. switch_exit_0, sw1_case_3...). Otherwise, uses $jp.astId whenever possible.
   */
  private deterministicIds: boolean;

  /**
   * Current switch id, in case deterministic ids are used
   */
  private currentId: number;

  /**
   * @param deterministicIds - If true, uses deterministic ids for the labels (e.g. switch_exit_0, sw1_case_3, ...). Otherwise, uses $jp.astId whenever possible.
   */
  constructor(deterministicIds: boolean = false) {
    super();
    this.deterministicIds = deterministicIds;
    this.currentId = 0;
  }

  matchJoinpoint($jp: Joinpoint) {
    return $jp instanceof Switch;
  }

  /**
   * Transformation to be applied to matching joinpoints
   * @override
   * @param $jp - Join point to transform
   */
  transformJoinpoint($jp: Switch) {
    this.currentId++;

    const exitName =
      "switch_exit_" + (this.deterministicIds ? this.currentId : $jp.astId);
    const $switchExitLabel = ClavaJoinPoints.labelDecl(exitName);
    const $switchExitLabelStmt = ClavaJoinPoints.labelStmt($switchExitLabel);
    const $switchExitGoTo = ClavaJoinPoints.gotoStmt($switchExitLabel);

    // Insert the switch exit label and an empty statement if needed
    $jp.insertAfter($switchExitLabelStmt);
    if ($switchExitLabelStmt.isLast)
      $switchExitLabelStmt.insertAfter(ClavaJoinPoints.emptyStmt());

    this.computeIfAndLabels($jp);

    if ($jp.hasDefaultCase) this.moveDefaultToEnd();
    else this.caseIfStmts.push($switchExitGoTo);

    $jp.insertBefore(this.caseIfStmts[0]);

    this.linkIfStmts();
    this.replaceBreakWithGoto($jp, $switchExitGoTo);
    this.addLabelsAndInstructions($jp);

    $jp.detach();
    return new PassResult(this, this.caseIfStmts[0]);
  }

  /**
   * Generates the label based on the switch ID and the values of the provided case statement.
   * If no switch ID is provided, a generic label based on the case statement's AST ID is returned.
   * @param $caseStmt - The case statement
   * @returns The generated label name for the provided case statement
   */
  private computeLabelName($caseStmt: Case): string {
    if (!this.deterministicIds) return "case_" + $caseStmt.astId;

    let labelName = "sw" + this.currentId;
    if ($caseStmt.isDefault) labelName += "_default";
    else if ($caseStmt.values.length == 1)
      labelName += `_case_${$caseStmt.values[0].code}`;
    else
      labelName += `_case_${$caseStmt.values[0].code}_to_${$caseStmt.values[1].code}`;
    return labelName;
  }

  /**
   * Creates if and label statements for each case in the provided switch statement and adds them to the private fields "caseIfStmts" and "caseLabels".
   * @param $jp - The switch statement
   */
  private computeIfAndLabels($jp: Switch) {
    const $switchCondition = $jp.condition;
    this.caseLabels = new Map();
    this.caseIfStmts.length = 0;

    for (const $case of $jp.cases) {
      const labelName = this.computeLabelName($case);
      const $labelDecl = ClavaJoinPoints.labelDecl(labelName);
      const $goto = ClavaJoinPoints.gotoStmt($labelDecl);

      const $labelStmt = ClavaJoinPoints.labelStmt($labelDecl);
      this.caseLabels.set($case.astId, $labelStmt);

      if ($case.isDefault) {
        this.caseIfStmts.push($goto);
        continue;
      }

      let $ifCondition;
      if ($case.values.length == 1)
        $ifCondition = ClavaJoinPoints.binaryOp(
          "==",
          $switchCondition,
          $case.values[0],
          "boolean"
        );
      else {
        const $binOpGE = ClavaJoinPoints.binaryOp(
          ">=",
          $switchCondition,
          $case.values[0],
          "boolean"
        );
        const $binOpLE = ClavaJoinPoints.binaryOp(
          "<=",
          $switchCondition,
          $case.values[1],
          "boolean"
        );
        $ifCondition = ClavaJoinPoints.binaryOp(
          "&&",
          $binOpGE,
          $binOpLE,
          "boolean"
        );
      }
      const $ifStmt = ClavaJoinPoints.ifStmt($ifCondition, $goto);
      this.caseIfStmts.push($ifStmt);
    }
  }

  /**
   * Reorders the private field "caseIfStmts" by moving the goto statement of the intermediate default case to the end.
   */
  private moveDefaultToEnd() {
    const index = this.caseIfStmts.findIndex(
      ($condition) => $condition instanceof GotoStmt
    );
    if (index !== -1)
      this.caseIfStmts.push(this.caseIfStmts.splice(index, 1)[0]);
  }

  /**
   * Links the statements stored in the private field "caseIfStmts" by setting the body of their else as the next statement in the list
   */
  private linkIfStmts() {
    for (let i = 0; i < this.caseIfStmts.length - 1; i++) {
      const $ifStmt = this.caseIfStmts[i];

      if ($ifStmt instanceof GotoStmt) {
        throw new Error(
          "Unexpected goto statement in the middle of the switch"
        );
      }

      const $nextIfStmt = this.caseIfStmts[i + 1];
      $ifStmt.setElse($nextIfStmt); // The condition "i < this.caseIfStmts.length - 1" guarantees that $ifStmt will always be of type If but it is very poor code.
    }
  }

  /**
   * Replaces the break statements that refer to the exit of the provided switch statement with goto statements.
   * @param $jp - The switch statement
   * @param $switchExitGoTo - The goto statement that corresponds to the switch exit. This statement will be used to replace the break statements
   */
  private replaceBreakWithGoto($jp: Switch, $switchExitGoTo: GotoStmt) {
    const $breakStmts = Query.searchFromInclusive($jp, Break, {
      enclosingStmt: (enclosingStmt: LaraJoinPoint) =>
        (enclosingStmt as Statement).astId === $jp.astId,
    });
    for (const $break of $breakStmts)
      $break.replaceWith($switchExitGoTo);
  }

  /**
   * Inserts the label and instructions of each case in the provided switch statement
   * @param $jp - the switch statement
   */
  addLabelsAndInstructions($jp: Switch) {
    for (const $case of $jp.cases) {
      const $caseLabel = this.caseLabels.get($case.astId);
      if ($caseLabel != undefined) {
        $jp.insertBefore($caseLabel);
      }

      for (const $inst of $case.instructions) $jp.insertBefore($inst);
    }
  }
}

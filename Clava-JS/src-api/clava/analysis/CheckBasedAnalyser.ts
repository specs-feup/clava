import Analyser from "./Analyser.js";
import Checker from "./Checker.js";
import ResultFormatManager from "./ResultFormatManager.js";
import Query from "lara-js/api/weaver/Query.js";
import { FileJp, Program } from "../../Joinpoints.js";
import { LaraJoinPoint } from "lara-js/api/LaraJoinPoint.js";
import AnalyserResult from "./AnalyserResult.js";

type T = Program | FileJp;

/**
 * Analyser that scan code to detect unsafe functions
 */
export default class CheckBasedAnalyser extends Analyser<T> {
  private checkers: Checker[] = [];
  private resultFormatManager: ResultFormatManager<T> =
    new ResultFormatManager();
  private fixFlag: boolean = false;
  unsafeCounter: number = 0;
  warningCounter: number = 0;

  addChecker(checker: Checker) {
    this.checkers.push(checker);
  }

  enableFixing() {
    this.fixFlag = true;
  }

  disableFixing() {
    this.fixFlag = false;
  }

  /**
   * Check file for unsafe functions, each one of them being specified by a checker
   * Analyser based on a list of checkers, each one of them is designed to spot one type of function.
   * The analysis is performed node by node.
   *
   * @param $startNode -
   * @returns fileResult
   */
  analyse($startNode: T = Query.root() as Program) {
    const checkResultList: AnalyserResult<LaraJoinPoint>[] = [];
    for (const $node of Query.searchFrom($startNode)) {
      for (const checker of this.checkers) {
        const checkResult = checker.check($node);
        if (checkResult === undefined) {
          continue;
        }

        checkResultList.push(checkResult);
      }
    }

    if (this.fixFlag) {
      checkResultList.forEach((checkResult) => {
        checkResult.performFix();
      });
    }
    this.resultFormatManager.setAnalyserResultList(checkResultList);

    return this.resultFormatManager.formatResultList($startNode);
  }
}

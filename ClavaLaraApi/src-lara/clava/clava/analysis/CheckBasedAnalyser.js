import Analyser from "./Analyser.js";
import ResultFormatManager from "./ResultFormatManager.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";
/**
 * Analyser that scan code to detect unsafe functions
 */
export default class CheckBasedAnalyser extends Analyser {
    checkers = [];
    resultFormatManager = new ResultFormatManager();
    fixFlag = false;
    unsafeCounter = 0;
    warningCounter = 0;
    addChecker(checker) {
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
    analyse($startNode = Query.root()) {
        const checkResultList = [];
        for (const a of Query.searchFrom($startNode)) {
            const $jp = a;
            for (const checker of this.checkers) {
                const checkResult = checker.check($jp);
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
//# sourceMappingURL=CheckBasedAnalyser.js.map
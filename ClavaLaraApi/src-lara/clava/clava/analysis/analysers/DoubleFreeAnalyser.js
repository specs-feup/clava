import Query from "lara-js/api/weaver/Query.js";
import { BinaryOp, Call, FunctionJp, Vardecl, } from "../../../Joinpoints.js";
import Analyser from "../Analyser.js";
import ResultFormatManager from "../ResultFormatManager.js";
import DoubleFreeResult from "./DoubleFreeResult.js";
/**
 * Analyser that scan scopes to check double-freed or unfreed memory
 */
export default class DoubleFreeAnalyser extends Analyser {
    resultFormatManager = new ResultFormatManager();
    static isDynamicAlloc($node) {
        if ($node.code.match(/.*malloc|calloc|realloc.*/)) {
            return 1;
        }
        return 0;
    }
    /**
     * Check file for pointers not being freed or being freed two times in the same scope
     * @param $startNode -
     * @returns fileResult
     */
    analyse($startNode) {
        let doubleFreeResultList = [];
        if ($startNode === undefined) {
            $startNode = Query.root();
        }
        for (const $node of Query.searchFrom($startNode)) {
            if (!($node instanceof FunctionJp)) {
                continue;
            }
            for (const $child of $node.descendants) {
                //Check for dynamic pointer declaration
                if (($child instanceof Vardecl || $child instanceof BinaryOp) &&
                    DoubleFreeAnalyser.isDynamicAlloc($child)) {
                    const ptrName = $child instanceof Vardecl ? $child.name : $child.left.code;
                    for (const $grandChild of $child.descendants) {
                        if ($grandChild instanceof Call &&
                            DoubleFreeAnalyser.isDynamicAlloc($grandChild)) {
                            const message = " Unfreed pointer in this scope. This can lead to a memory leak and a potential vunerability (CWE-401)." +
                                " Make sure it is freed somewhere in your program.\n\n";
                            doubleFreeResultList.push(new DoubleFreeResult("Unfreed array", $child, message, ptrName, $node.name));
                        }
                    }
                }
                if ($child instanceof Call && $child.name === "free") {
                    for (const result of doubleFreeResultList) {
                        if ($child.args[0].code === result.ptrName &&
                            $node.name === result.scopeName) {
                            if (result.freedFlag === 0) {
                                result.freedFlag = 1;
                                result.message = "";
                            }
                            else if (result.freedFlag === 1) {
                                result.freedFlag = -1;
                                result.name = "Double-freed array";
                                result.message =
                                    " Double-freed pointer in this scope. This could lead to a security vulnerability (CWE-415). Remove one of the calls to free().\n\n";
                            }
                        }
                    }
                }
            }
        }
        // We have now a list of checker's name each leading to a list of CheckResult
        doubleFreeResultList = doubleFreeResultList.filter((result) => result.freedFlag === 0);
        this.resultFormatManager.setAnalyserResultList(doubleFreeResultList);
        const fileResult = this.resultFormatManager.formatResultList($startNode);
        if (fileResult === undefined) {
            return;
        }
        return fileResult;
    }
}
//# sourceMappingURL=DoubleFreeAnalyser.js.map
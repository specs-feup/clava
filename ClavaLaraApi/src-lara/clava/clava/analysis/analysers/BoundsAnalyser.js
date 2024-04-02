import Query from "lara-js/api/weaver/Query.js";
import { ArrayAccess, ArrayType, FunctionJp, Vardecl, } from "../../../Joinpoints.js";
import Analyser from "../Analyser.js";
import ResultFormatManager from "../ResultFormatManager.js";
import BoundsResult from "./BoundsResult.js";
/**
 * Analyser that scan code to detect unsafe array accesses
 */
export default class BoundsAnalyser extends Analyser {
    resultFormatManager = new ResultFormatManager();
    /**
     * Check file for illegal access of an array with an invalid index
     * @param $startNode -
     * @returns fileResult
     */
    analyse($startNode = Query.root()) {
        let boundsResultList = [];
        for (const $node of Query.searchFrom($startNode)) {
            if (!($node instanceof FunctionJp)) {
                continue;
            }
            for (const $child of $node.descendants) {
                if ($child instanceof Vardecl && $child.type instanceof ArrayType) {
                    const lengths = $child.type.arrayDims;
                    if ($child.hasInit) {
                        boundsResultList.push(new BoundsResult("Unsafe array access", $child, " The index used to access the array is not valid (CWE-119). Please check the length of the array accessed.\n\n", $node.name, true, false, lengths));
                        continue;
                    }
                    boundsResultList.push(new BoundsResult("Unsafe array access", $child, " The array being accessed has not been initialized (CWE-457).\n\n", $node.name, false, false, lengths));
                    continue;
                }
                if ($child instanceof ArrayAccess) {
                    const arrayName = $child.arrayVar.code;
                    for (const result of boundsResultList) {
                        if (result.arrayName === arrayName) {
                            // list of indexes in square brackets
                            const indexes = $child.code.match(/\[[0-9]+\]/g);
                            if (indexes == null) {
                                continue;
                            }
                            for (let i = 0; i < indexes.length; i++) {
                                if (indexes[i].length > 1) {
                                    // formats list of indexes
                                    indexes.forEach((index) => index.substring(1, index.length - 1));
                                }
                                if (result.initializedFlag === false) {
                                    result.unsafeAccessFlag = true;
                                    result.line = $child.line;
                                    continue;
                                }
                                if (Number(indexes[i]) > result.lengths[i] - 1 ||
                                    Number(indexes[i]) < 0) {
                                    // access out of bounds
                                    result.unsafeAccessFlag = true;
                                    result.line = $child.line;
                                    continue;
                                }
                            }
                        }
                    }
                }
            }
        }
        boundsResultList = boundsResultList.filter((result) => result.unsafeAccessFlag === true);
        this.resultFormatManager.setAnalyserResultList(boundsResultList);
        const fileResult = this.resultFormatManager.formatResultList($startNode);
        if (fileResult === undefined) {
            return;
        }
        return fileResult;
    }
}
//# sourceMappingURL=BoundsAnalyser.js.map
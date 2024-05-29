import Pass from "lara-js/api/lara/pass/Pass.js";
import PassResult from "lara-js/api/lara/pass/results/PassResult.js";
import Query from "lara-js/api/weaver/Query.js";
import { ReturnStmt } from "../../Joinpoints.js";
// TODO: Refactor to use the SimplePass pattern
export default class SimplifyReturnStmts extends Pass {
    _name = "SimplifyReturnStmts";
    statementDecomposer;
    constructor(statementDecomposer) {
        super();
        this.statementDecomposer = statementDecomposer;
    }
    _apply_impl($jp) {
        let appliedPass = false;
        for (const $returnStmt of Query.searchFromInclusive($jp, ReturnStmt)) {
            const transformed = this.transform($returnStmt);
            // If any change, mark as applied
            if (transformed) {
                appliedPass = true;
            }
        }
        return new PassResult(this, $jp, {
            appliedPass: appliedPass,
            insertedLiteralCode: false,
        });
    }
    /**
     *
     * @param $returnStmt -
     * @returns true if there were changes, false otherwise
     */
    transform($returnStmt) {
        const decomposeResult = this.statementDecomposer.decompose($returnStmt);
        if (decomposeResult.length === 0) {
            return false;
        }
        // Returns a list of stmts, replace with current return
        for (const stmt of decomposeResult) {
            $returnStmt.insertBefore(stmt);
        }
        $returnStmt.detach();
        return true;
    }
}
//# sourceMappingURL=SimplifyReturnStmts.js.map
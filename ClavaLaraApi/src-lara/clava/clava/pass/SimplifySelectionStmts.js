import Pass from "lara-js/api/lara/pass/Pass.js";
import PassResult from "lara-js/api/lara/pass/results/PassResult.js";
import Query from "lara-js/api/weaver/Query.js";
import { If } from "../../Joinpoints.js";
// TODO: Refactor to use the SimplePass pattern
export default class SimplifySelectionStmts extends Pass {
    _name = "SimplifySelectionStmts";
    statementDecomposer;
    constructor(statementDecomposer) {
        super();
        this.statementDecomposer = statementDecomposer;
    }
    _apply_impl($jp) {
        let appliedPass = false;
        for (const jp of Query.searchFromInclusive($jp, If)) {
            const $if = jp;
            appliedPass = true;
            this.transform($if);
        }
        return new PassResult(this, $jp, {
            appliedPass: appliedPass,
            insertedLiteralCode: false,
        });
    }
    transform($ifStmt) {
        const $ifCond = $ifStmt.cond;
        const decomposeResult = this.statementDecomposer.decomposeExpr($ifCond);
        for (const stmt of decomposeResult.precedingStmts) {
            $ifStmt.insertBefore(stmt);
        }
        for (const stmt of decomposeResult.succeedingStmts.slice().reverse()) {
            $ifStmt.then.insertBegin(stmt);
            $ifStmt.else.insertBegin(stmt);
        }
        $ifStmt.cond.replaceWith(decomposeResult.$resultExpr);
    }
}
//# sourceMappingURL=SimplifySelectionStmts.js.map
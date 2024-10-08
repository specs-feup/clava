import Pass from "@specs-feup/lara/api/lara/pass/Pass.js";
import PassResult from "@specs-feup/lara/api/lara/pass/results/PassResult.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";
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
        for (const $if of Query.searchFromInclusive($jp, If)) {
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
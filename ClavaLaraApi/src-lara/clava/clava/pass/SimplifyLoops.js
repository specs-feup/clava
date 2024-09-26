import Pass from "@specs-feup/lara/api/lara/pass/Pass.js";
import PassResult from "@specs-feup/lara/api/lara/pass/results/PassResult.js";
import { DeclStmt, Loop } from "../../Joinpoints.js";
import ClavaJoinPoints from "../ClavaJoinPoints.js";
import DoToWhileStmt from "../code/DoToWhileStmt.js";
import ForToWhileStmt from "../code/ForToWhileStmt.js";
export default class SimplifyLoops extends Pass {
    _name = "SimplifyLoops";
    statementDecomposer;
    options;
    label_suffix = 0;
    /**
     *
     * @param statementDecomposer -
     * @param options - Object with options. Supported options: 'forToWhile' (default: true), transforms for loops into while loops
     */
    constructor(statementDecomposer, options = { forToWhile: true }) {
        super();
        this.statementDecomposer = statementDecomposer;
        this.options = options;
    }
    _apply_impl($jp) {
        let appliedPass = false;
        for (const $loop of this._findLoops($jp)) {
            appliedPass = true;
            if (this.options.forToWhile) {
                const $whileLoop = this.makeWhileLoop($loop);
                this.transform($whileLoop);
            }
        }
        return new PassResult(this, $jp, {
            appliedPass: appliedPass,
            insertedLiteralCode: true,
        });
    }
    *_findLoops($jp) {
        for (const child of $jp.children) {
            yield* this._findLoops(child);
        }
        if ($jp instanceof Loop &&
            ($jp.kind === "for" || $jp.kind === "dowhile" || $jp.kind === "while")) {
            yield $jp;
        }
    }
    makeWhileLoop($loop) {
        if ($loop.kind === "for") {
            const $forToWhileScope = ForToWhileStmt($loop, this.label_suffix++);
            return $forToWhileScope.children[1];
        }
        else if ($loop.kind === "dowhile") {
            return DoToWhileStmt($loop, this.label_suffix++);
        }
        else {
            return $loop;
        }
    }
    transform($whileLoop) {
        const $loopCond = $whileLoop.cond;
        const decomposeResult = this.statementDecomposer.decomposeExpr($loopCond.expr);
        for (const stmt of decomposeResult.precedingStmts) {
            $whileLoop.insertBefore(stmt);
        }
        for (const stmt of decomposeResult.succeedingStmts) {
            $whileLoop.insertAfter(stmt);
        }
        for (const stmt of decomposeResult.succeedingStmts.slice().reverse()) {
            $whileLoop.body.insertBegin(stmt);
        }
        for (const stmt of decomposeResult.precedingStmts.filter(($stmt) => !($stmt instanceof DeclStmt))) {
            $whileLoop.body.insertEnd(stmt);
        }
        $whileLoop.cond.replaceWith(ClavaJoinPoints.exprStmt(decomposeResult.$resultExpr));
    }
}
//# sourceMappingURL=SimplifyLoops.js.map
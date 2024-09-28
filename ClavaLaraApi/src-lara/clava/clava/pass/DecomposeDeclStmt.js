import SimplePass from "@specs-feup/lara/api/lara/pass/SimplePass.js";
import PassResult from "@specs-feup/lara/api/lara/pass/results/PassResult.js";
import { DeclStmt } from "../../Joinpoints.js";
import ClavaJoinPoints from "../ClavaJoinPoints.js";
/**
 * Decomposes composite declaration statements into separate statements for each variable.
 *
 * This means that a declaration like:
 *
 * ```c
 * int a, b = 10, c;
 * ```
 *
 * Will be decomposed to:
 *
 * ```c
 * int a;
 * int b = 10;
 * int c;
 * ```
 */
export default class DecomposeDeclStmt extends SimplePass {
    _name = "DecomposeDeclStmt";
    matchJoinpoint($jp) {
        if (!($jp instanceof DeclStmt)) {
            return false;
        }
        if ($jp.numChildren <= 1) {
            return false;
        }
        return true;
    }
    transformJoinpoint($jp) {
        let $firstDeclStmt = undefined;
        for (const $decl of $jp.decls) {
            const $singleDeclStmt = ClavaJoinPoints.declStmt($decl);
            if (!$firstDeclStmt) {
                $firstDeclStmt = $singleDeclStmt;
            }
            $jp.insertBefore($singleDeclStmt);
        }
        $jp.detach();
        return new PassResult(this, $firstDeclStmt);
    }
}
//# sourceMappingURL=DecomposeDeclStmt.js.map
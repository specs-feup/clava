import { Loop } from "../../Joinpoints.js";
import ClavaJoinPoints from "../ClavaJoinPoints.js";
export default function DoToWhileStmt($doStmt, labelSuffix) {
    // do statements have an unconditional first iteration
    const firstIterStmts = $doStmt.scopeNodes.map(($stmt) => $stmt.copy());
    const firstIterScope = ClavaJoinPoints.scope(...firstIterStmts);
    $doStmt.insertBefore(firstIterScope);
    // convert continues in first iteration to jumps to beginning of loop
    const localContinues = [...findLocalContinue(firstIterScope)];
    if (localContinues.length > 0) {
        const $labelDecl = ClavaJoinPoints.labelDecl(`__do_loop_head_${labelSuffix}`);
        $doStmt.insertBefore(ClavaJoinPoints.labelStmt($labelDecl));
        for (const $continue of localContinues) {
            $continue.replaceWith(ClavaJoinPoints.gotoStmt($labelDecl));
        }
    }
    // convert breaks in first iteration to jumps to after loop
    const localBreaks = [...findLocalBreak(firstIterScope)];
    if (localBreaks.length > 0) {
        const $labelDecl = ClavaJoinPoints.labelDecl(`__do_loop_end_${labelSuffix}`);
        $doStmt.insertAfter(ClavaJoinPoints.emptyStmt());
        $doStmt.insertAfter(ClavaJoinPoints.labelStmt($labelDecl));
        for (const $break of localBreaks) {
            $break.replaceWith(ClavaJoinPoints.gotoStmt($labelDecl));
        }
    }
    const $while = ClavaJoinPoints.whileStmt($doStmt.cond, $doStmt.body);
    $doStmt.replaceWith($while);
    return $while;
}
function* findLocalBreak($jp) {
    if ($jp.astName === "BreakStmt") {
        yield $jp;
        return;
    }
    if ($jp instanceof Loop) {
        return;
    }
    for (const $child of $jp.children) {
        yield* findLocalBreak($child);
    }
}
function* findLocalContinue($jp) {
    if ($jp.astName === "ContinueStmt") {
        yield $jp;
        return;
    }
    if ($jp instanceof Loop) {
        return;
    }
    for (const $child of $jp.children) {
        yield* findLocalContinue($child);
    }
}
//# sourceMappingURL=DoToWhileStmt.js.map
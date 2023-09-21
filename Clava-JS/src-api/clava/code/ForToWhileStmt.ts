import { Joinpoint, Loop } from "../../Joinpoints.js";
import ClavaJoinPoints from "../ClavaJoinPoints.js";

/**
 * Replaces for loop with an equivalent construct based on a while loop:
 * ```c
 * for (init; cond; step) {
 *   //...
 *   continue;
 *   //...
 *   //...
 * }
 * ```
 * becomes
 * ```c
 * {
 *   init;
 *   while (cond) {
 *     // ...
 *     goto __for_loop_step_${step_label_suffix};
 *     // ...
 *     // ...
 * __for_loop_step_${step_label_suffix}:
 *     step;
 *   }
 * }
 * ```
 * @param $forStmt - For-loop joinpoint
 * @param stepLabelSuffix - Suffix to attach to the for loop step label, added in case there are `continue` statements to account for
 * @returns The newly created replacement joinpoint
 */
export default function ForToWhileStmt($forStmt: Loop, stepLabelSuffix: number | string) {
  // replace continues with gotos to the step statement for the new loop body
  const localContinues = [...findLocalContinue($forStmt.body)];
  let loopStepStatements;

  const initStmt = $forStmt.init ?? ClavaJoinPoints.emptyStmt();
  const condStmt = $forStmt.cond ?? ClavaJoinPoints.integerLiteral(1);
  const stepStmt = $forStmt.step ?? ClavaJoinPoints.emptyStmt();

  if (localContinues.length > 0) {
    const $labelDecl = ClavaJoinPoints.labelDecl(
      `__for_loop_step_${stepLabelSuffix}`
    );
    loopStepStatements = [ClavaJoinPoints.labelStmt($labelDecl), stepStmt];

    for (const $continue of localContinues) {
      $continue.replaceWith(ClavaJoinPoints.gotoStmt($labelDecl));
    }
  } else {
    loopStepStatements = [stepStmt];
  }

  const $scope = ClavaJoinPoints.scope(
    initStmt,
    ClavaJoinPoints.whileStmt(
      condStmt,
      ClavaJoinPoints.scope(...$forStmt.scopeNodes, ...loopStepStatements)
    ),
  );

  $forStmt.replaceWith($scope);

  return $scope;
}

/**
 * find local continue statements in a loop: that is, recursively traverse the tree and return continue statements,
 * while ignoring sub-trees starting from an inner loop
 * 
 * TODO: optimization - ignore sub-trees that will for sure not contain ContinueStmt nodes
 * 
 * @param $jp -
 * @returns 
 */
function* findLocalContinue($jp: Joinpoint): Generator<Joinpoint> {
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
};

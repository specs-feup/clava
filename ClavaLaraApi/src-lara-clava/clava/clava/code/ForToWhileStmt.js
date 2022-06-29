laraImport("clava.ClavaJoinPoints");

function ForToWhileStmt($forStmt, stepLabelSuffix) {
  // replace continues with gotos to the step statement for the new loop body
  const localContinues = [...ForToWhileStmt._findLocalContinue($forStmt.body)];
  let loopStepStatements;
  if (localContinues.length > 0) {
    const $labelDecl = ClavaJoinPoints.labelDecl(
      `__for_loop_step_${stepLabelSuffix}`
    );
    loopStepStatements = [ClavaJoinPoints.labelStmt($labelDecl), $forStmt.step];

    for (const $continue of localContinues) {
      $continue.replaceWith(ClavaJoinPoints.gotoStmt($labelDecl));
    }
  } else {
    loopStepStatements = [$forStmt.step];
  }

  const $scope = ClavaJoinPoints.scope([
    $forStmt.init,
    ClavaJoinPoints.whileStmt(
      $forStmt.cond,
      ClavaJoinPoints.scope([...$forStmt.scopeNodes, ...loopStepStatements])
    ),
  ]);

  $forStmt.replaceWith($scope);

  return $scope;
}

// find local continue statements in a loop: that is, recursively traverse the tree and return continue statements,
// while ignoring sub-trees starting from an inner loop
// TODO: optimization - ignore sub-trees that will for sure not contain ContinueStmt nodes
ForToWhileStmt._findLocalContinue = function* ($jp) {
  if ($jp.astName === "ContinueStmt") {
    yield $jp;
    return;
  }
  if ($jp.instanceOf("loop")) {
    return;
  }
  for (const $child of $jp.children) {
    yield* ForToWhileStmt._findLocalContinue($child);
  }
};

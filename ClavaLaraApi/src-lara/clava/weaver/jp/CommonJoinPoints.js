// Enable Clava version of common join points
laraImport("weaver.jp.ClavaJoinPoint");
laraImport("weaver.jp.ClavaClassJp");
laraImport("weaver.jp.ClavaMethodJp");
laraImport("weaver.jp.ClavaFunctionJp");
laraImport("weaver.jp.ClavaCallJp");
laraImport("weaver.jp.ClavaMemberCallJp");
laraImport("weaver.jp.ClavaFieldJp");
laraImport("weaver.jp.ClavaFieldRefJp");
laraImport("weaver.jp.ClavaTypeJp");
laraImport("weaver.jp.ClavaVarDeclJp");
laraImport("weaver.jp.ClavaVarRefJp");
laraImport("weaver.jp.ClavaParamJp");
laraImport("weaver.jp.ClavaConstructorJp");
laraImport("weaver.jp.ClavaConstructorCallJp");
laraImport("weaver.jp.ClavaLoopJp");
laraImport("weaver.jp.ClavaIfJp");
laraImport("weaver.jp.ClavaBinaryJp");
laraImport("weaver.jp.ClavaFileJp");
laraImport("weaver.jp.ClavaElseJp");

// Patches weaver.JoinPoint and other classes
laraImport("weaver.jp.JoinPointsCommonPath");

laraImport("weaver.Selector");

///

Selector.prototype._addJps = function (
  $newJps,
  $jps,
  jpFilter,
  $jpChain,
  name
) {
  for (var $jp of $jps) {
    const $filteredJp = jpFilter.filter([$jp]);

    if ($filteredJp.length === 0) {
      continue;
    }

    if ($jp.instanceOf("function") || $jp.instanceOf("class")) {
      const decl = $jp.originalAstNode;
      const declaration = decl.getDeclaration();
      const definition = decl.getDefinition();

      // If none is present
      if (!declaration.isPresent() && !definition.isPresent())
        throw "Expected at least one of them to be present";

      // XOR, if only one of them is present, current node must be one of them
      if (declaration.isEmpty() ^ definition.isEmpty()) {
      }
      // Both are present, only add current node if it is the definition
      else if (declaration.get().equals(decl)) {
        continue;
      }
    }

    if ($filteredJp.length > 1) {
      throw `Selector._addJps: Expected \$filteredJp to have length 1, has ${$filteredJp.length}`;
    }

    // Copy chain
    const $updatedChain = Selector._copyChain($jpChain);

    // Update join point
    $updatedChain[name] = $jp;

    // Add jp with unique id
    const id = `${name}_${$updatedChain[Selector._COUNTER].add(name)}`;
    $updatedChain[id] = $jp;

    $newJps.push($updatedChain);
  }
};

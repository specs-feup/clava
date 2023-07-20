laraImport("weaver.Query");


class LivenessUtils {
  static getVarDeclsWithInit($stmt) {
    const $varDecls = Query.searchFromInclusive($stmt, "vardecl", {hasInit: true});
    const varNames = [...$varDecls].map(($decl) => $decl.name);

    return new Set(varNames);
  }

  static getAssignedVars($stmt) {
    const $assignments = Query.searchFromInclusive($stmt, "binaryOp", {isAssignment: true, left: left => left.instanceOf("varref")});
    const assignedVars = [...$assignments].map(($assign) => $assign.left.name);

    return new Set(assignedVars);
  }

  static getVarRefs($stmt) {
    const $varRefs = Query.searchFromInclusive($stmt, "varref");

    let varNames = new Set();
    for (const $ref of $varRefs) {
        if ($ref.hasParent && $ref.parent.isAssignment && $ref.parent.left.astId === $ref.astId)
            continue;
        varNames.add($ref.name);
    }

    return varNames;
  }
  
  static unionSets(set1, set2) {
    return new Set([...set1, ...set2]);
  }
  
  static differenceSets(set1, set2) {
    return new Set([...set1].filter(x => !set2.has(x)));
  }

  static isSameSet(set1, set2) {
    if (set1.size !== set2.size)
        return false;
    return [...set1].every(set2.has, set2);
  }

  static getChildren(node) {
    const edges = node.connectedEdges();
    const outgoingEdges = edges.filter((edge) => edge.source().equals(node));
    return outgoingEdges.map((edge) => edge.target());
  }
}

laraImport("weaver.Query");

class LivenessUtils {

  /**
   * Checks if the given graph is a Cytoscape graph
   * @param graph 
   * @returns {Boolean}
   */
  static isCytoscapeGraph(graph) {
    return (typeof graph === 'object' && 
            'nodes' in graph && 
            'edges' in graph &&
            typeof graph.add === 'function' && 
            typeof graph.layout === 'function')
  }

  /**
   * Computes the union of two sets
   * @param {Set} set1 
   * @param {Set} set2 
   * @returns {Set} a set containing the union of elements from both input sets
   */
  static unionSets(set1, set2) {
    return new Set([...set1, ...set2]);
  }
  
  /**
   * Computes the set difference between two sets
   * @param {Set} set1 
   * @param {Set} set2 
   * @returns {Set} a set containing all the elements present in set1 but not in set2
   */
  static differenceSets(set1, set2) {
    return new Set([...set1].filter(x => !set2.has(x)));
  }

  /**
   * Checks if two sets contain the same elements
   * @param {Set} set1 
   * @param {Set} set2 
   * @returns {Boolean}
   */
  static isSameSet(set1, set2) {
    if (set1.size !== set2.size)
        return false;
    return [...set1].every(set2.has, set2);
  }

  /**
   * Returns the children of a node
   * @param {Cytoscape.node} node
   * @returns {Array <Cytoscape.node>} an array containing the children of the given node
   */
  static getChildren(node) {
    const edges = node.connectedEdges();
    const outgoingEdges = edges.filter((edge) => edge.source().equals(node));
    return outgoingEdges.map((edge) => edge.target());
  }

  /**
   * Checks if the provided joinpoint refers to an assigned variable.
   * @param {joinpoint} $varref the varref join point 
   * @returns {Boolean} 
   */
  static isAssignedVar($varref) {
    const $parent = $varref.parent;

    return $parent !== undefined && $parent.isAssignment && $parent.left.astId === $varref.astId;
  }

  /**
   * Checks if the given joinpoint is a local variable or parameter
   * @param {joinpoint} $varref the varref join point 
   * @returns {Boolean} 
   */
  static isLocalOrParam($varref) {
    const $varDecl = $varref.vardecl;
    return $varDecl !== undefined && !$varDecl.isGlobal; 
  }

  /**
   * 
   * @param {joinpoint} $stmt the statement join point 
   * @return {Set} a set of variable names declared with initialization in the given joinpoint 
   */
  static getVarDeclsWithInit($stmt) {
    const $varDecls = Query.searchFromInclusive($stmt, "vardecl", {hasInit: true});
    const varNames = [...$varDecls].map(($decl) => $decl.name);

    return new Set(varNames);
  }

  /**
   * 
   * @param {joinpoint} $stmt the statement join point 
   * @returns {Set} a set containing the names of the local variables or parameters on the left-hand side (LHS) of each assignment present in the given joinpoint
   */
  static getAssignedVars($stmt) {
    const $assignments = Query.searchFromInclusive($stmt, "binaryOp", {isAssignment: true, left: left => left.getInstanceOf("varref")});
    const assignedVars = [...$assignments].filter($assign => LivenessUtils.isLocalOrParam($assign.left)).map($assign => $assign.left.name)

    return new Set(assignedVars);
  }

  /**
   * 
   * @param {joinpoint} $stmt the statement join point 
   * @returns {Set} A set containing the names of local variables or parameters referenced by varref joinpoints, excluding those present on the LHS of assignments.
   */
  static getVarRefs($stmt) {
    const $varRefs = Query.searchFromInclusive($stmt, "varref");
    const varNames = [...$varRefs].filter($ref => !LivenessUtils.isAssignedVar($ref) && LivenessUtils.isLocalOrParam($ref)).map($ref => $ref.name)

    return new Set(varNames);
  }
}

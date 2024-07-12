import { LaraJoinPoint } from "lara-js/api/LaraJoinPoint.js";
import cytoscape from "lara-js/api/libs/cytoscape-3.26.0.js";
import Query from "lara-js/api/weaver/Query.js";
import {
  BinaryOp,
  Expression,
  Statement,
  Vardecl,
  Varref,
} from "../../Joinpoints.js";

export default class LivenessUtils {
  /**
   * Checks if the given graph is a Cytoscape graph
   * @deprecated Typescript type checking should be used instead
   */
  static isCytoscapeGraph(graph: cytoscape.Core): boolean {
    return (
      typeof graph === "object" &&
      "nodes" in graph &&
      "edges" in graph &&
      typeof graph.add === "function" &&
      typeof graph.layout === "function"
    );
  }

  /**
   * Computes the union of two sets
   * @param set1 -
   * @param set2 -
   * @returns A set containing the union of elements from both input sets
   */
  static unionSets<T>(
    set1: Set<T> | undefined,
    set2: Set<T> | undefined
  ): Set<T> {
    if (set1 === undefined && set2 === undefined) {
      return new Set<T>();
    } else if (set1 === undefined) {
      if (set2 === undefined) {
        return new Set<T>();
      } else {
        return set2;
      }
    } else if (set2 === undefined) {
      return set1;
    }

    return new Set<T>([...set1, ...set2]);
  }

  /**
   * Computes the set difference between two sets
   * @param set1 -
   * @param set2 -
   * @returns A set containing all the elements present in set1 but not in set2
   */
  static differenceSets<T>(
    set1: Set<T> | undefined,
    set2: Set<T> | undefined
  ): Set<T> {
    if (set1 === undefined) {
      return new Set<T>();
    }

    if (set2 === undefined) {
      return set1;
    }

    return new Set<T>([...set1].filter((x) => !set2.has(x)));
  }

  /**
   * Checks if two sets contain the same elements
   */
  static isSameSet<T>(
    set1: Set<T> | undefined,
    set2: Set<T> | undefined
  ): boolean {
    if (set1 === undefined) {
      return false;
    }

    if (set2 === undefined) {
      return false;
    }

    if (set1.size !== set2.size) {
      return false;
    }

    return [...set1].every((e: T) => set2.has(e), set2);
  }

  /**
   * Returns the children of a node
   * @param node -
   * @returns An array containing the children of the given node
   */
  static getChildren(node: cytoscape.NodeSingular): cytoscape.NodeSingular[] {
    const edges = node.connectedEdges();
    const outgoingEdges = edges.filter(
      (edge) => edge.source() == node
    ) as cytoscape.EdgeCollection;
    return outgoingEdges.map((edge) => edge.target());
  }

  /**
   * Checks if the provided joinpoint refers to an assigned variable.
   * @param $varref - The varref join point
   * @deprecated This method assumes that the giver Varref has a BinaryOp parent. Use carefully.
   */
  static isAssignedVar($varref: Varref): boolean {
    const $parent = $varref.parent;

    if ($parent === undefined) {
      return false;
    }

    if (!($parent instanceof BinaryOp)) {
      return false;
    }

    return (
      $parent !== undefined &&
      $parent.isAssignment &&
      $parent.left.astId === $varref.astId
    );
  }

  /**
   * Checks if the given joinpoint is a local variable or parameter
   * @param $varref - The varref join point
   */
  static isLocalOrParam($varref: Varref): boolean {
    const $varDecl = $varref.vardecl;
    return $varDecl !== undefined && !$varDecl.isGlobal;
  }

  /**
   *
   * @param $jp - The statement join point
   * @returns A set of variable names declared with initialization in the given joinpoint
   */
  static getVarDeclsWithInit($jp: Statement | Expression): Set<string> {
    const $varDecls = Query.searchFromInclusive($jp, Vardecl, {
      hasInit: true,
    });
    const varNames = ([...$varDecls] as Vardecl[]).map(($decl) => $decl.name);

    return new Set(varNames);
  }

  /**
   *
   * @param $jp - The statement join point
   * @returns A set containing the names of the local variables or parameters on the left-hand side (LHS) of each assignment present in the given joinpoint
   */
  static getAssignedVars($jp: Statement | Expression): Set<string> {
    const $assignments = Query.searchFromInclusive($jp, BinaryOp, {
      isAssignment: true,
      left: (left: LaraJoinPoint) => left instanceof Varref,
    });
    const assignedVars = ([...$assignments] as BinaryOp[])
      .filter(($assign) => LivenessUtils.isLocalOrParam($assign.left as Varref))
      .map(($assign) => ($assign.left as Varref).name);

    return new Set(assignedVars);
  }

  /**
   *
   * @param $jp - The statement join point
   * @returns A set containing the names of local variables or parameters referenced by varref joinpoints, excluding those present on the LHS of assignments.
   */
  static getVarRefs($jp: Statement | Expression): Set<string> {
    const $varRefs = Query.searchFromInclusive($jp, Varref);
    const varNames = ([...$varRefs] as Varref[])
      .filter(
        ($ref) =>
          !LivenessUtils.isAssignedVar($ref) &&
          LivenessUtils.isLocalOrParam($ref)
      )
      .map(($ref) => $ref.name);

    return new Set(varNames);
  }
}

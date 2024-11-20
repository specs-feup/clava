import cytoscape from "@specs-feup/lara/api/libs/cytoscape-3.26.0.js";
import {
  Case,
  FunctionJp,
  If,
  Loop,
  Scope,
  Statement,
} from "../../../Joinpoints.js";

export default class NextCfgNode {
  /**
   * The AST node to process
   */
  private entryPoint: Statement;

  /**
   * Maps stmts to graph nodes
   */
  private nodes: Map<string, cytoscape.NodeSingular>;

  /**
   * The end node of the graph
   */
  private endNode: cytoscape.NodeSingular;

  constructor(
    $entryPoint: Statement,
    nodes: Map<string, cytoscape.NodeSingular>,
    endNode: cytoscape.NodeSingular
  ) {
    this.entryPoint = $entryPoint;
    this.nodes = nodes;
    this.endNode = endNode;
  }

  /**
   *
   * @param $stmt -
   *
   * @returns the next graph node that executes unconditionally after the given stmt, or end node if no statement is executed
   */
  nextExecutedNode($stmt: Statement): cytoscape.NodeSingular {
    const afterStmt = this.nextExecutedStmt($stmt);

    // If after statement is undefined, return end node
    if (afterStmt === undefined) {
      return this.endNode;
    }

    // Get node corresponding to the after statement
    const afterNode = this.nodes.get(afterStmt.astId);

    // If the statement does not have an associated node, this means the next node is out of scope and should be considered the end node
    if (afterNode === undefined) {
      return this.endNode;
    }

    return afterNode;
  }

  /**
   * @returns The next stmt that executes unconditionally after the given stmt, of undefined if no statement is executed
   */
  nextExecutedStmt($stmt: Statement): Statement | undefined {
    // By definition, there is no statement executed after the entry point
    if ($stmt.equals(this.entryPoint)) {
      return undefined;
    }

    // If stmt is a scope, there are several special cases
    if ($stmt instanceof Scope) {
      return this.nextExecutedStmtAfterScope($stmt);
    }

    const rightStmts = $stmt.siblingsRight as Statement[];

    // If there are statements to the right, the rightmost non-case statement is the next to be executed
    if (rightStmts.length > 0) {
      for (const sibling of rightStmts) {
        if (!(sibling instanceof Case)) return sibling;
      }
    }

    // When there are no more statements, return what's next for the parent
    const $parent = $stmt.parent;

    if ($parent instanceof Statement) {
      return this.nextExecutedStmt($parent);
    }
    // There are no more statements
    else if ($parent instanceof FunctionJp) {
      return undefined;
    } else {
      throw new Error(
        "Case not defined for nodes with parent of type " +
          $parent.joinPointType
      );
    }
  }

  /**
   * @returns The the next stmt that executes unconditionally after the given scope, of undefined if no statement is executed
   */
  private nextExecutedStmtAfterScope($scope: Scope): Statement | undefined {
    // Before returning what's next to the scope of the statement, there are some special cases

    // Check if scope is a then/else of an if
    const $scopeParent = $scope.parent;
    if ($scopeParent instanceof If) {
      // Next stmt is what comes next of if
      return this.nextExecutedStmt($scopeParent);
    }

    // Check if scope is the body of a loop
    if ($scopeParent instanceof Loop) {
      // Next stmt is what comes next of if

      switch ($scopeParent.kind) {
        case "while":
        case "dowhile":
          if ($scopeParent.cond === undefined) {
            throw new Error(
              "Not implemented when for loops do not have a condition statement"
            );
          }
          return $scopeParent.cond;
        case "for":
          if ($scopeParent.step === undefined) {
            throw new Error(
              "Not implemented when for loops do not have a step statement"
            );
          }
          return $scopeParent.step;
        default:
          throw new Error(
            "Case not defined for loops of kind " + $scopeParent.kind
          );
      }
    }

    // Special cases handled, check scope siblings
    const rightStmts = $scope.siblingsRight as Statement[];

    // If there are statements, return next of parent
    if (rightStmts.length > 0) {
      return rightStmts[0];
    }

    // If scope parent is not a statement, there is no next statement
    if ($scopeParent instanceof Statement) {
      // Return next statement of parent statement
      return this.nextExecutedStmt($scopeParent);
    } else {
      return undefined;
    }
  }
}

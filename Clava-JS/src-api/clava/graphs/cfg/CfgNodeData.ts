import NodeData from "lara-js/api/lara/graphs/NodeData.js";
import { Statement } from "../../../Joinpoints.js";
import CfgNodeType from "./CfgNodeType.js";

/**
 * The data of a CFG node.
 */
export default class CfgNodeData<T extends Statement = Statement> extends NodeData {
  /**
   * The statement join point of the CFG node
   */
  nodeStmt: T | undefined;

  protected nodeType: CfgNodeType;

  protected nodeName: string | undefined;

  /**
   * Creates a new instance of the CfgNodeData class
   * @param cfgNodeType - Node type
   * @param $stmt - Statement that originated this CFG node
   * @param id - Identification of the CFG node
   */
  constructor(
    cfgNodeType: CfgNodeType,
    $stmt?: T,
    id: string | undefined = $stmt?.astId
  ) {
    // If id defined, give priority to it. Othewise, use stmt astId, if defined
    const _id =
      id !== undefined ? id : $stmt === undefined ? undefined : $stmt.astId;

    // Use AST node id as graph node id
    super(_id);

    this.nodeStmt = $stmt;

    this.nodeType = cfgNodeType;
  }

  /**
   * @returns The CFG node type
   */
  get type(): CfgNodeType {
    return this.nodeType;
  }

  /**
   * @returns The CFG node name
   */
  get name(): string {
    if (this.nodeName === undefined) {
      const typeName = this.nodeType.name;
      this.nodeName =
        typeName.substring(0, 1).toUpperCase() +
        typeName.substring(1, typeName.length).toLowerCase();
    }

    return this.nodeName;
  }

  /**
   * @returns The statements associated with this CFG node.
   */
  get stmts(): T[] {
    // By default, the list only containts the node statement
    return this.nodeStmt !== undefined ? [this.nodeStmt] : [];
  }

  /**
   *
   * @returns String representation of the CFG node
   */
  toString(): string {
    // By default, content of the node is the name of the type
    return this.name;
  }

  /**
   *
   * @returns True if this is a branch node, false otherwise. If this is a branch node, contains two edges, true and false.
   * If not, contains only one uncoditional edge (expect if it is the end node, which contains no edges).
   */
  isBranch(): boolean {
    return false;
  }
}

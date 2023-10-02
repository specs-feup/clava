import EdgeData from "lara-js/api/lara/graphs/EdgeData.js";
import CfgEdgeType from "./CfgEdgeType.js";

/**
 * An edge of the CFG
 */
export default class CfgEdge extends EdgeData {
  private edgeType: CfgEdgeType;

  /**
   * Creates a new instance of the CfgEdge class
   * @param type - Edge type
   */
  constructor(type: CfgEdgeType) {
    super();
    this.edgeType = type;
  }

  /**
   * @returns The edge type
   */
  get type(): CfgEdgeType {
    return this.edgeType;
  }

  /**
   *
   * @returns String representation of the edge. If it is a unconditional edge, an empty string is returned
   */
  toString(): string {
    // If unconditional jump, do not print a label
    if (this.edgeType === CfgEdgeType.UNCONDITIONAL) {
      return "";
    }

    // Otherwise, return the type name
    return this.edgeType.name;
  }
}

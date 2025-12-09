/**
 * Enumeration of CFG edge types.
 */
export default class CfgEdgeType {
  static TRUE = new CfgEdgeType("TRUE");
  static FALSE = new CfgEdgeType("FALSE");
  static UNCONDITIONAL = new CfgEdgeType("UNCONDITIONAL");

  private instanceName;

  /**
   * Creates a new instance of the CfgEdgeType class
   * @param name - The name of the CFG edge type
   */
  constructor(name: string) {
    this.instanceName = name;
  }

  /**
   * @returns The name of the CFG edge type
   */
  get name(): string {
    return this.instanceName;
  }

  /**
   * @returns String representation of the CFG edge type, which corresponds to its name
   */
  toString(): string {
    return this.name;
  }
}

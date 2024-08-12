import NodeData from "lara-js/api/lara/graphs/NodeData.js";
import Query from "lara-js/api/weaver/Query.js";
import { Call, FunctionJp } from "../../../Joinpoints.js";

export default class ScgNodeData extends NodeData {
  /**
   * The function represented by this node
   */
  private $function: FunctionJp;

  constructor($function: FunctionJp) {
    super();

    // Should use only canonical functions
    this.$function = $function.canonical;
  }

  get function(): FunctionJp {
    return this.$function;
  }

  toString(): string {
    return this.$function.signature;
  }

  /**
   * @returns true, if the function represented by this node has an available implementation, false otherwise
   */
  hasImplementation(): boolean {
    return this.$function.isImplementation;
  }

  hasCalls(): boolean {
    return Query.searchFrom(this.$function, Call).get().length > 0;
  }
}

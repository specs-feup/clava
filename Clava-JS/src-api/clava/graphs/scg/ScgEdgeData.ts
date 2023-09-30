import EdgeData from "lara-js/api/lara/graphs/EdgeData.js";
import { Call } from "../../../Joinpoints.js";

export default class ScgEdgeData extends EdgeData {
  /**
   * The calls that contributed to this edge
   */
  private $calls: Call[] = [];

  get calls(): Call[] {
    return this.$calls;
  }

  inc($call: Call) {
    this.$calls.push($call);
  }

  toString(): string {
    return this.$calls.length.toString();
  }
}

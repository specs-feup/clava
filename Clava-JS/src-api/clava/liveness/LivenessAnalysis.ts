import cytoscape from "lara-js/api/libs/cytoscape-3.26.0.js";
import ControlFlowGraph from "../graphs/ControlFlowGraph.js";
import LivenessAnalyser from "./LivenessAnalyser.js";

export default class LivenessAnalysis {
  /**
   * Maps each CFG node ID to the corresponding def set
   */
  private defsMap: Map<string, Set<string>>;

  /**
   * Maps each CFG node ID to the corresponding use set
   */
  private usesMap: Map<string, Set<string>>;

  /**
   * Maps each CFG node ID to the corresponding LiveIn set
   */
  private liveInMap: Map<string, Set<string>>;

  /**
   * Maps each CFG node ID to the corresponding LiveOut set
   */
  private liveOutMap: Map<string, Set<string>>;

  /**
   * Creates a new instance of the LivenessAnalysis class
   * @param defs - A map with CFG node IDs as keys and their corresponding def set as value
   * @param uses - A map with CFG node IDs as keys and their corresponding use set as value
   * @param liveIn - A map with CFG node IDs as keys and their corresponding live in set as value
   * @param liveOut - A map with CFG node IDs as keys and their corresponding live out set as value
   */
  constructor(
    defs: Map<string, Set<string>>,
    uses: Map<string, Set<string>>,
    liveIn: Map<string, Set<string>>,
    liveOut: Map<string, Set<string>>
  ) {
    this.defsMap = defs;
    this.usesMap = uses;
    this.liveInMap = liveIn;
    this.liveOutMap = liveOut;
  }

  /**
   *
   * @param cfg - The control flow graph. Can be either a Cytoscape graph or a ControlFlowGraph object and each instruction list node must contain only one statement
   * @returns A new instance of the LivenessAnalysis class
   */
  static analyse(cfg: ControlFlowGraph | cytoscape.Core): LivenessAnalysis {
    const analyser = new LivenessAnalyser(cfg).analyse();
    return new LivenessAnalysis(...analyser);
  }

  /**
   * @returns A map with CFG node IDs as keys and their corresponding def set as value
   */
  get defs() {
    return this.defsMap;
  }

  /**
   * @returns A map with CFG node IDs as keys and their corresponding use set as value
   */
  get uses() {
    return this.usesMap;
  }

  /**
   * @returns A map with CFG node IDs as keys and their corresponding liveIn set as value
   */
  get liveIn() {
    return this.liveInMap;
  }

  /**
   * @returns A map with CFG node IDs as keys and their corresponding liveOut set as value
   */
  get liveOut() {
    return this.liveOutMap;
  }
}

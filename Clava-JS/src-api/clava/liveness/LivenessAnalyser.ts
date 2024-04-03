import cytoscape from "lara-js/api/libs/cytoscape-3.26.0.js";
import { Case, Expression, If, Statement, Switch } from "../../Joinpoints.js";
import ControlFlowGraph from "../graphs/ControlFlowGraph.js";
import CfgNodeData from "../graphs/cfg/CfgNodeData.js";
import CfgNodeType from "../graphs/cfg/CfgNodeType.js";
import LivenessUtils from "./LivenessUtils.js";

export default class LivenessAnalyser {
  /**
   * A Cytoscape graph representing the CFG
   */
  private cfg: cytoscape.Core;

  /**
   * Maps each CFG node ID to the corresponding def set
   */
  private defs: Map<string, Set<string>>;

  /**
   * Maps each CFG node ID to the corresponding use set
   */
  private uses: Map<string, Set<string>>;

  /**
   * Maps each CFG node ID to the corresponding LiveIn set
   */
  private liveIn: Map<string, Set<string>>;

  /**
   * Maps each CFG node ID to the corresponding LiveOut set
   */
  private liveOut: Map<string, Set<string>>;

  /**
   * Creates a new instance of the LivenessAnalyser class
   * @param cfg - The control flow graph. Can be either a Cytoscape graph or a ControlFlowGraph object.
   */
  constructor(cfg: ControlFlowGraph | cytoscape.Core) {
    this.cfg = this.validateCfg(cfg);
    this.defs = new Map();
    this.uses = new Map();
    this.liveIn = new Map();
    this.liveOut = new Map();
  }

  /**
   * Checks if the given control flow graph is a Cytoscape graph or a ControlFlowGraph object.
   * Additionally, verifies if each instruction list node contains only one statement.
   * @param cfg - The control flow graph to be validated
   */
  private validateCfg(cfg: ControlFlowGraph | cytoscape.Core): cytoscape.Core {
    if (cfg instanceof ControlFlowGraph) cfg = cfg.graph;
    else if (!LivenessUtils.isCytoscapeGraph(cfg))
      throw new Error(
        "'cfg' must be a Cytoscape graph or a ControlFlowGraph object."
      );

    for (const node of cfg.nodes()) {
      const nodeData: CfgNodeData = node.data() as CfgNodeData;
      if (nodeData.type === CfgNodeType.INST_LIST && nodeData.stmts.length > 1)
        throw new Error(
          "Each instruction list node of the control flow graph must contain only one statement."
        );
    }
    return cfg;
  }

  /**
   * Computes the def, use, live in and live out sets of each CFG node
   * @returns An array that contains the def, use, live in and live out of each CFG node.
   */
  analyse(): [
    Map<string, Set<string>>,
    Map<string, Set<string>>,
    Map<string, Set<string>>,
    Map<string, Set<string>>
  ] {
    this.computeDefs();
    this.computeUses();
    this.computeLiveInOut();

    return [this.defs, this.uses, this.liveIn, this.liveOut];
  }

  /**
   *
   * @param $jp -
   * @returns The def set for the given joinpoint
   */
  private computeDef($jp: Statement | Expression | undefined): Set<string> {
    if ($jp === undefined) {
      throw new Error("Joinpoint is undefined");
    }

    const declaredVars = LivenessUtils.getVarDeclsWithInit($jp);
    const assignedVars = LivenessUtils.getAssignedVars($jp);
    return LivenessUtils.unionSets(declaredVars, assignedVars);
  }

  /**
   *
   * @param $jp -
   * @returns The use set for the given joinpoint
   */
  private computeUse($jp: Statement | Expression | undefined): Set<string> {
    if ($jp === undefined) {
      throw new Error("Joinpoint is undefined");
    }

    return LivenessUtils.getVarRefs($jp);
  }

  /**
   * Computes the def set of each CFG node according to its type
   */
  private computeDefs() {
    for (const node of this.cfg.nodes()) {
      const nodeData: CfgNodeData = node.data() as CfgNodeData;
      const $nodeStmt = nodeData.nodeStmt;
      const nodeType = nodeData.type;

      let def: Set<string>;
      switch (nodeType) {
        case CfgNodeType.START:
        case CfgNodeType.END:
        case CfgNodeType.SCOPE:
        case CfgNodeType.THEN:
        case CfgNodeType.ELSE:
        case CfgNodeType.LOOP:
        case CfgNodeType.SWITCH:
        case CfgNodeType.CASE:
          def = new Set<string>();
          break;
        case CfgNodeType.IF:
          def = this.computeDef(($nodeStmt as If | undefined)?.cond);
          break;
        default:
          def = this.computeDef($nodeStmt);
      }
      this.defs.set(node.id(), def);
    }
  }

  /**
   * Computes the use set of each CFG node according to its type
   */
  private computeUses() {
    for (const node of this.cfg.nodes()) {
      const nodeData: CfgNodeData = node.data() as CfgNodeData;
      const $nodeStmt = nodeData.nodeStmt;
      const nodeType = nodeData.type;

      let use: Set<string>;
      switch (nodeType) {
        case CfgNodeType.START:
        case CfgNodeType.END:
        case CfgNodeType.SCOPE:
        case CfgNodeType.THEN:
        case CfgNodeType.ELSE:
        case CfgNodeType.LOOP:
          use = new Set<string>();
          break;
        case CfgNodeType.IF:
          use = this.computeUse(($nodeStmt as If | undefined)?.cond);
          break;
        case CfgNodeType.SWITCH:
          use = this.computeUse(($nodeStmt as Switch | undefined)?.condition);
          break;
        case CfgNodeType.CASE: {
          const $switchCondition = (
            ($nodeStmt as Case | undefined)?.getAncestor("switch") as Switch
          ).condition;
          use = ($nodeStmt as Case | undefined)?.isDefault
            ? new Set<string>()
            : this.computeUse($switchCondition);
          break;
        }
        default:
          use = this.computeUse($nodeStmt);
      }
      this.uses.set(node.id(), use);
    }
  }

  /**
   * Computes the LiveIn and LiveOut set of each CFG node
   */
  private computeLiveInOut() {
    for (const node of this.cfg.nodes()) {
      this.liveIn.set(node.id(), new Set());
      this.liveOut.set(node.id(), new Set());
    }

    let liveChanged;
    do {
      liveChanged = false;

      for (const node of this.cfg.nodes()) {
        const def = this.defs.get(node.id());
        const use = this.uses.get(node.id());

        // Save current liveIn and liveOut
        const oldLiveIn = this.liveIn.get(node.id());
        const oldLiveOut = this.liveOut.get(node.id());

        // Compute and save new liveIn
        const diff = LivenessUtils.differenceSets(oldLiveOut, def);
        const newLiveIn = LivenessUtils.unionSets(use, diff);
        this.liveIn.set(node.id(), newLiveIn);

        // Compute and save new liveOut
        let newLiveOut = new Set<string>();
        for (const child of LivenessUtils.getChildren(node)) {
          const childId = child.id();
          const childLiveIn = this.liveIn.get(childId);

          newLiveOut = LivenessUtils.unionSets(newLiveOut, childLiveIn);
        }
        this.liveOut.set(node.id(), newLiveOut);

        // Update liveChanged
        if (
          !LivenessUtils.isSameSet(oldLiveIn, newLiveIn) ||
          !LivenessUtils.isSameSet(oldLiveOut, newLiveOut)
        )
          liveChanged = true;
      }
    } while (liveChanged);
  }
}

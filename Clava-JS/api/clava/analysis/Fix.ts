import { Joinpoint } from "../../Joinpoints.ts";

export default class Fix {
  private node: Joinpoint;
  private fixAction: ($jp: Joinpoint) => void;

  constructor(node: Joinpoint, fixAction: ($jp: Joinpoint) => void) {
    this.node = node;
    this.fixAction = fixAction;
  }

  getNode() {
    return this.node;
  }

  execute() {
    this.fixAction(this.node);
  }
}

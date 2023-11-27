import { LaraJoinPoint } from "lara-js/api/LaraJoinPoint.js";

export default class Fix<T extends LaraJoinPoint> {
  private node: T;
  private fixAction: ($jp: T) => void;

  constructor(node: T, fixAction: ($jp: T) => void) {
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

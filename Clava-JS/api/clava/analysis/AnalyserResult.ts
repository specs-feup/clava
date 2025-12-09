import { Joinpoint } from "../../Joinpoints.js";
import Fix from "./Fix.js";

/**
 * Abstract class created as a model for every result of analyser
 */

export default class AnalyserResult {
  private node: Joinpoint;
  private fix: Fix | undefined;
  name: string;
  message: string;

  constructor(name: string, node: Joinpoint, message: string, fix?: Fix) {
    this.name = name;
    this.node = node;
    this.message = message;
    this.fix = fix;
  }

  getName() {
    return this.name;
  }

  getNode() {
    return this.node;
  }

  getMessage() {
    return this.message;
  }

  performFix() {
    this.fix?.execute();
  }
}

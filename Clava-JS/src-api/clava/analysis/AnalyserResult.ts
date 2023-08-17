import { LaraJoinPoint } from "lara-js/api/LaraJoinPoint.js";
import Fix from "./Fix.js";

/**
 * Abstract class created as a model for every result of analyser
 */

export default class AnalyserResult<T extends LaraJoinPoint> {
  private name: string;
  private node: T;
  private message: string;
  private fix: Fix<T> | undefined;

  constructor(name: string, node: T, message: string, fix?: Fix<T>) {
    this.name = name;
    this.node = node;
    this.message = message;
    this.fix = fix;
  }

  analyse(startNode: T) {
    throw "Not implemented";
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

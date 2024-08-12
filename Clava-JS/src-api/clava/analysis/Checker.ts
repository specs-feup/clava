import { Joinpoint } from "../../Joinpoints.js";
import AnalyserResult from "./AnalyserResult.js";

export default abstract class Checker {
  name: string;

  constructor(name: string) {
    this.name = name;
  }

  abstract check<T extends Joinpoint>(node: T): AnalyserResult | undefined;

  getName() {
    return this.name;
  }
}

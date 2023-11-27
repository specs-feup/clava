import { LaraJoinPoint } from "lara-js/api/LaraJoinPoint.js";
import AnalyserResult from "./AnalyserResult.js";

export default abstract class Checker {
  name: string;

  constructor(name: string) {
    this.name = name;
  }

  abstract check(node: LaraJoinPoint): AnalyserResult<any> | undefined;

  getName() {
    return this.name;
  }
}

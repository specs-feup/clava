import { LaraJoinPoint } from "lara-js/api/LaraJoinPoint.js";
import AnalyserResult from "./AnalyserResult.js";

export default class CheckResult<
  T extends LaraJoinPoint
> extends AnalyserResult<T> {}

import { Joinpoint } from "../../Joinpoints.js";
import AnalyserResult from "./AnalyserResult.js";

export default class ResultList<T extends Joinpoint> {
  fileName: string;
  list: AnalyserResult<T>[] = [];

  constructor(fileName: string) {
    this.fileName = fileName;
  }

  append(result: AnalyserResult<T>) {
    this.list.push(result);
  }
}

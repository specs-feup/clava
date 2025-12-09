import AnalyserResult from "./AnalyserResult.js";

export default class ResultList {
  fileName: string;
  list: AnalyserResult[] = [];

  constructor(fileName: string) {
    this.fileName = fileName;
  }

  append(result: AnalyserResult) {
    this.list.push(result);
  }
}

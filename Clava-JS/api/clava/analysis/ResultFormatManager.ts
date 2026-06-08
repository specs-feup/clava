import { FileJp, Program } from "../../Joinpoints.ts";
import AnalyserResult from "./AnalyserResult.ts";
import ResultList from "./ResultList.ts";

/**
 * Class to format the results from the analyser into a resultList
 */
export default class ResultFormatManager<T extends Program | FileJp> {
  analyserResultList: AnalyserResult[] = [];

  /**
   * Create a new ResultList object with the filename
   *
   * @param $startNode -
   * @returns resultList
   */
  formatResultList($startNode: T) {
    if (Object.entries(this.analyserResultList).length === 0) {
      return;
    }
    const resultList = new ResultList($startNode.name);
    for (const analyserResult of this.analyserResultList) {
      if (analyserResult.getName() === undefined) {
        continue;
      }
      resultList.append(analyserResult);
    }
    return resultList;
  }

  setAnalyserResultList(analyserResultList: AnalyserResult[]) {
    this.analyserResultList = analyserResultList;
  }
}

import ResultList from "./ResultList.js";
/**
 * Class to format the results from the analyser into a resultList
 */
export default class ResultFormatManager {
    analyserResultList = [];
    /**
     * Create a new ResultList object with the filename
     *
     * @param $startNode -
     * @returns resultList
     */
    formatResultList($startNode) {
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
    setAnalyserResultList(analyserResultList) {
        this.analyserResultList = analyserResultList;
    }
}
//# sourceMappingURL=ResultFormatManager.js.map
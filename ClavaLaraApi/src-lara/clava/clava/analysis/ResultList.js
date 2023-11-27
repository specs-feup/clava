export default class ResultList {
    fileName;
    list = [];
    constructor(fileName) {
        this.fileName = fileName;
    }
    append(result) {
        this.list.push(result);
    }
}
//# sourceMappingURL=ResultList.js.map
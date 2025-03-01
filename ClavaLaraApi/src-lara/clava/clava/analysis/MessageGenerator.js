import Io from "@specs-feup/lara/api/lara/Io.js";
import Clava from "../Clava.js";
// Class sorting resultLists and generating an analysis report
export default class MessageGenerator {
    globalFileResultList = {};
    printMessage;
    writeFile;
    constructor(printMessage = true, writeFile = false) {
        this.printMessage = printMessage;
        this.writeFile = writeFile;
    }
    enableFileOutput() {
        this.writeFile = true;
    }
    append(resultList) {
        if (resultList === undefined) {
            return;
        }
        const fileName = resultList.fileName;
        let fileResultList = this.globalFileResultList[fileName];
        if (fileResultList === undefined) {
            fileResultList = [];
            this.globalFileResultList[fileName] = fileResultList;
        }
        for (const result of resultList.list) {
            fileResultList.push(result);
        }
    }
    generateReport() {
        if (this.globalFileResultList === undefined) {
            return;
        }
        const allMessages = {};
        for (const fileName in this.globalFileResultList) {
            const messages = [];
            for (const result of this.globalFileResultList[fileName]) {
                messages.push(fileName + "/l." + result.getNode().line + ": " + result.getMessage());
            }
            if (this.writeFile || this.printMessage) {
                const message = messages.join("\n");
                if (this.printMessage) {
                    console.log(message);
                }
                if (this.writeFile) {
                    const analysisFileName = Io.getPath(Clava.getData().getContextFolder(), "AnalysisReports/analysis_" + fileName + "_report.txt");
                    Io.writeFile(analysisFileName, message);
                }
            }
            // Store messages of file
            allMessages[fileName] = messages;
        }
        return allMessages;
    }
}
//# sourceMappingURL=MessageGenerator.js.map
import Query from "@specs-feup/lara/api/weaver/Query.js";
import StrcpyChecker from "@specs-feup/clava/api/clava/analysis/checkers/StrcpyChecker.js";
import CheckBasedAnalyser from "@specs-feup/clava/api/clava/analysis/CheckBasedAnalyser.js";
import MessageGenerator from "@specs-feup/clava/api/clava/analysis/MessageGenerator.js";

const analyser = new CheckBasedAnalyser();
analyser.addChecker(new StrcpyChecker());

const result = analyser.analyse(Query.search("file").first());

const messageManager = new MessageGenerator(false);
messageManager.append(result);
const allMessages = messageManager.generateReport();

for (const filename in allMessages) {
    const fileMessages = allMessages[filename];

    for (const message of fileMessages) {
        console.log(message.substring(0, message.indexOf(":")));
    }
}

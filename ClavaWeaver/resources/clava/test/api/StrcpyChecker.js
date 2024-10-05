laraImport("weaver.Query");
laraImport("clava.analysis.checkers.StrcpyChecker");
laraImport("clava.analysis.CheckBasedAnalyser");
laraImport("clava.analysis.MessageGenerator");

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

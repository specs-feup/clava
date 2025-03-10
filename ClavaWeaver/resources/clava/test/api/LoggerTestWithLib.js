import Logger from "@specs-feup/clava/api/lara/code/Logger.js";
import Clava from "@specs-feup/clava/api/clava/Clava.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

// Enable SpecsLogger, for testing
Clava.useSpecsLogger = true;

const loggerConsole = new Logger();
const loggerFile = new Logger(false, "log.txt");

// Disable SpecsLogger, in order to compile again woven code
loggerConsole.useSpecsLogger = false;
loggerFile.useSpecsLogger = false;

for (const $call of Query.search("file").search("call")) {
    loggerConsole
        .append("Print double ")
        .appendDouble(2)
        .append(" after " + $call.name)
        .ln();
    loggerConsole.log($call, true);

    loggerConsole.append("Printing again").ln();
    loggerConsole.log($call);

    loggerFile.append("Logging to a file").ln();
    loggerFile.log($call, true);

    loggerFile.append("Logging again to a file").ln();
    loggerFile.log($call);
}

for (const $file of Query.search("file")) {
    console.log($file.code);
}

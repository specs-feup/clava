laraImport("lara.code.Logger");
laraImport("clava.Clava");
laraImport("weaver.Query");

const loggerConsole = new Logger();
const loggerFile = new Logger(false, "log.txt");

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

const appendLogger = new Logger().long("aLong").longLong("aLongLong");

const $vardecl = Query.search("function", "testAppend")
    .search("vardecl", "a")
    .first();
appendLogger.log($vardecl);

for (const $varref of Query.search("function", "testAppendJp").search(
    "varref"
)) {
    new Logger().int($varref).log($varref);
}

for (const $file of Query.search("file")) {
    console.log($file.code);
}

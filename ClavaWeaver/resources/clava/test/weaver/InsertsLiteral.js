laraImport("weaver.Query");

for (const $function of Query.search("function", "fooStmtBeforeAfter")) {
    for (const $stmt of Query.searchFrom($function.body, "statement")) {
        $stmt.insertBefore("int b = 1;");
        $stmt.insertAfter("int c = a + b;");
    }
}

for (const $function of Query.search("function", "fooStmtReplace")) {
    for (const $stmt of Query.searchFrom($function.body, "statement")) {
        $stmt.replaceWith("int a = 100;");
    }
}

for (const $function of Query.search("function", "fooBodyBeforeAfter")) {
    $function.body.insertBefore("int b = 1;");
    $function.body.insertAfter("int c = a + b;");
}

for (const $function of Query.search("function", "fooBodyReplace")) {
    $function.body.replaceWith("int a = 100;");
}

for (const $function of Query.search("function", "fooBodyEmptyBeforeAfter")) {
    $function.body.insertBefore("int b = 1;");
    $function.body.insertAfter("int c = 2 + b;");
}

for (const $function of Query.search("function", "fooBodyEmptyReplace")) {
    $function.body.replaceWith("int a = 100;");
}

for (const $function of Query.search("function", "fooCallBeforeAfter")) {
    for (const $call of Query.searchFrom($function.body, "statement").search(
        "call"
    )) {
        $call.insertBefore("int b = 1;");
        $call.insertAfter("int c = 2 + b;");
    }
}

for (const $function of Query.search("function", "fooCallReplace")) {
    for (const $call of Query.searchFrom($function.body, "statement").search(
        "call"
    )) {
        $call.replaceWith("pow(2.0, 3.0)");
    }
}

for (const $function of Query.search("function", "fooBeforeAfter")) {
    $function.insertBefore("// A comment");
    $function.insertAfter("int GLOBAL = 10;");
}

for (const $function of Query.search("function", "fooReplace")) {
    $function.replaceWith(`void fooReplaced() {
int a = 0;
}`);
}

for (const $call of Query.search("function", "callsInsideFor").search("call")) {
    $call.insertAfter("// After call");
    $call.insertAfter("// After call");
}

// Output code
for (const $file of Query.search("file")) {
    console.log($file.code);
}

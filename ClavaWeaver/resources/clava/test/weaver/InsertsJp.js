laraImport("clava.ClavaJoinPoints");
laraImport("weaver.Query");

const $bDecl = ClavaJoinPoints.varDecl("b", ClavaJoinPoints.integerLiteral(10));
const $cDecl = ClavaJoinPoints.varDecl(
    "c",
    ClavaJoinPoints.integerLiteral("20")
);

for (const $function of Query.search("function", "fooStmtBeforeAfter")) {
    for (const $stmt of Query.searchFrom($function.body, "statement")) {
        $stmt.insertBefore($bDecl);
        $stmt.insertAfter($cDecl);
    }
}

for (const $function of Query.search("function", "fooStmtReplace")) {
    for (const $stmt of Query.searchFrom($function.body, "statement")) {
        $stmt.replaceWith($bDecl);
    }
}

for (const $function of Query.search("function", "fooBodyBeforeAfter")) {
    $function.body.insertBegin($bDecl);
    $function.body.insertEnd($cDecl);
}

for (const $function of Query.search("function", "fooBodyReplace")) {
    $function.body.replaceWith($bDecl);
}

for (const $function of Query.search("function", "fooBodyEmptyBeforeAfter")) {
    $function.body.insertBegin($bDecl);
    $function.body.insertEnd($cDecl);
}

for (const $function of Query.search("function", "fooBodyEmptyReplace")) {
    $function.body.replaceWith($bDecl);
}

for (const $function of Query.search("function", "fooCallBeforeAfter")) {
    for (const $call of Query.searchFrom($function.body, "statement").search(
        "call"
    )) {
        $call.insertBefore($bDecl);
        $call.insertAfter($cDecl);
    }
}

const $double2 = ClavaJoinPoints.doubleLiteral(2.0);
const $double3 = ClavaJoinPoints.doubleLiteral("3.0");
const $doubleType = ClavaJoinPoints.builtinType("double");
const $powCall = ClavaJoinPoints.callFromName(
    "pow",
    $doubleType,
    $double2,
    $double3
);

for (const $function of Query.search("function", "fooCallReplace")) {
    for (const $call of Query.searchFrom($function.body, "statement").search(
        "call"
    )) {
        $call.replaceWith($powCall);
    }
}

for (const $function of Query.search("function", "fooBeforeAfter")) {
    $function.insertBefore($bDecl);
    $function.insertAfter($cDecl);
}

const $dDecl = ClavaJoinPoints.varDecl("d", ClavaJoinPoints.integerLiteral(30));
for (const $function of Query.search("function", "fooReplace")) {
    $function.replaceWith($dDecl);
}

// Output code
for (const $file of Query.search("file")) {
    console.log($file.code);
}

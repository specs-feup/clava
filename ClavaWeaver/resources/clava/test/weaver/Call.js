laraImport("weaver.Query");

let $firstOp = undefined;
for (const $binaryOp of Query.search("binaryOp")) {
    $firstOp = $binaryOp;
}

for (const $call of Query.search("call", "foo")) {
    console.log("foo original:" + $call.code);
    var arg1 = $call.getArg(1);
    $call.setArg(0, arg1);
    console.log("foo new expr arg:" + $call.code);
    $call.setArgFromString(1, "10");
    console.log("foo new string arg:" + $call.code);
    $call.setArg(1, $firstOp);
    console.log("foo op arg:" + $call.code);
}

for (const $call of Query.search("call", "foo2")) {
    // Member access
    console.log("Decl:" + $call.declaration.line);
    console.log("Def:" + $call.definition.line);
}

const functionRegex = /function*/;
for (const $call of Query.search("call")) {
    if (!functionRegex.test($call.name)) {
        continue;
    }
    console.log(
        "function decl of call is definition?: " + $call.function.hasDefinition
    );
}

for (const $call of Query.search("function", "main").search("call", "printf")) {
    console.log(
        "printf call has function decl?: " + ($call.function !== undefined)
    );
    break;
}

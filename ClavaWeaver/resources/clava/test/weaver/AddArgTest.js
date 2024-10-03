laraImport("clava.ClavaJoinPoints");
laraImport("weaver.Query");

for (const $function of Query.search("function", "foo")) {
    if (!$function.isImplementation) {
        continue;
    }

    $function.addParam("char* str");
}

for (const $function of Query.search("function", "bar")) {
    if ($function.isImplementation) {
        continue;
    }

    $function.addParam("int num");
}

for (const $call of Query.search("call", "bar")) {
    $call.addArg("0", ClavaJoinPoints.builtinType("int"));
}

for (const $call of Query.search("call", "foo")) {
    const type = ClavaJoinPoints.pointerFromBuiltin("unsigned char");
    $call.addArg('"foo"', type);
}


console.log(Query.root().code);
console.log("---------------");

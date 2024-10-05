laraImport("weaver.Query");

for (const $function of Query.search("function")) {
    console.log("Select function " + $function.name);
}

for (const _ of Query.search("function", "foo")) {
    console.log("Select function foo");
}

for (const _ of Query.search("function", {line: 1})) {
    console.log("Select function at line 1");	
}

for (const _ of Query.search("function").search("call")) {
    console.log("Select calls");
}


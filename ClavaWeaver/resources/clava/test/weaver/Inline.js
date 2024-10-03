laraImport("weaver.Query");

for (const $call of Query.search("function", "main").search("call")) {
    $call.inline();
}

for (const $function of Query.search("function", "main")) {
    console.log($function.code);
}

laraImport("weaver.Query");

for (const $call of Query.search("loop").search("call")) {
    $call.inline();
}

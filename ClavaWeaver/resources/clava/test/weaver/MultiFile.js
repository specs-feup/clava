laraImport("weaver.Query");

// Rename record in a header, change should propagate
Query.search("record", "A").first().name = "A_renamed";

Query.search("function", "template_foo").first().name = "template_foo_renamed";

for (const $call of Query.search("call", "template_foo_2")) {
    $call.definition.name = "template_foo_2_renamed";
    break;
}

for (const $call of Query.search("call", "template_foo_3")) {
    $call.declaration.name = "template_foo_3_renamed";
    break;
}

console.log(Query.root().code);

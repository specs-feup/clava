import Query from "@specs-feup/lara/api/weaver/Query.js";

// Rename record in a header, change should propagate
Query.search("record", "A").first().name = "A_renamed";

Query.search("function", "template_foo").first().name = "template_foo_renamed";

for (const $call of Query.search("call", "template_foo_2")) {
    // Included header definitions remain distinct occurrences. Rename the entity through its declaration
    // instead of selecting an arbitrary project-wide definition.
    $call.declaration.name = "template_foo_2_renamed";
    break;
}

for (const $call of Query.search("call", "template_foo_3")) {
    $call.declaration.name = "template_foo_3_renamed";
    break;
}

console.log(Query.root().code);

laraImport("clava.ClavaJoinPoints");
laraImport("weaver.Query");

const $vardecl = Query.search("function", "multiMatrix")
    .search("vardecl", "s_a")
    .first();
for (const $attr of $vardecl.attrs) {
    console.log("Attr kind: " + $attr.kind);
    console.log("Attr code: " + $attr.code);
}

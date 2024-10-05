laraImport("weaver.Query");

for (const $vardecl of Query.search("vardecl", "a")) {
    console.log("Type kind:" + $vardecl.type.kind);
    console.log("Is builtin? " + $vardecl.type.astIsInstance("BuiltinType"));
    console.log("Is decl? " + $vardecl.astIsInstance("Decl"));
}
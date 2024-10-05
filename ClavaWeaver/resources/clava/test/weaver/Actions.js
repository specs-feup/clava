laraImport("clava.ClavaJoinPoints");
laraImport("weaver.Query");

for (const $vardecl of Query.search("function")
    .search("statement")
    .search("vardecl")) {
    if ($vardecl.type.code === "double") {
        $vardecl.setType(ClavaJoinPoints.builtinType("float"));
    } else if ($vardecl.type.code === "int") {
        $vardecl.setType(ClavaJoinPoints.builtinType("unsigned int"));
    }
}

console.log(Query.root().code);


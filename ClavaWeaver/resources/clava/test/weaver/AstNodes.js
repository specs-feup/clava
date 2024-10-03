laraImport("weaver.Query");
laraImport("weaver.Ast");

for (const $if of Query.search("function", "testNodes").search("if")) {
    console.log("astNumChildren  = " + $if.astNumChildren);
    console.log("astChildren = " + $if.astChildren);
    console.log("astChild(0) = " + $if.getAstChild(0));
    console.log("numChildren  = " + $if.numChildren);
    console.log("children = " + $if.children);
    console.log("child(0) = " + $if.getChild(0));
    console.log('astAncestor("Decl") = ' + $if.getAstAncestor("Decl").name);
}

console.log("\n\nAst API:");
for (const $if of Query.search("function", "testNodes").search("if")) {
    const astNode = $if.node;

    console.log("astNumChildren  = " + Ast.getNumChildren(astNode));
    console.log(
        "astChildren = " +
            Ast.getChildren(astNode).map((node) =>
                node.getClass().getSimpleName()
            )
    );
}

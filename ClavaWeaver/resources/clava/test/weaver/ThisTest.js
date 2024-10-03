laraImport("weaver.Query");

console.log("This decl");
for (const $memberAccess of Query.search("function", "getVolume").search(
    "memberAccess"
)) {
    console.log($memberAccess.base.decl.name);
}

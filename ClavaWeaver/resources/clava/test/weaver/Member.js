laraImport("weaver.Query");

const $memberAccess = Query.search("memberAccess").first();
console.log("Member: " + $memberAccess.name);
console.log(
    "Member decl type: " + $memberAccess.getValue("memberDecl").joinPointType
);
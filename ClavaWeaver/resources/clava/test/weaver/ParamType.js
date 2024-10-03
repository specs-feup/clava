laraImport("weaver.Query");

for (const $varref of Query.search("function", "error_norm")
    .search("loop")
    .search("varref", (varref) => varref.name != "m")) {
    console.log("varref_name : " + $varref.name + " #" + $varref.line);
    console.log("\t\tisArray = " + $varref.vardecl.type.isArray);
    console.log("\t\tisArray = " + $varref.type.isArray);
    console.log($varref.ast);
}

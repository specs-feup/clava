laraImport("weaver.Query");

console.log("VARREF:");
for (const $varref of Query.search("varref")) {
    console.log($varref.code + "->" + $varref.use);
}

console.log("ARRAY ACCESS:");
for (const $arrayAccess of Query.search("arrayAccess")) {
    console.log($arrayAccess.code + "->" + $arrayAccess.use);
}

laraImport("weaver.Query");

for (const $arrayAccess of Query.search("arrayAccess")) {
    console.log("Array var: " + $arrayAccess.arrayVar.name);
    console.log($arrayAccess.code + "->" + $arrayAccess.use);
}

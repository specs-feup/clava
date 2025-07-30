import Query from "@specs-feup/lara/api/weaver/Query.js";

for (const $arrayAccess of Query.search("arrayAccess")) {
    console.log("Array var: " + $arrayAccess.arrayVar.name);
    console.log($arrayAccess.code + "->" + $arrayAccess.use);
}

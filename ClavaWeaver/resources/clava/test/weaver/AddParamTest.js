import Query from "@specs-feup/lara/api/weaver/Query.js";

for (const $function of Query.search("function", "foo")) {
    $function.addParam("char* str");
    console.log($function.code);
}

for (const $function of Query.search("function", "bar")) {
    $function.addParam("int num");
    console.log($function.code);
    console.log("---------------");
}

laraImport("weaver.Query");

for (const $function of Query.search("function", "foo")) {
    $function.addParam("char* str");
    console.log($function.code);
}

for (const $function of Query.search("function", "bar")) {
    $function.addParam("int num");
    console.log($function.code);
    console.log("---------------");
}

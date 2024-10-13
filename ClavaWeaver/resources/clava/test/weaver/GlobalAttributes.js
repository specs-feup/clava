import Query from "@specs-feup/lara/api/weaver/Query.js";

for (const $vardecl of Query.search("function", "main").search(
    "vardecl",
    "a"
)) {
    console.log("- Testing keys, setValue, getValue -");
    const $type = $vardecl.type;
    console.log("type keys: " + $type.keys);
    console.log("type builtin kind: " + $type.getValue("builtinKind"));
    $vardecl.type = $type.copy().setValue("builtinKind", "float");
    console.log("Changed vardecl: " + $vardecl.code);
}

console.log("Inside header:");
for (const $function of Query.search("function", "insideHeader")) {
    for (const $jp of $function.descendants) {
        if ($jp.isInsideHeader) {
            console.log($jp.joinPointType + " -> " + $jp.code);
        }
    }
}

laraImport("clava.ClavaJoinPoints");
laraImport("weaver.Query");

for (const $file of Query.search("file")) {
    var type = ClavaJoinPoints.builtinType("int");
    $file.addGlobal("num_threads", type, "16");
    console.log($file.code);
    console.log("---------------");
}

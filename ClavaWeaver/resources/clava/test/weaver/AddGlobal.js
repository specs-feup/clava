import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

for (const $file of Query.search("file")) {
    var type = ClavaJoinPoints.builtinType("int");
    $file.addGlobal("num_threads", type, "16");
    console.log($file.code);
    console.log("---------------");
}

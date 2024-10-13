import Query from "@specs-feup/lara/api/weaver/Query.js";
import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";

// Add file
const $file = ClavaJoinPoints.file("no_parsing.cpp");
$file.insert("after", "int main() { return 0;}");

Query.root().addFile($file);

console.log(Query.root().code);

laraImport("weaver.Query");
laraImport("clava.ClavaJoinPoints");

// Add file
const $file = ClavaJoinPoints.file("no_parsing.cpp");
$file.insert("after", "int main() { return 0;}");

Query.root().addFile($file);

console.log(Query.root().code);

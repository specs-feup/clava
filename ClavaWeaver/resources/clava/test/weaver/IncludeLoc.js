laraImport("weaver.Query")

for (const include of Query.search("include")) {
    println(include.code + " -> " + include.line);
}
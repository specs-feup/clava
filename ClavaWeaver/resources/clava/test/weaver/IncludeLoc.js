import Query from "@specs-feup/lara/api/weaver/Query.js"

for (const include of Query.search("include")) {
    console.log(include.code + " -> " + include.line);
}

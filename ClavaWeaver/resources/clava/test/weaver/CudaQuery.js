laraImport("weaver.Query");

for (const $jp of Query.search("arrayAccess")) {
    console.log("arrayAccess var: " + $jp.name);
}

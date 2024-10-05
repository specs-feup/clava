laraImport("weaver.Query");

const $main = Query.search("function", "main").first();

for (const JP of Query.searchFrom($main.body, "statement")) {
    console.log("code : " + JP.code + "  jpType : " + JP.joinPointType);
}

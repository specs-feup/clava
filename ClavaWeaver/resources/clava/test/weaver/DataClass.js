laraImport("weaver.Query");

for (const $method of Query.search("method")) {
    const tinits = $method.getValue("constructorInits");

    for (const tinit of tinits) {
        console.log("InitExpr class: " + tinit.getValue("initExpr").getClass());
    }
}

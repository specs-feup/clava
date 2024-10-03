laraImport("weaver.Query");

for (const $class of Query.search("class")) {
    console.log("Class: " + $class.name);

    for (const $base of $class.bases) {
        console.log("- " + $base.name);
    }
}

import Query from "@specs-feup/lara/api/weaver/Query.js";

for (const $class of Query.search("class")) {
    console.log("Class: " + $class.name);

    for (const $base of $class.bases) {
        console.log("- " + $base.name);
    }
}

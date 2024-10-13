import Query from "@specs-feup/lara/api/weaver/Query.js";

for (const $jp of Query.search("arrayAccess")) {
    console.log("arrayAccess var: " + $jp.name);
}

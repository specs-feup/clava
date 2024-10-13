import Query from "@specs-feup/lara/api/weaver/Query.js";

for (const $call of Query.search("loop").search("call")) {
    $call.inline();
}

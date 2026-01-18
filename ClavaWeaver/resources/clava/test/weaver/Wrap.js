import Query from "@specs-feup/lara/api/weaver/Query.js";

let lastWrapped = undefined;

for (const $call of Query.search("call")) {
    $call.wrap("wrap_" + $call.name);
    // At this point, the call joinpoint changed and became the wrapped call
    lastWrapped = $call.name;
}

for (const $call of Query.search("function", lastWrapped).search("call")) {
    $call.insertBefore("// Before call");
}

console.log(Query.root().code);

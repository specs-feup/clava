import Clava from "@specs-feup/clava/api/clava/Clava.js";
import TransformSwitchToIf from "@specs-feup/clava/api/clava/pass/TransformSwitchToIf.js";

import Query from "@specs-feup/lara/api/weaver/Query.js";

const switchTransformer = new TransformSwitchToIf(true);

for (const $switch of Query.search("switch")) {
    switchTransformer.transformJoinpoint($switch);
}

Clava.rebuild();
console.log(Query.search("function", "foo").first().code);
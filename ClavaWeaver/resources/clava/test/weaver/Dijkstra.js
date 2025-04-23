import Weaver from "@specs-feup/lara/api/weaver/Weaver.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

// FIXME: currentTime() is a method only available in the MasterWeaver class and not in any Weaver that is actually shipped.
// This test only runs in the java test runner and does not work in the real world.
const tic = Weaver.getWeaverEngine().currentTime();
let acc = 0;

let toc;
for (const $function of Query.search("function", { name: "exact_rhs" })) {
    for (const _ of Query.searchFrom($function.body, "varref")) {
        toc = JavaTypes.LaraI.getThreadLocalLarai()
            .getWeaver()
            .currentTime();
        console.log("Time:" + (toc - tic));
        acc += 1;
    }
}

const toc2 = Weaver.getWeaverEngine().currentTime();
console.log("Time 2:" + (toc2 - toc));

console.log("Found " + acc + " variables");

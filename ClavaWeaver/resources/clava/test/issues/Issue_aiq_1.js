import Query from "@specs-feup/lara/api/weaver/Query.js"
import { Loop, LoopKind } from "@specs-feup/clava/api/Joinpoints.js"

const forLoops = Query.search(Loop, (loop) => loop.kind === LoopKind.for).get();

for (const forLoop of forLoops) {
    console.log("Cond: {\n" + forLoop.cond.code + "\n}\n");

    if (forLoop.hasCondRelation) {
        console.log(forLoop.condRelation);
    }
}

console.log("Found " + forLoops.length + " forloops.")
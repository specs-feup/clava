import Query from "@specs-feup/lara/api/weaver/Query.js";
import { FunctionJp } from "@specs-feup/clava/api/Joinpoints.js";
const functionJp = Query.search(FunctionJp, { name: "foo2", isImplementation: true }).get()[0];
console.log("Decl count before: ", functionJp.declarationJps.length); // 1
functionJp.setParams([]);
console.log("Decl count after: ", functionJp.declarationJps.length);
const decl = Query.search(FunctionJp, { name: "foo2", isImplementation: false }).get()[0];
console.log("Declaration: ", decl.code); // extern ...
console.log("Decl astID: ", decl.astId);
console.log("Definition Jp: ", decl.definitionJp);

/*
import Query from "@specs-feup/lara/api/weaver/Query.js";
import {FunctionJp} from "@specs-feup/clava/api/Joinpoints.js";
import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";

for (const func of Query.search(FunctionJp, {"isImplementation": true})) {
    console.log("Definition: " + func.name + "@" + func.line);
}

for (const func of Query.search(FunctionJp, {"isImplementation": false})) {
    console.log("Declaration: " + func.name + "@" + func.line);
}

// Create and  set parameters
for (const func of Query.search(FunctionJp, {"isImplementation": true})) {
    let p = [...func.params];
    p.push(ClavaJoinPoints.param("b", ClavaJoinPoints.builtinType("int")));

    func.setParams(p);

    const functionDecl = Query.search(FunctionJp, {name: "foo", isImplementation: false}).get()[0];
    console.log(functionDecl.definitionJp) // returns undefined, not functionJp
}

*/
// Set parameters

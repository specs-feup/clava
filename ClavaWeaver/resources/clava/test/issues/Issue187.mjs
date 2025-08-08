import Query from "@specs-feup/lara/api/weaver/Query.js";
import {FunctionJp} from "@specs-feup/clava/api/Joinpoints.js";
import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";


const functionJp = Query.search(FunctionJp).first();

const newParam1 = ClavaJoinPoints.param(
    "b",
    ClavaJoinPoints.builtinType("int")
);

const newParam2 = ClavaJoinPoints.param(
    "c",
    ClavaJoinPoints.builtinType("int")
);

functionJp.setParams([newParam1, newParam2]);

const functionDecl = Query.search(FunctionJp, {name: "foo", isImplementation: false}).get()[0];
console.log(functionDecl);
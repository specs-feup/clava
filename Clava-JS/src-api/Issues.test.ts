import { registerSourceCode } from "@specs-feup/lara/jest/jestHelpers.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";
import { FunctionJp, Loop } from "./Joinpoints.js";

const code = `
double foo2(int x, int y) {
 return 0.0;
}
 
double foo2(int x, int y);


unsigned int test_17_4_2() {
    foo2(1, 1);
}
`;

describe("Query", () => {
  registerSourceCode(code);

  it("Issue 187: should keep declarations/definitions after changing parameters", () => {
  const functionJp =   Query.search(FunctionJp, { name: "foo2", isImplementation: true }).get()[0];
console.log("Decl count before: ", functionJp.declarationJps.length); // 1
    expect(functionJp.declarationJps.length).toBe(1);
functionJp.setParams([]);
console.log("Decl count after: ", functionJp.declarationJps.length);
    expect(functionJp.declarationJps.length).toBe(1);
const decl = Query.search(FunctionJp, { name: "foo2", isImplementation: false }).get()[0];
console.log("Declaration: ", decl.code); // extern ...
console.log("Decl astID: ", decl.astId);
console.log("Definition Jp: ", decl.definitionJp);

  });


});
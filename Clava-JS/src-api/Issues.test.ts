import { registerSourceCode } from "@specs-feup/lara/jest/jestHelpers.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";
import { FunctionJp, Loop } from "@specs-feup/clava/api/Joinpoints.js";
import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";

const code187 = `
int foo(int a);
int foo(int a) {return 0;}`;

describe("issue187", () => {
  registerSourceCode(code187);

  it("should return a valid definition", () => {
    const functionJp = Query.search(FunctionJp).first();
    expect(functionJp).toBeDefined();

    const newParam1 = ClavaJoinPoints.param(
      "b",
      ClavaJoinPoints.builtinType("int")
    );

    const newParam2 = ClavaJoinPoints.param(
      "c",
      ClavaJoinPoints.builtinType("int")
    );

    if (functionJp === undefined) {
      fail();
    }

    functionJp.setParams([newParam1, newParam2]);

    const functionDecl = Query.search(FunctionJp, {name: "foo", isImplementation: false}).get()[0];
    expect(functionDecl).toBeInstanceOf(FunctionJp)
    expect(functionDecl.definitionJp).toBeInstanceOf(FunctionJp)
  });
});

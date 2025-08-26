import {
  registerSourceCode,
  registerSourceCodes,
} from "@specs-feup/lara/jest/jestHelpers.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";
import { FunctionJp } from "@specs-feup/clava/api/Joinpoints.js";
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
      throw new Error("functionJp is undefined");
    }

    functionJp.setParams([newParam1, newParam2]);

    const functionDecl = Query.search(FunctionJp, {
      name: "foo",
      isImplementation: false,
    }).get()[0];
    expect(functionDecl).toBeInstanceOf(FunctionJp);
    expect(functionDecl.definitionJp).toBeInstanceOf(FunctionJp);
  });
});

const code190_1 = `
static int foo() {
    return 0;
}`;

const code190_2 = `
static double foo() {
    return 0.0;
}`;

describe("issue190", () => {
  registerSourceCodes({ "file1.c": code190_1, "file2.c": code190_2 });

  it("the right function should be renamed", () => {
    const functionJp2 = Query.search(FunctionJp, (jp) =>
      jp.filepath.endsWith("file2.c")
    ).first();

    if (functionJp2 === undefined) {
      throw new Error("functionJp2 is undefined");
    }

    functionJp2.setName("bar");
    expect(functionJp2.name).toBe("bar");

    const functionJp1 = Query.search(FunctionJp, (jp) =>
      jp.filepath.endsWith("file1.c")
    ).first();

    if (functionJp1 === undefined) {
      throw new Error("functionJp1 is undefined");
    }

    expect(functionJp1.name).toBe("foo");
  });
});

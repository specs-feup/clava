import { registerSourceCodes } from "@specs-feup/lara/vitest/weaverTestHelpers.ts";
import Query from "@specs-feup/lara/api/weaver/Query.ts";
import { Call, FunctionJp } from "./Joinpoints.ts";
import ClavaJoinPoints from "./clava/ClavaJoinPoints.ts";

describe("cross-TU lookup through joinpoints and actions", () => {
  registerSourceCodes({
    "use.cpp": "int target(int); int use(){return target(7);}",
    "impl.cpp": "int target(int x){return x+1;}",
  });

  function target(implementation: boolean): FunctionJp {
    const result = Query.search(FunctionJp, {
      name: "target",
      isImplementation: implementation,
    }).first();
    if (result === undefined) throw new Error("Missing target declaration");
    return result;
  }

  it("resolves a call to the implementation in another file", () => {
    const call = Query.search(Call).first();
    expect(call).toBeDefined();
    expect(call?.definition?.filepath).toBe(target(true).filepath);
    expect(target(false).definitionJp.filepath).toBe(target(true).filepath);
  });

  it("keeps declaration lookup valid after changing parameters", () => {
    const declaration = target(false);
    expect(declaration.definitionJp).toBeInstanceOf(FunctionJp);
    declaration.setParams([
      ClavaJoinPoints.param("value", ClavaJoinPoints.builtinType("int")),
    ]);
    expect(declaration.params).toHaveLength(1);
    expect(target(true).params).toHaveLength(1);
    expect(declaration.definitionJp.filepath).toBe(target(true).filepath);
  });

  it("supports staged parameter and caller updates", () => {
    const declaration = target(false);
    declaration.setParams([
      ClavaJoinPoints.param("value", ClavaJoinPoints.builtinType("int")),
      ClavaJoinPoints.param("extra", ClavaJoinPoints.builtinType("double")),
    ]);
    expect(declaration.params).toHaveLength(2);
    expect(target(true).params).toHaveLength(2);
    expect(declaration.definitionJp.filepath).toBe(target(true).filepath);
    const call = Query.search(Call).first();
    call?.addArg("0", ClavaJoinPoints.builtinType("double"));
    expect(call?.code).toContain("target(7, 0)");
  });

  it("keeps declaration lookup valid after changing the return type", () => {
    const declaration = target(false);
    expect(declaration.definitionJp).toBeInstanceOf(FunctionJp);
    declaration.setReturnType(ClavaJoinPoints.builtinType("double"));
    expect(declaration.returnType.code).toBe("double");
    expect(target(true).returnType.code).toBe("double");
    expect(declaration.definitionJp.filepath).toBe(target(true).filepath);
  });
});

describe("cross-TU static function actions", () => {
  registerSourceCodes({
    "left.cpp": "static int helper(){return 1;} int left(){return helper();}",
    "right.cpp": "static int helper(){return 2;} int right(){return helper();}",
  });

  it("renames only the selected TU's static function", () => {
    const functions = Query.search(FunctionJp, { name: "helper" }).get();
    const left = functions.find((jp) => jp.filepath.endsWith("left.cpp"));
    const right = functions.find((jp) => jp.filepath.endsWith("right.cpp"));
    if (left === undefined || right === undefined) throw new Error("Missing static helper");
    expect(left.definitionJp.filepath).toBe(left.filepath);
    expect(right.definitionJp.filepath).toBe(right.filepath);
    right.setName("renamed");
    expect(right.name).toBe("renamed");
    expect(left.name).toBe("helper");
    expect(right.definitionJp.filepath).toBe(right.filepath);
    expect(left.definitionJp.filepath).toBe(left.filepath);
  });
});

describe("shared header source ownership through joinpoints", () => {
  registerSourceCodes({
    "api.hpp": "inline int target(){return 7;} struct R{int method(){return 8;}};",
    "left.cpp": '#include "api.hpp"\nint left(){R r;return target()+r.method();}',
    "right.cpp": '#include "api.hpp"\nint right(){R r;return target()+r.method();}',
  });

  it("returns the header definition for calls from both source files", () => {
    const calls = Query.search(Call).get().filter(call => call.name === "target");
    expect(calls).toHaveLength(2);
    const definitions = calls.map(call => call.definition);
    for (const definition of definitions) expect(definition?.filepath.endsWith("api.hpp")).toBe(true);
    expect(definitions[0]?.astId).toBe(definitions[1]?.astId);
    definitions[0]?.setName("changed");
    for (const call of calls) expect(call.code).toBe("changed()");
  });

  it("renames a header method and updates stored member-call names", () => {
    const calls = Query.search(Call).get().filter(call => call.name === "method");
    expect(calls).toHaveLength(2);
    calls[0]?.definition?.setName("changed_method");
    for (const call of calls) expect(call.code).toBe("r.changed_method()");
  });
});

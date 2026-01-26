import { registerSourceCode } from "@specs-feup/lara/jest/jestHelpers.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";
import { FunctionJp, Vardecl } from "../../Joinpoints.js";
import NormalizeToSubset from "../opt/NormalizeToSubset.js";

const codeWithExistingDecompVars = `
int foo(int a) {
  int decomp_0 = 5;
  int decomp_1 = 10;
  int result = (a + 1) * (decomp_0 + decomp_1);
  return result;
}`;

describe("StatementDecomposer duplicate symbols", () => {
  registerSourceCode(codeWithExistingDecompVars);

  it("should not create duplicate decomp_ symbols when normalizing", () => {
    const functionJp = Query.search(FunctionJp, { name: "foo" }).first();
    
    if (functionJp === undefined) {
      fail("Function not found");
    }

    // Get all variable names before normalization
    const varsBefore = Query.searchFrom(functionJp, Vardecl).map(v => v.name);
    expect(varsBefore).toContain("decomp_0");
    expect(varsBefore).toContain("decomp_1");

    // Apply normalization
    NormalizeToSubset(functionJp);

    // Get all variable names after normalization
    const varsAfter = Query.searchFrom(functionJp, Vardecl).map(v => v.name);
    
    // Check that all variable names are unique
    const uniqueVars = new Set(varsAfter);
    expect(uniqueVars.size).toBe(varsAfter.length);
    
    // Ensure no duplicate decomp_0 or decomp_1
    const decompCounts: Record<string, number> = {};
    for (const varName of varsAfter) {
      if (varName.startsWith("decomp_")) {
        decompCounts[varName] = (decompCounts[varName] || 0) + 1;
      }
    }
    
    for (const [varName, count] of Object.entries(decompCounts)) {
      expect(count).toBe(1);
    }
  });
});

const codeMultipleNormalization = `
int bar(int x) {
  return (x + 1) * (x + 2);
}`;

describe("StatementDecomposer multiple normalizations", () => {
  registerSourceCode(codeMultipleNormalization);

  it("should not create duplicate symbols when normalizing multiple times", () => {
    const functionJp = Query.search(FunctionJp, { name: "bar" }).first();
    
    if (functionJp === undefined) {
      fail("Function not found");
    }

    // Apply normalization twice
    NormalizeToSubset(functionJp);
    NormalizeToSubset(functionJp);

    // Get all variable names
    const vars = Query.searchFrom(functionJp, Vardecl).map(v => v.name);
    
    // Check that all variable names are unique
    const uniqueVars = new Set(vars);
    expect(uniqueVars.size).toBe(vars.length);
  });
});

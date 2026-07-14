import { registerSourceCode } from "@specs-feup/lara/vitest/weaverTestHelpers.ts";
import Query from "@specs-feup/lara/api/weaver/Query.ts";
import { ExprStmt } from "../Joinpoints.ts";
import ClavaJoinPoints from "./ClavaJoinPoints.ts";

const code = `int main() {
    int a = 0, b = 0;
    b = a + 1;
    a  = b + 1;
}
`;

describe("ClavaJoinPoints", () => {
  registerSourceCode(code);

  it("Creates a new Scope from ExpressionStatements", () => {
    const exprStmts = Query.search(ExprStmt).get();

    expect(exprStmts.length).toBe(2);

    const scope = ClavaJoinPoints.scope(...exprStmts);

    expect(scope).toBeDefined();
    expect(scope.children.length).toBe(2);
  });
});

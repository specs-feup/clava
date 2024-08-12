import { registerSourceCode } from "lara-js/jest/jestHelpers.js";
import Query from "lara-js/api/weaver/Query.js";
import { ExprStmt } from "../Joinpoints.js";
import ClavaJoinPoints from "./ClavaJoinPoints.js";

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

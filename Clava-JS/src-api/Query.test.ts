import { registerSourceCode } from "lara-js/jest/jestHelpers.js";
import Query from "lara-js/api/weaver/Query.js";
import { FunctionJp, Joinpoint, Loop } from "./Joinpoints.js";

const code = `void query_loop() {
    for(int i=0; i<10; i++) {
    }

    for(int j1=0; j1<10; j1++) {
        for(int j2=0; j2<10; j2++) {
        }
    }

    for(int k1=0; k1<10; k1++) {
        for(int k2=0; k2<10; k2++) {
            for(int k3=0; k3<10; k3++) {
            }
        }
    }
}

int query_empty() {
    int a;
    return a + 2;
}

int query_regex() {
    return 0;
}
`;

describe("Query", () => {
  registerSourceCode(code);

  it("should be able to search for a function", () => {
    const lst: number[][] = [];
    for (const query of Query.search(FunctionJp, "query_loop")
      .search(Loop)
      .search(Loop)
      .chain()) {
      lst.push((query["loop"] as Loop).rank);
    }
    expect(lst.length).toBe(4);
    expect(lst[0]).toEqual([2, 1]);
    expect(lst[1]).toEqual([3, 1]);
    expect(lst[2]).toEqual([3, 1, 1]);
    expect(lst[3]).toEqual([3, 1, 1]);
  });

  it("should be able to search for a function2", () => {
    const lst: number[][] = [];

    for (const query of Query.search(FunctionJp, "query_loop")
      .search(Loop)
      .scope(Loop)
      .chain()) {
      lst.push((query["loop"] as Loop).rank);
    }
    expect(lst.length).toBe(3);
    expect(lst[0]).toEqual([2, 1]);
    expect(lst[1]).toEqual([3, 1]);
    expect(lst[2]).toEqual([3, 1, 1]);
  });

  it("should be able to search for a function3", () => {
    const lst: number[][] = [];

    for (const query of Query.search(FunctionJp, "query_loop")
      .search(Loop, { isOutermost: true })
      .scope(Loop)
      .chain()) {
      lst.push((query["loop"] as Loop).rank);
    }
    expect(lst.length).toBe(2);
    expect(lst[0]).toEqual([2, 1]);
    expect(lst[1]).toEqual([3, 1]);
  });

  it("should be able to search for a function4", () => {
    const lst: string[] = [];
    for (const query of Query.search(FunctionJp, "query_empty")
      .scope()
      .chain()) {
      lst.push((query["joinpoint"] as Joinpoint).joinPointType);
    }
    expect(lst.length).toBe(2);
    expect(lst[0]).toBe("declStmt");
    expect(lst[1]).toBe("returnStmt");
  });

  it("should be able to search for a function5", () => {
    const lst: string[] = [];
    for (const query of Query.search(FunctionJp, "query_loop")
      .search(Loop)
      .search(Loop)
      .search(Loop)
      .chain()) {
      lst.push(...Object.keys(query).sort());
    }
    expect(lst.length).toBe(7);
    expect(lst[0]).toBe("_starting_point");
    expect(lst[1]).toBe("function");
    expect(lst[2]).toBe("function_0");
    expect(lst[3]).toBe("loop");
    expect(lst[4]).toBe("loop_0");
    expect(lst[5]).toBe("loop_1");
    expect(lst[6]).toBe("loop_2");
  });

  it("should be able to search for a function6", () => {
    const lst: string[] = [];
    for (const query of Query.search(FunctionJp, /_regex/)) {
      lst.push((query as FunctionJp).name);
    }
    expect(lst.length).toBe(1);
    expect(lst[0]).toBe("query_regex");
  });
});

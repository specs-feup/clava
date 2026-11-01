import { registerSourceCode } from "@specs-feup/lara/vitest/weaverTestHelpers.ts";
import Query from "@specs-feup/lara/api/weaver/Query.ts";
import {
  clearJoinpointMappers,
  getJoinpointMappers,
  LaraJoinPoint,
  registerJoinpointMapper,
  registerJoinpointMapperFunction,
  unwrapJoinPoint,
  wrapJoinPoint,
} from "@specs-feup/lara/api/LaraJoinPoint.ts";
import { Expression, FileJp, FunctionJp, Joinpoint, Loop, Program, Vardecl } from "./Joinpoints.ts";

// Covers many join point and type classes, so the per-class wrap cache sees lots of classes
const code = `#include <vector>
typedef unsigned long ulong_t;
enum Color { RED, GREEN };
struct Point { int x; float y; };
template <typename T> class Box {
public:
  T value;
  Box(T v) : value(v) {}
  T get() const { return this->value; }
};
namespace ns { int counter = 0; }
int sum(const int *values, int n, int &out) {
  int total = 0;
  // comment
  for (int i = 0; i < n; i++) { total += values[i]; }
  while (total > 100) { total /= 2; }
  do { total++; } while (total < 0);
  switch (total) { case 1: total = 2; break; default: break; }
  out = total > 0 ? total : -total;
  return total;
}
int main() {
  int arr[3] = {1, 2, 3};
  int out;
  ulong_t big = (ulong_t) sum(arr, 3, out);
  Point p = {1, 2.0f};
  p.x = static_cast<int>(p.y);
  Box<int> box(4);
  std::vector<int> v;
  v.push_back(box.get());
  auto square = [](int a) { return a * a; };
  int *heap = new int(square(ns::counter));
  delete heap;
  Color c = GREEN;
  #pragma omp parallel for
  for (int k = 0; k < 2; k++) { arr[k] = sizeof(Point) + c; }
  if (big) goto end;
end:
  return 0;
}
`;

// Assertions compare classes, never join points: printing a join point in a failure
// message runs its getters through node-java, which can abort the test worker
const classOf = (value: unknown) => (value as object | undefined)?.constructor;

// node-java objects, typed by the methods these tests call
type JavaObject = { getClass(): { getName(): string } };
type JavaJoinPoint = JavaObject & { joinPointType(): string };
type JavaNamed = { getName(): string };

// Same lookup the uncached implementation did for every instance
function mappedClass(javaJp: JavaJoinPoint): typeof LaraJoinPoint {
  const jpType = javaJp.joinPointType();
  const mappers = getJoinpointMappers();
  for (let i = mappers.length - 1; i >= 0; i--) {
    const jpClass = mappers[i].toJpClass(jpType);
    if (jpClass) return jpClass;
  }
  throw new Error("No mapper for " + jpType);
}

// Java join points of the AST, plus the types reachable from it (types are not AST children)
function javaJoinPoints(): JavaJoinPoint[] {
  const result: JavaJoinPoint[] = [];
  for (const jp of [Query.root(), ...Query.root().descendants]) {
    result.push(unwrapJoinPoint(jp));
    if (jp instanceof Expression || jp instanceof Vardecl || jp instanceof FunctionJp) {
      const type = jp.type;
      if (type) result.push(unwrapJoinPoint(type));
    }
  }
  return result;
}

describe("wrapJoinPoint", () => {
  registerSourceCode(code);

  // Mappers are module state shared with other test files in this worker
  const savedMappers = [...getJoinpointMappers()];
  const restoreMappers = () => {
    clearJoinpointMappers();
    getJoinpointMappers().push(...savedMappers);
  };
  afterEach(restoreMappers);

  it("wraps every join point as the class its joinPointType maps to, with a cold and a warm cache", () => {
    const javaJps = javaJoinPoints();
    restoreMappers(); // starts with an empty cache

    const jpTypesPerJavaClass = new Map<string, Set<string>>();
    for (let round = 0; round < 2; round++) {
      for (const javaJp of javaJps) {
        const wrapped = wrapJoinPoint(javaJp);
        expect(classOf(wrapped)).toBe(mappedClass(javaJp));
        expect(wrapped._javaObject === javaJp).toBe(true);

        const javaClass = javaJp.getClass().getName();
        const jpTypes = jpTypesPerJavaClass.get(javaClass) ?? new Set<string>();
        jpTypes.add(javaJp.joinPointType());
        jpTypesPerJavaClass.set(javaClass, jpTypes);
      }
    }

    // The cache relies on this: one join point type per Java class
    for (const [javaClass, jpTypes] of jpTypesPerJavaClass) {
      expect([javaClass, [...jpTypes]]).toEqual([javaClass, [...jpTypes].slice(0, 1)]);
    }
    expect(jpTypesPerJavaClass.size).toBeGreaterThanOrEqual(50);
  });

  it("uses a mapper registered after the cache is warm", () => {
    class MyFunction extends FunctionJp {}
    const fn = Query.search(FunctionJp, "sum").first()!;
    const loop = Query.search(Loop).first()!;
    expect(classOf(wrapJoinPoint(unwrapJoinPoint(fn)))).toBe(FunctionJp);

    registerJoinpointMapper({ function: MyFunction });

    expect(classOf(wrapJoinPoint(unwrapJoinPoint(fn)))).toBe(MyFunction);
    expect(classOf(Query.search(FunctionJp, "sum").first())).toBe(MyFunction);
    // Types the new mapper does not know still fall back to earlier mappers
    expect(classOf(wrapJoinPoint(unwrapJoinPoint(loop)))).toBe(Loop);
  });

  it("uses a function mapper registered after the cache is warm", () => {
    class MyLoop extends Loop {}
    const loop = unwrapJoinPoint(Query.search(Loop).first()!);
    const fn = unwrapJoinPoint(Query.search(FunctionJp, "sum").first()!);
    wrapJoinPoint(loop);

    registerJoinpointMapperFunction((jpType) => (jpType === "loop" ? MyLoop : undefined));

    expect(classOf(wrapJoinPoint(loop))).toBe(MyLoop);
    expect(classOf(wrapJoinPoint(fn))).toBe(FunctionJp);
  });

  it("does not cache a missing mapper error", () => {
    const fn = unwrapJoinPoint(Query.search(FunctionJp, "sum").first()!);
    clearJoinpointMappers();
    registerJoinpointMapper({ program: Program });

    expect(() => wrapJoinPoint(fn)).toThrow("No mapper found for join point type: function");
    expect(() => wrapJoinPoint(fn)).toThrow("No mapper found for join point type: function");

    registerJoinpointMapper({ function: FunctionJp });
    expect(classOf(wrapJoinPoint(fn))).toBe(FunctionJp);
  });

  it("returns Java values that are not join points, lists or enums as-is", () => {
    const file = Query.search(FileJp).first()!;
    const fn = Query.search(FunctionJp, "sum").first()!;

    expect((file.file as JavaNamed).getName()).toBe("dummyFile.cpp");
    expect((fn.getKeyType("id") as JavaNamed).getName()).toBe("java.lang.String");
    expect((fn.getValue("location") as JavaObject).getClass().getName()).toBe(
      "pt.up.fe.specs.clava.SourceRange",
    );
    // Lists and join points in getValue results are still converted
    expect(fn.getValue("templateArguments")).toEqual([]);
    expect(fn.getValue("type") instanceof Joinpoint).toBe(true);
  });
});

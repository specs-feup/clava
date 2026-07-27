import { registerSourceCode } from "@specs-feup/lara/jest/jestHelpers.ts";
import Query from "@specs-feup/lara/api/weaver/Query.ts";
import { Joinpoint, Vardecl } from "./Joinpoints.ts";

const code = `#define VALUE 7
#define CAT_IMPL(left, right) left ## right
#define CAT(left, right) CAT_IMPL(left, right)
#define DECL(name) int name = VALUE;

DECL(CAT(macro_, value))
int ordinary = 0;
int foobar = 1;
int pasted_reference = CAT(foo, bar);

namespace std {
using uint8_t = unsigned char;
template <class T> class vector;
} // namespace std

template <class BinaryType = std::vector<std::uint8_t>>
class Holder {};
`;

describe("source locations", () => {
  registerSourceCode(code);

  it("uses real file coordinates for ordinary and macro-expanded nodes", () => {
    const ordinary = Query.search(Vardecl, "ordinary").first();
    const macro = Query.search(Vardecl, "macro_value").first();

    expect(ordinary).toBeDefined();
    expect(macro).toBeDefined();

    if (ordinary === undefined || macro === undefined) {
      return;
    }

    expect(ordinary.isMacro).toBe(false);
    expect(ordinary.filename).toBe("dummyFile.cpp");
    expect(ordinary.filepath).toMatch(/dummyFile\.cpp$/);
    expect(ordinary.line).toBe(7);
    expect(ordinary.endLine).toBe(7);
    expect(ordinary.location).not.toContain("<scratch space>");

    expect(macro.isMacro).toBe(true);
    expect(macro.filename).toBe("dummyFile.cpp");
    expect(macro.filepath).toMatch(/dummyFile\.cpp$/);
    expect(macro.line).toBe(6);
    expect(macro.endLine).toBe(6);
    expect(macro.column).toBe(1);
    expect(macro.endColumn).toBe(24);
    expect(macro.location).not.toContain("<scratch space>");

    expect(macro.init.isMacro).toBe(true);
    expect(macro.init.filename).toBe("dummyFile.cpp");
    expect(macro.init.line).toBe(6);
    expect(macro.init.location).not.toContain("<scratch space>");

    const pastedReference = Query.search(Vardecl, "pasted_reference").first();
    expect(pastedReference).toBeDefined();
    if (pastedReference === undefined) {
      return;
    }

    expect(pastedReference.init.isMacro).toBe(true);
    expect(pastedReference.init.filename).toBe("dummyFile.cpp");
    expect(pastedReference.init.line).toBe(9);
    expect(pastedReference.init.column).toBe(24);
    expect(pastedReference.init.endColumn).toBe(36);
    expect(pastedReference.init.location).not.toContain("<scratch space>");
  });

  it("does not treat Clang's split closing angle brackets as macros", () => {
    const declarations = Query.search("decl").get() as Joinpoint[];
    const templateParameter = declarations.find(
      (joinpoint) => joinpoint.astName === "TemplateTypeParmDecl"
        && joinpoint.line === 16
    );

    expect(templateParameter).toBeDefined();
    if (templateParameter === undefined) {
      return;
    }

    expect(templateParameter.isMacro).toBe(false);
    expect(templateParameter.filename).toBe("dummyFile.cpp");
    expect(templateParameter.filepath).toMatch(/dummyFile\.cpp$/);
    expect(templateParameter.line).toBe(16);
    expect(templateParameter.endLine).toBe(16);
    expect(templateParameter.column).toBe(11);
    expect(templateParameter.endColumn).toBe(54);
    expect(templateParameter.location).not.toContain("<scratch space>");
  });
});

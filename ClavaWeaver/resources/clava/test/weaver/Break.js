import Query from "@specs-feup/lara/api/weaver/Query.js";

for (const $break of Query.search("function", "foo").search("break")) {
  const enclosingStmt = $break.enclosingStmt;
  println(
    "Break enclosing statement: " +
      enclosingStmt.joinPointType +
      "@" +
      enclosingStmt.line
  );
}

import Query from "@specs-feup/lara/api/weaver/Query.js";

for (const $break of Query.search("function", "foo").search("break")) {
  const enclosingStmt = $break.enclosingStmt;
  console.log(
    "Break enclosing statement: " +
      enclosingStmt.joinPointType +
      "@" +
      enclosingStmt.line
  );
}

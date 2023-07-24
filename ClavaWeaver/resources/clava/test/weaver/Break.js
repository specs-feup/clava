laraImport("weaver.Query");

for (const $break of Query.search("function", "foo").search("break")) {
  const enclosingStmt = $break.enclosingStmt;
  println(
    "Break enclosing statement: " +
      enclosingStmt.joinPointType +
      "@" +
      enclosingStmt.line
  );
}

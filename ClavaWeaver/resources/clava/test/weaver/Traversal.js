laraImport("weaver.Query");
laraImport("weaver.TraversalType");

println("Postorder traversal");
for (const $jp of Query.search("function", "foo").search(
  undefined,
  undefined,
  TraversalType.POSTORDER
)) {
  println($jp);
}

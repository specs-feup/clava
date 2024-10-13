import Query from "@specs-feup/lara/api/weaver/Query.js";
import TraversalType from "@specs-feup/lara/api/weaver/TraversalType.js";

println("Postorder traversal");
for (const $jp of Query.search("function", "foo").search(
  undefined,
  undefined,
  TraversalType.POSTORDER
)) {
  println($jp);
}

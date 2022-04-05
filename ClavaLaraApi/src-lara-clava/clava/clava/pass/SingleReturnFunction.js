laraImport("lara.pass.Pass");
laraImport("weaver.Query");
//laraImport("clava.Clava");

class SingleReturnFunction extends Pass {
  constructor() {
    super("SingleReturnFunction");
  }

  _apply_impl($jp) {
    if (!$jp.instanceOf("function") || !$jp.isImplementation) {
      return;
    }
    const $body = $jp.body;
    const $returnStmts = Array.from(Query.searchFrom($body, "returnStmt"));
    if (
      $returnStmts.length === 0 ||
      ($returnStmts.length === 1 && $body.lastChild.instanceOf("returnStmt"))
    ) {
      return;
    }

    $body.insertEnd("__return_label:");

    const returnType = $jp.returnType;
    const returnIsVoid =
      returnType.isBuiltin && returnType.builtinKind === "Void";
    if (returnIsVoid) {
      $body.insertEnd(";");
    } else {
      $body.addLocal("__return_value", returnType);
      $body.insertEnd("return __return_value;");
    }

    for (const $returnStmt of $returnStmts) {
      if (!returnIsVoid) {
        $returnStmt.insertBefore(
          `__return_value = ${$returnStmt.returnExpr.code};`
        );
      }
      $returnStmt.insertBefore("goto __return_label;");
      $returnStmt.detach();
    }
    //Clava.rebuild();
  }
}

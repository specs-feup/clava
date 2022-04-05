laraImport("lara.pass.Pass");
laraImport("weaver.Query");

class SingleReturnFunction extends Pass {
  constructor() {
    super("SingleReturnFunction");
  }

  _apply_impl($jp) {
    if ($jp.joinpointType !== "function" || !$jp.isImplementation) {
      return;
    }
    const $body = $jp.body;
    const $returnStmts = Array.from(Query.searchFrom($body, "returnStmt"));
    if ($returnStmts.length <= 1) {
      return;
    }

    $body.insertEnd("__returnLabel:");

    const returnType = $jp.returnType;
    const returnIsVoid =
      !returnType.isBuiltin || returnType.builtinKind !== "Void";
    if (returnIsVoid) {
      $body.addLocal("__returnValue", returnType);
      $body.insertEnd("return __returnValue;");
    } else {
      $body.insertEnd(";");
    }

    for (const $returnStmt of $returnStmts) {
      if (returnIsVoid) {
        $returnStmt.insertBefore(
          `__return_value = ${$returnStmt.returnExpr.code};`
        );
      }
      $returnStmt.insertBefore("goto __returnLabel;");
      $returnStmt.detach();
    }
  }
}

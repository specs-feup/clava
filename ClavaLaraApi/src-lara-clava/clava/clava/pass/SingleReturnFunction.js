laraImport("lara.pass.Pass");
laraImport("weaver.Query");
laraImport("clava.pass.DecomposeVarDeclarations");
laraImport("clava.ClavaJoinPoints");

class SingleReturnFunction extends Pass {
  constructor() {
    super("SingleReturnFunction");
  }

  _apply_impl($jp) {
    // destructure joinpoint factories to prevent excessive verbosity
    const { labelDecl, labelStmt, returnStmt, varRef, assign, gotoStmt } =
      ClavaJoinPoints;

    if (!$jp.instanceOf("function") || !$jp.isImplementation) {
      return _new_result($jp, false);
    }
    const $body = $jp.body;
    const $returnStmts = Query.searchFrom($body, "returnStmt").get();
    if (
      $returnStmts.length === 0 ||
      ($returnStmts.length === 1 && $body.lastChild.instanceOf("returnStmt"))
    ) {
      return this.#new_result($jp, false);
    }

    // C++ spec has some restrictions about jumping over initialized values that
    // would be invalidated by the generated code, so we need to decompose variable
    // declarations first
    new DecomposeVarDeclarations().apply($body);

    const $label = labelDecl("__return_label");
    $body.insertEnd(labelStmt($label));

    const returnType = $jp.returnType;
    const returnIsVoid =
      returnType.isBuiltin && returnType.builtinKind === "Void";
    let $local;
    if (returnIsVoid) {
      $body.insertEnd(returnStmt());
    } else {
      $local = $body.addLocal("__return_value", returnType);
      $body.insertEnd(returnStmt(varRef($local)));
    }

    for (const $returnStmt of $returnStmts) {
      if (!returnIsVoid) {
        $returnStmt.insertBefore(
          // null safety: $local is initialized whenever return is not void
          assign(varRef($local), $returnStmt.returnExpr)
        );
      }
      $returnStmt.insertBefore(gotoStmt($label));
      $returnStmt.detach();
    }

    return this.#new_result($jp, true);
  }

  #new_result($jp, appliedPass) {
    return new PassResult(this.name, {
      appliedPass,
      insertedLiteralCode: false,
      location: $jp.location,
    });
  }
}

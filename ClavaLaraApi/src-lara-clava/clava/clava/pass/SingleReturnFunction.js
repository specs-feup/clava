laraImport("lara.pass.Pass");
laraImport("weaver.Query");
laraImport("clava.pass.DecomposeVarDeclarations");

class SingleReturnFunction extends Pass {
  constructor() {
    super("SingleReturnFunction");
  }

  _apply_impl($jp) {
    if (!$jp.instanceOf("function") || !$jp.isImplementation) {
      return _new_result($jp, false);
    }
    const $body = $jp.body;
    const $returnStmts = Query.searchFrom($body, "returnStmt").get();
    if (
      $returnStmts.length === 0 ||
      ($returnStmts.length === 1 && $body.lastChild.instanceOf("returnStmt"))
    ) {
      return _new_result($jp, false);
    }

    // C++ spec has some restrictions about jumping over initialized values that
    // would be invalidated by the generated code, so we need to decompose variable
    // declarations first
    new DecomposeVarDeclarations().apply($body);

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
    
    return _new_result($jp, true);
  }
  
  _new_result($jp, appliedPass) {
		var result = new PassResult(this.name);
		result.isUndefined = false;
		result.appliedPass = appliedPass;
		result.insertedLiteralCode = true;
		result.location = $jp.location;
		return result;
	}  
}

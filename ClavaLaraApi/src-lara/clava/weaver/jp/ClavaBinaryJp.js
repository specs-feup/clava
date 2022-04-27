laraImport("weaver.jp.BinaryJp");
laraImport("weaver.Weaver");

Object.defineProperty(BinaryJp.prototype, "kind", {
  get: function () {
    return this.astNode.getOperatorCode();
  },
});

Object.defineProperty(BinaryJp.prototype, "isInnerExpr", {
  get: function () {
    var parent = this.parent;

    while (parent !== null && parent.instanceOf("expr")) {
      if (parent.astNode.getClass().getSimpleName() !== "ParenExpr")
        return true;

      parent = parent.parent;
    }

    return false;
  },
});

Object.defineProperty(BinaryJp.prototype, "outerExpr", {
  get: function () {
    var parent = this.parent;

    while (parent !== null && parent.instanceOf("expr")) {
      if (parent.astNode.getClass().getSimpleName() !== "ParenExpr")
        return parent;

      parent = parent.parent;
    }

    return null;
  },
});

Object.defineProperty(BinaryJp.prototype, "isLogicOp", {
  get: function () {
    var kind = this.kind;
    return kind === "&&" || kind === "||";
  },
});

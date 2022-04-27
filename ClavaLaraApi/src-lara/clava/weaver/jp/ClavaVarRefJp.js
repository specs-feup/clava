laraImport("weaver.jp.VarRefJp");
laraImport("weaver.Weaver");

Object.defineProperty(VarRefJp.prototype, "name", {
  get: function () {
    return Weaver.AST_METHODS.toJavaJoinPoint(this.astNode).name;
  },
});

Object.defineProperty(VarRefJp.prototype, "decl", {
  get: function () {
    return CommonJoinPoints.toJoinPoint(this.astNode.getDeclaration());
  },
});

laraImport("weaver.jp.VarDeclJp");
laraImport("weaver.Weaver");

Object.defineProperty(VarDeclJp.prototype, "name", {
  get: function () {
    return Weaver.AST_METHODS.toJavaJoinPoint(this.astNode).name;
  },
});

Object.defineProperty(VarDeclJp.prototype, "type", {
  get: function () {
    return CommonJoinPoints.toJoinPoint(this.astNode.getType());
  },
});

Object.defineProperty(VarDeclJp.prototype, "isParam", {
  get: function () {
    return false;
  },
});

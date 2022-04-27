laraImport("weaver.jp.CallJp");
laraImport("weaver.Weaver");

Object.defineProperty(CallJp.prototype, "name", {
  get: function () {
    return Weaver.AST_METHODS.toJavaJoinPoint(this.astNode).name;
  },
});

Object.defineProperty(CallJp.prototype, "function", {
  get: function () {
    const functionDecl = this.astNode.getFunctionDecl();
    if (functionDecl.isPresent())
      return CommonJoinPoints.toJoinPoint(functionDecl.get());
    else return null;
  },
});

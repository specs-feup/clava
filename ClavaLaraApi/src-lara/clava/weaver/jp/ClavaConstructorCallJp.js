laraImport("weaver.jp.ConstructorCallJp");
laraImport("weaver.Weaver");

Object.defineProperty(ConstructorCallJp.prototype, "function", {
  get: function () {
    const functionDecl = this.astNode.getDecl();
    if (functionDecl.isPresent())
      return CommonJoinPoints.toJoinPoint(functionDecl.get());
    else return null;
  },
});

Object.defineProperty(ConstructorCallJp.prototype, "constructor", {
  get: function () {
    return this.method;
  },
});

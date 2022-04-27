laraImport("weaver.jp.ParamJp");
laraImport("weaver.Weaver");

Object.defineProperty(ParamJp.prototype, "function", {
  get: function () {
    return CommonJoinPoints.toJoinPoint(this.astNode.getFunctionDecl());
  },
});

Object.defineProperty(ParamJp.prototype, "isParam", {
  get: function () {
    return true;
  },
});

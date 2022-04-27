laraImport("weaver.jp.IfJp");
laraImport("weaver.Weaver");

Object.defineProperty(IfJp.prototype, "condition", {
  get: function () {
    return CommonJoinPoints.toJoinPoint(this.astNode.getCondition());
  },
});

Object.defineProperty(IfJp.prototype, "then", {
  get: function () {
    return CommonJoinPoints.toJoinPoint(this.astNode.getThen().get());
  },
});

Object.defineProperty(IfJp.prototype, "else", {
  get: function () {
    if (this.hasElse)
      return CommonJoinPoints.toJoinPoint(this.astNode.getElse().get());
    return null;
  },
});

Object.defineProperty(IfJp.prototype, "hasElse", {
  get: function () {
    return this.astNode.getElse().isPresent();
  },
});

Object.defineProperty(IfJp.prototype, "isElseIf", {
  get: function () {
    return this.parent.instanceOf("else") && this.parent.isElseIf;
  },
});

laraImport("weaver.jp.LoopJp");
laraImport("weaver.Weaver");

Object.defineProperty(LoopJp.prototype, "condition", {
  get: function () {
    const conditionExpr = this.astNode.getStmtCondition();
    if (conditionExpr.isPresent())
      return CommonJoinPoints.toJoinPoint(conditionExpr.get());
    else return null;
  },
});

Object.defineProperty(LoopJp.prototype, "hasCondition", {
  get: function () {
    return this.astNode.getStmtCondition().isPresent();
  },
});

laraImport("weaver.jp.JoinPoint");
laraImport("weaver.Weaver");

Object.defineProperty(JoinPoint.prototype, "line", {
  get: function () {
    return Weaver.AST_METHODS.toJavaJoinPoint(this.astNode).line;
  },
});

Object.defineProperty(JoinPoint.prototype, "endLine", {
  get: function () {
    return Weaver.AST_METHODS.toJavaJoinPoint(this.astNode).endLine;
  },
});

Object.defineProperty(JoinPoint.prototype, "code", {
  get: function () {
    try {
      return Weaver.toJs(this.astNode.getCode());
    } catch (err) {
      return null;
    }
  },
});

Object.defineProperty(JoinPoint.prototype, "astId", {
  get: function () {
    return this.astNode.getValue("id");
  },
});

JoinPoint.prototype.equals = function (joinPoint) {
  if (joinPoint === undefined) {
    return false;
  }

  const javaJoinPoint = Weaver.AST_METHODS.toJavaJoinPoint(joinPoint.astNode);
  return Weaver.AST_METHODS.toJavaJoinPoint(this.astNode).compareNodes(
    javaJoinPoint
  );
};

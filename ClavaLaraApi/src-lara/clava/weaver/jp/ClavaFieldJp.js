laraImport("weaver.jp.FieldJp");
laraImport("weaver.Weaver");

Object.defineProperty(FieldJp.prototype, "id", {
  get: function () {
    return this.name;
  },
});

Object.defineProperty(FieldJp.prototype, "name", {
  get: function () {
    return Weaver.AST_METHODS.toJavaJoinPoint(this.astNode).name;
  },
});

Object.defineProperty(FieldJp.prototype, "class", {
  get: function () {
    return CommonJoinPoints.toJoinPoint(this.astNode.getParent());
  },
});

Object.defineProperty(FieldJp.prototype, "type", {
  get: function () {
    return CommonJoinPoints.toJoinPoint(this.astNode.getType());
  },
});

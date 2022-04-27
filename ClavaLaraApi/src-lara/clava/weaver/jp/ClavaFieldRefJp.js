laraImport("weaver.jp.FieldRefJp");
laraImport("weaver.Weaver");

Object.defineProperty(FieldRefJp.prototype, "name", {
  get: function () {
    return Weaver.AST_METHODS.toJavaJoinPoint(this.astNode).name;
  },
});

Object.defineProperty(FieldRefJp.prototype, "class", {
  get: function () {
    return this.field.class;
  },
});

Object.defineProperty(FieldRefJp.prototype, "field", {
  get: function () {
    return CommonJoinPoints.toJoinPoint(this.astNode.getMemberDecl());
  },
});

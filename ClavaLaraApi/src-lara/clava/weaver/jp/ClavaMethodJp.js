laraImport("weaver.jp.MethodJp");
laraImport("weaver.Weaver");

Object.defineProperty(MethodJp.prototype, "class", {
  get: function () {
    const classDecl = this.astNode.getRecordDecl();
    if (classDecl.isPresent())
      return CommonJoinPoints.toJoinPoint(classDecl.get());
    else return null;
  },
});

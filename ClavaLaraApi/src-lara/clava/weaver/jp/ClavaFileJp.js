laraImport("weaver.jp.FileJp");
laraImport("weaver.Weaver");

Object.defineProperty(FileJp.prototype, "name", {
  get: function () {
    return Weaver.AST_METHODS.toJavaJoinPoint(this.astNode).name;
  },
});

Object.defineProperty(FileJp.prototype, "path", {
  get: function () {
    return this.astNode.getRelativeFilepath();
  },
});

Object.defineProperty(FileJp.prototype, "id", {
  get: function () {
    return this.name;
  },
});

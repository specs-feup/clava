laraImport("weaver.jp.ElseJp");
laraImport("weaver.Weaver");

Object.defineProperty(ElseJp.prototype, "isElseIf", {
  get: function () {
    const filteredChildren = this.children.filter(
      (stmt) => !stmt.astNode.isWrapper()
    );
    return (
      filteredChildren.length === 1 && filteredChildren[0].instanceOf("if")
    );
  },
});

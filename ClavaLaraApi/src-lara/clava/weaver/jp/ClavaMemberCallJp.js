laraImport("weaver.jp.MemberCallJp");
laraImport("weaver.Weaver");

// TODO: This *probably* should be changed to use CXXMemberCallExpr.getBase()
// After implementing ThisJp, PointerJp, and other possible memberCall bases
Object.defineProperty(MemberCallJp.prototype, "class", {
  get: function () {
    if (this.method === null) return null;
    else return this.method.class;
  },
});

Object.defineProperty(MemberCallJp.prototype, "method", {
  get: function () {
    return this.function;
  },
});

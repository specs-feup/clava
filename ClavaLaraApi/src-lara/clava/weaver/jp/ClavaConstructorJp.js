laraImport("weaver.jp.ConstructorJp");
laraImport("weaver.Weaver");

Object.defineProperty(ConstructorJp.prototype, "superCalls", {
  get: function () {
    const initializers = Weaver.toJs(this.astNode.getInitializers())
      .filter(
        // Filter Constructor calls
        (init) => init.getValue("initKind").getString() === "BaseInitializer"
      )
      .map(function (init) {
        // Get Constructor calls
        const initExpr = init.getValue("initExpr");
        if (initExpr.getClass().getSimpleName() === "CXXConstructExpr")
          return initExpr;
        else return initExpr.getSubExpr();
      });

    return CommonJoinPoints.toJoinPoints(initializers);
  },
});

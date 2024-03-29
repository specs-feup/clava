laraImport("weaver.jp.FunctionJp");
laraImport("weaver.Weaver");

// Override FunctionJp constructor
var oldFunctionJp = FunctionJp.prototype;
var oldFunction_JP_TYPES = FunctionJp._JP_TYPES;

FunctionJp = function (astNode) {
  // Parent constructor
  DeclJp.call(this, astNode);
};

FunctionJp.prototype = oldFunctionJp;
FunctionJp._JP_TYPES = oldFunction_JP_TYPES;

_lara_dummy_ = Object.defineProperty(FunctionJp.prototype, "id", {
  get: function () {
    return this.signature;
  },
});

_lara_dummy_ = Object.defineProperty(FunctionJp.prototype, "name", {
  get: function () {
    return Weaver.AST_METHODS.toJavaJoinPoint(this.astNode).name;
  },
});

_lara_dummy_ = Object.defineProperty(FunctionJp.prototype, "signature", {
  get: function () {
    return Weaver.AST_METHODS.toJavaJoinPoint(this.astNode).signature;
  },
});

_lara_dummy_ = Object.defineProperty(FunctionJp.prototype, "returnType", {
  get: function () {
    return CommonJoinPoints.toJoinPoint(this.astNode.getReturnType());
  },
});

_lara_dummy_ = Object.defineProperty(FunctionJp.prototype, "stmts", {
  get: function () {
    var functionBody = this.astNode.getBody();
    if (!functionBody.isPresent()) return [];

    var functionStmts = functionBody.get().getStatements();
    return CommonJoinPoints.toJoinPoints(Weaver.toJs(functionStmts));
  },
});

_lara_dummy_ = Object.defineProperty(FunctionJp.prototype, "hasBody", {
  get: function () {
    return this.astNode.getBody().isPresent();
  },
});

_lara_dummy_ = Object.defineProperty(FunctionJp.prototype, "children", {
  get: function () {
    return new FunctionJp(this.astNode, false)._children;
  },
});

_lara_dummy_ = Object.defineProperty(MethodJp.prototype, "isCustom", {
  get: function () {
    //return true;
    //return this.class.isCustom && this.hasBody;
    return this.class.isCustom;
  },
});

// To avoid warning
ClavaFunctionJp = FunctionJp;

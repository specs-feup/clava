laraImport("weaver.jp.FunctionJp");
laraImport("weaver.Weaver");

// Override FunctionJp constructor
var oldFunctionJp = FunctionJp.prototype;
var oldFunction_JP_TYPES = FunctionJp._JP_TYPES;

FunctionJp = function (astNode, getDefinition = true) {
  const actualGetDefinition = getDefinition ?? true;

  // check for definition
  this.originalAstNode = astNode;
  if (astNode.getDefinition().isPresent() && actualGetDefinition)
    astNode = astNode.getDefinition().get();

  // Parent constructor
  DeclJp.call(this, astNode);
};

FunctionJp.prototype = oldFunctionJp;
FunctionJp._JP_TYPES = oldFunction_JP_TYPES;

Object.defineProperty(FunctionJp.prototype, "id", {
  get: function () {
    return this.signature;
  },
});

Object.defineProperty(FunctionJp.prototype, "name", {
  get: function () {
    return Weaver.AST_METHODS.toJavaJoinPoint(this.astNode).name;
  },
});

Object.defineProperty(FunctionJp.prototype, "signature", {
  get: function () {
    return Weaver.AST_METHODS.toJavaJoinPoint(this.astNode).signature;
  },
});

Object.defineProperty(FunctionJp.prototype, "returnType", {
  get: function () {
    return CommonJoinPoints.toJoinPoint(this.astNode.getReturnType());
  },
});

Object.defineProperty(FunctionJp.prototype, "stmts", {
  get: function () {
    const functionBody = this.astNode.getBody();
    if (!functionBody.isPresent()) return [];

    const functionStmts = functionBody.get().getStatements();
    return CommonJoinPoints.toJoinPoints(Weaver.toJs(functionStmts));
  },
});

Object.defineProperty(FunctionJp.prototype, "hasBody", {
  get: function () {
    return this.originalAstNode.getBody().isPresent();
  },
});

Object.defineProperty(FunctionJp.prototype, "children", {
  get: function () {
    return new FunctionJp(this.originalAstNode, false)._children;
  },
});

Object.defineProperty(MethodJp.prototype, "isCustom", {
  get: function () {
    //return true;
    //return this.class.isCustom && this.hasBody;
    return this.class.isCustom;
  },
});

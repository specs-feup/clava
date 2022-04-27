laraImport("weaver.jp.ClassJp");
laraImport("weaver.Weaver");

// Override ClassJp constructor
const oldClassJp = ClassJp.prototype;
const oldClass_JP_TYPES = ClassJp._JP_TYPES;

ClassJp = function (astNode, getDefinition = true) {
  const actualGetDefinition = getDefinition ?? true;

  // check for definition
  this.originalAstNode = astNode;
  if (astNode.getDefinition().isPresent() && actualGetDefinition)
    astNode = astNode.getDefinition().get();

  // Parent constructor
  DeclJp.call(this, astNode);
};

ClassJp.prototype = oldClassJp;
ClassJp._JP_TYPES = oldClass_JP_TYPES;

Object.defineProperty(ClassJp.prototype, "id", {
  get: function () {
    return this._qualifiedName;
  },
});

Object.defineProperty(ClassJp.prototype, "_qualifiedName", {
  get: function () {
    return this.astNode.getFullyQualifiedName();
  },
});

Object.defineProperty(ClassJp.prototype, "name", {
  get: function () {
    return Weaver.AST_METHODS.toJavaJoinPoint(this.astNode).name;
  },
});

Object.defineProperty(ClassJp.prototype, "superClasses", {
  get: function () {
    return CommonJoinPoints.toJoinPoints(
      Weaver.toJs(this.astNode.getBases())
    ).filter(($jp) => $jp.instanceOf("class"));
  },
});

Object.defineProperty(ClassJp.prototype, "allSuperClasses", {
  get: function () {
    return this.superClasses.flatMap((superClass) => [
      superClass,
      ...superClass.allSuperClasses,
    ]);
  },
});

Object.defineProperty(ClassJp.prototype, "_allMethods", {
  get: function () {
    const astMethods = Weaver.toJs(this.astNode.getMethods());
    const methods = astMethods.map((method) =>
      method.getDefinition().isPresent() ? method.getDefinition().get() : method
    );
    return CommonJoinPoints.toJoinPoints(Weaver.toJs(methods));
  },
});

Object.defineProperty(ClassJp.prototype, "allMethods", {
  get: function () {
    //return this._allMethods.filter(method => method.hasBody);
    return this._allMethods;
  },
});

Object.defineProperty(ClassJp.prototype, "methods", {
  get: function () {
    return this.allMethods.filter(
      (method) => method.joinPointType === "method"
    );
  },
});

Object.defineProperty(ClassJp.prototype, "constructors", {
  get: function () {
    return this.allMethods.filter(
      (method) => method.joinPointType === "constructor"
    );
  },
});

Object.defineProperty(ClassJp.prototype, "fields", {
  get: function () {
    return CommonJoinPoints.toJoinPoints(Weaver.toJs(this.astNode.getFields()));
  },
});

Object.defineProperty(ClassJp.prototype, "_kind", {
  get: function () {
    return "class";
  },
});

Object.defineProperty(ClassJp.prototype, "isCustom", {
  get: function () {
    return this.ancestor("file") != null;
  },
});

Object.defineProperty(ClassJp.prototype, "children", {
  get: function () {
    return new ClassJp(this.originalAstNode, false)._children;
  },
});

Object.defineProperty(ClassJp.prototype, "members", {
  get: function () {
    return this.fields.concat(this.allMethods);
  },
});

laraImport("weaver.jp.TypeJp");
laraImport("weaver.Weaver");

Object.defineProperty(TypeJp.prototype, "kind", {
  get: function () {
    return Weaver.AST_METHODS.toJavaJoinPoint(this.astNode).kind;
  },
});

Object.defineProperty(TypeJp.prototype, "isArray", {
  get: function () {
    return Weaver.AST_METHODS.toJavaJoinPoint(this.astNode).isArray;
  },
});

Object.defineProperty(TypeJp.prototype, "isPointer", {
  get: function () {
    return Weaver.AST_METHODS.toJavaJoinPoint(this.astNode).isPointer;
  },
});

Object.defineProperty(TypeJp.prototype, "isPrimitive", {
  get: function () {
    return Weaver.AST_METHODS.toJavaJoinPoint(this.astNode).isBuiltin;
  },
});

Object.defineProperty(TypeJp.prototype, "isClass", {
  get: function () {
    if (this.kind !== "RecordType") return false;
    return this.astNode.getTagKind().toString() === "CLASS";
  },
});

Object.defineProperty(TypeJp.prototype, "_hasSugar", {
  get: function () {
    return this.astNode.hasSugar();
  },
});

Object.defineProperty(TypeJp.prototype, "_desugar", {
  get: function () {
    return CommonJoinPoints.toJoinPoint(this.astNode.desugar());
  },
});

Object.defineProperty(TypeJp.prototype, "_unwrap", {
  get: function () {
    return CommonJoinPoints.toJoinPoint(this.astNode.getElementType());
  },
});

Object.defineProperty(TypeJp.prototype, "_desugarAll", {
  get: function () {
    return CommonJoinPoints.toJoinPoint(this.astNode.desugarAll());
  },
});

Object.defineProperty(TypeJp.prototype, "decl", {
  get: function () {
    return CommonJoinPoints.toJoinPoint(this.astNode.getDecl());
  },
});

Object.defineProperty(TypeJp.prototype, "_hasTemplateArgs", {
  get: function () {
    return this.astNode.hasTemplateArgs();
  },
});

Object.defineProperty(TypeJp.prototype, "_templateArgsTypes", {
  get: function () {
    return CommonJoinPoints.toJoinPoints(
      Weaver.toJs(this.astNode.getTemplateArgumentTypes())
    );
  },
});

Object.defineProperty(TypeJp.prototype, "_typeDescendants", {
  get: function () {
    return CommonJoinPoints.toJoinPoints(
      Weaver.toJs(this.astNode.getTypeDescendants())
    );
  },
});

Object.defineProperty(TypeJp.prototype, "_typeDescendantsAndSelf", {
  get: function () {
    return [this].concat(this._typeDescendants);
  },
});

// TODO: It currently returns duplicates
Object.defineProperty(TypeJp.prototype, "usedTypes", {
  get: function () {
    const usedTypes = [];

    for (type of this._typeDescendantsAndSelf) {
      usedTypes.push(type);

      if (type._hasTemplateArgs)
        for (templateType of type._templateArgsTypes)
          usedTypes = usedTypes.concat(templateType.usedTypes);
    }

    return usedTypes;
  },
});

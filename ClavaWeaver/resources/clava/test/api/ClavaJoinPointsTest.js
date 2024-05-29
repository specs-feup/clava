laraImport("clava.ClavaJoinPoints");
laraImport("weaver.Query");

var intType = ClavaJoinPoints.builtinType("int");
var doubleType = ClavaJoinPoints.builtinType("double");
var intExpr = ClavaJoinPoints.exprLiteral("a", intType);
var intPointerType = ClavaJoinPoints.pointer(intType);
var intPointerExpr = ClavaJoinPoints.exprLiteral("aPointer", intPointerType);

// Type Literal of user defined type
println("User literal type: " + ClavaJoinPoints.typeLiteral("xpto").code);

// Stmt Literal
println("Literal statement: " + ClavaJoinPoints.stmtLiteral("int a = 0;").code);

// TypedefDecl
var typedefDecl = ClavaJoinPoints.typedefDecl(
  ClavaJoinPoints.builtinType("int"),
  "custom_int"
);
println("Typedef decl: " + typedefDecl.code);

// TypedefType
println("Typedef type: " + ClavaJoinPoints.typedefType(typedefDecl).code);

// Cast
println(
  "C-Style cast: " + ClavaJoinPoints.cStyleCast(doubleType, intExpr).code
);

// If Stmt
println("Empty if:\n" + ClavaJoinPoints.ifStmt("a == 0").code);
println(
  "If with then:\n" +
    ClavaJoinPoints.ifStmt("a == 0", ClavaJoinPoints.stmtLiteral("a = 1;")).code
);
println(
  "If with else:\n" +
    ClavaJoinPoints.ifStmt(
      "a == 0",
      undefined,
      ClavaJoinPoints.stmtLiteral("a = 2;")
    ).code
);

// For Stmt
println("Empty for:\n" + ClavaJoinPoints.forStmt().code);
println(
  "Complete for:\n" +
    ClavaJoinPoints.forStmt("int i=0;", "i<10;", "i++;", "i = i+1;\ni = i - 1;")
      .code
);

// Unary Operator
var addressOfOp = ClavaJoinPoints.unaryOp("&", intExpr);
println("AddressOf code: " + addressOfOp.code);
println("AddressOf code type: " + addressOfOp.type.code);

var derefOp = ClavaJoinPoints.unaryOp("*", intPointerExpr);
println("Deref code: " + derefOp.code);
println("Deref code type: " + derefOp.type.code);

const xConstPointerDecl = Query.search("function", "constPointer")
  .search("vardecl", "x")
  .first();
const xConstPointerRef = ClavaJoinPoints.varRef(xConstPointerDecl);

// Deref of qualified pointer
const derefQualified = ClavaJoinPoints.unaryOp("*", xConstPointerRef);
println("Deref code: " + derefQualified.code);
println("Deref code type: " + derefQualified.type.code);

var type;
type = ClavaJoinPoints.type("int");
println("Builtin type: " + type.joinPointType);
type = ClavaJoinPoints.type("uint64_t");
println("Literal type: " + type.node.getClass());
var type2 = ClavaJoinPoints.type(type);
println("Same type: " + (type === type2)); // They should be the same join point
type2 = ClavaJoinPoints.type(ClavaJoinPoints.varDeclNoInit("dummyVar", type));
println("Same type again: " + (type.code === type2.code)); // No longer the same join point, but the same node
// Invalid input, only strings or jps
try {
  ClavaJoinPoints.type([0, 1]);
  println("fail 1");
} catch (e) {
  println("Rejected invalid input type");
}
// Invalid input, jp must have type
try {
  ClavaJoinPoints.type(ClavaJoinPoints.file("dummyFile"));
  println("fail 2");
} catch (e) {
  println("Rejected jp without type");
}

// Varref
var $varref = ClavaJoinPoints.varRef("varref_test", type);
println("Varref code: " + $varref.code);
println("Varref type: " + $varref.type.code);
var $varref2 = ClavaJoinPoints.varRef(
  ClavaJoinPoints.varDeclNoInit("varref_test_2", type)
);
println("Varref2 code: " + $varref2.code);
println("Varref2 type: " + $varref2.type.code);

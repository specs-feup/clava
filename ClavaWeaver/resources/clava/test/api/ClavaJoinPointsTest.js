import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

var intType = ClavaJoinPoints.builtinType("int");
var doubleType = ClavaJoinPoints.builtinType("double");
var intExpr = ClavaJoinPoints.exprLiteral("a", intType);
var intPointerType = ClavaJoinPoints.pointer(intType);
var intPointerExpr = ClavaJoinPoints.exprLiteral("aPointer", intPointerType);

// Type Literal of user defined type
console.log("User literal type: " + ClavaJoinPoints.typeLiteral("xpto").code);

// Stmt Literal
console.log("Literal statement: " + ClavaJoinPoints.stmtLiteral("int a = 0;").code);

// TypedefDecl
var typedefDecl = ClavaJoinPoints.typedefDecl(
  ClavaJoinPoints.builtinType("int"),
  "custom_int"
);
console.log("Typedef decl: " + typedefDecl.code);

// TypedefType
console.log("Typedef type: " + ClavaJoinPoints.typedefType(typedefDecl).code);

// Cast
console.log(
  "C-Style cast: " + ClavaJoinPoints.cStyleCast(doubleType, intExpr).code
);

// If Stmt
console.log("Empty if:\n" + ClavaJoinPoints.ifStmt("a == 0").code);
console.log(
  "If with then:\n" +
    ClavaJoinPoints.ifStmt("a == 0", ClavaJoinPoints.stmtLiteral("a = 1;")).code
);
console.log(
  "If with else:\n" +
    ClavaJoinPoints.ifStmt(
      "a == 0",
      undefined,
      ClavaJoinPoints.stmtLiteral("a = 2;")
    ).code
);

// For Stmt
console.log("Empty for:\n" + ClavaJoinPoints.forStmt().code);
console.log(
  "Complete for:\n" +
    ClavaJoinPoints.forStmt("int i=0;", "i<10;", "i++;", "i = i+1;\ni = i - 1;")
      .code
);

// Unary Operator
var addressOfOp = ClavaJoinPoints.unaryOp("&", intExpr);
console.log("AddressOf code: " + addressOfOp.code);
console.log("AddressOf code type: " + addressOfOp.type.code);

var derefOp = ClavaJoinPoints.unaryOp("*", intPointerExpr);
console.log("Deref code: " + derefOp.code);
console.log("Deref code type: " + derefOp.type.code);

const xConstPointerDecl = Query.search("function", "constPointer")
  .search("vardecl", "x")
  .first();
const xConstPointerRef = ClavaJoinPoints.varRef(xConstPointerDecl);

// Deref of qualified pointer
const derefQualified = ClavaJoinPoints.unaryOp("*", xConstPointerRef);
console.log("Deref code: " + derefQualified.code);
console.log("Deref code type: " + derefQualified.type.code);

var type;
type = ClavaJoinPoints.type("int");
console.log("Builtin type: " + type.joinPointType);
type = ClavaJoinPoints.type("uint64_t");
console.log("Literal type: " + type.node.getClass());
var type2 = ClavaJoinPoints.type(type);
console.log("Same type: " + (type === type2)); // They should be the same join point
type2 = ClavaJoinPoints.type(ClavaJoinPoints.varDeclNoInit("dummyVar", type));
console.log("Same type again: " + (type.code === type2.code)); // No longer the same join point, but the same node
// Invalid input, only strings or jps
try {
  ClavaJoinPoints.type([0, 1]);
  console.log("fail 1");
} catch (e) {
  console.log("Rejected invalid input type");
}
// Invalid input, jp must have type
try {
  ClavaJoinPoints.type(ClavaJoinPoints.file("dummyFile"));
  console.log("fail 2");
} catch (e) {
  console.log("Rejected jp without type");
}

// Varref
var $varref = ClavaJoinPoints.varRef("varref_test", type);
console.log("Varref code: " + $varref.code);
console.log("Varref type: " + $varref.type.code);
var $varref2 = ClavaJoinPoints.varRef(
  ClavaJoinPoints.varDeclNoInit("varref_test_2", type)
);
console.log("Varref2 code: " + $varref2.code);
console.log("Varref2 type: " + $varref2.type.code);

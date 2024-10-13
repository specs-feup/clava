import Query from "@specs-feup/lara/api/weaver/Query.js";
import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";
import ClavaType from "@specs-feup/clava/api/clava/ClavaType.js";

const $jp = Query.search("call").first();
const $var1 = ClavaJoinPoints.varDeclNoInit(
    "boolVar",
    ClavaJoinPoints.builtinType("bool")
);
const $var2 = ClavaJoinPoints.varDeclNoInit(
    "boolVar2",
    ClavaJoinPoints.builtinType("bool")
);
$jp.insertAfter($var2);
$jp.insertAfter($var1);
const $ifStmt = ClavaJoinPoints.ifStmt(
    ClavaJoinPoints.varRefFromDecl($var1),
    ClavaJoinPoints.stmtLiteral("int a = 1;")
);
$var2.insertAfter($ifStmt);

//ClavaType.asStatement(
const op = ClavaJoinPoints.binaryOp(
    "=",
    ClavaJoinPoints.varRefFromDecl($var1),
    ClavaJoinPoints.varRefFromDecl($var2),
    $var1.type
);
//)
console.log("OP: " + op);
$ifStmt.then.insertEnd(op);

import weaver.Query;
import clava.ClavaJoinPoints;
import clava.ClavaType;
aspectdef LSIssue2

	var $jp = Query.search("call").first();
	var $var1 = ClavaJoinPoints.varDeclNoInit(
	    "boolVar",
	    ClavaJoinPoints.builtinType("bool")
	);
	var $var2 = ClavaJoinPoints.varDeclNoInit(
	    "boolVar2",
	    ClavaJoinPoints.builtinType("bool")
	);
	$jp.insertAfter($var2);
	$jp.insertAfter($var1);
	var $ifStmt = ClavaJoinPoints.ifStmt(
	    ClavaJoinPoints.varRefFromDecl($var1),
	    ClavaJoinPoints.stmtLiteral("int a = 1;")
	);
	$var2.insertAfter($ifStmt);

//ClavaType.asStatement(
   var op =     ClavaJoinPoints.binaryOp(
            "=",
            ClavaJoinPoints.varRefFromDecl($var1),
            ClavaJoinPoints.varRefFromDecl($var2),
            $var1.type
        );
        //)
println("OP: " + op);
	$ifStmt.then.insertEnd(
        op

    );
            
end
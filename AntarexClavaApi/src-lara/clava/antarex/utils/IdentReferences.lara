/*
   Author(s): Loïc Besnard (loic.besnard@irisa.fr). 
              IRISA-INRIA Rennes, France.
              
   This code has been developed during the ANTAREX project (http://www.antarex-project.eu).
   It is provided under the MIT license (https://opensource.org/licenses/MIT).
*/

/**
  @return the set of the declarations of the variables referenced in a statement ($statement) 
  but not declared in it (ie external references to the statement).
*/
function getExternalDeclsOf($statement, $FunctionDecls) {
  var $varDecls = [];
  var $externDecls = [];
  var $stmt = getStmtToVisitForExternalDecls($statement);
  getRefVarsInStmt($stmt, $varDecls, $FunctionDecls);
  for (var $f of $varDecls)  {
	 if (isOutsideDeclaration($f, $statement)) {
	   insertInUsingLocation($externDecls, $f); // unicity.
	   TRACE(" isOutsideDeclaration location  = " + $f.code + ', '  + $f.location);
    }
  }
 return $externDecls; 
}


/**
  @return the set of the parameters referenced in a statement ($statement). 
  Assumed: $statement is a statement of a function/method. 
*/
function getReferencedParameters($statement) {
  var $varDecls = [];
  var $parameters = [];
  var $FunctionDecls = [ ];
  var $stmt = getStmtToVisitForExternalDecls($statement);
  getRefVarsInStmt($stmt, $varDecls, $FunctionDecls);
  for (var $f of $varDecls)  {
	if ( isParameterDeclaration($f))
		insertInUsingLocation($parameters, $f);
    }
 return  $parameters;
}


/**
   @return true if a declaration ($aDecl) is not local to a statement ($statement), false otherwise.
*/
function isOutsideDeclaration($aDecl, $statement) {
  if ( isParameterDeclaration($aDecl)) return false;
  var $father = $aDecl.parent;
  TRACE(" +++  $aDecl = " + $aDecl.code + " astName = " + $aDecl.astName);
  var idStatement = $statement.astId;
  do {
      TRACE("    ----  parent = " + $father.astId + " $statementId= " + $statement.astId );
      if ($father.astId === idStatement ) return false;
      if ($father.astName === 'FunctionDecl') return true;
      if ($father.astName === 'TranslationUnit') return true;
      $father = $father.parent;
     } while (true);
  return false;
}

/**
   @return the statement to visit for analyzing the declarations.
*/
function getStmtToVisitForExternalDecls($statement)
{ 
 var $astname = $statement.astName;
 switch ($astname) { 
     case 'NullDecl':
     case 'NullStmt':
     case 'ExprStmt':
     case 'ReturnStmt':
     case 'VarDecl':
	 case 'DeclStmt':
	 case 'IfStmt':
     case 'CompoundStmt':   // {  } 
     case 'WrapperStmt': // comment.
        return $statement;
        
     case 'ForStmt':
     case 'WhileStmt':
     case 'CXXForRangeStmt':  //  for(auto edgeId : Pred[w]) {  }
	  return $statement.getAstChild(3);

     case 'CXXOperatorCallExpr':
     	NYI("getStmtToVisitForExternalDecls,  CXXOperatorCallExpr detected, code = " +  $statement.code);
 	 break;
    		
     default:
	  NYI(" getStmtToVisitForExternalDecls, astname = " + $astname);
     }
   return $statement;
 }


/**     
   Return  (in $refVars) the set of symbols (variables, constants, objects) referenced in a statement ($statement).
  */
function getRefVarsInStmt($statement, $refVars, $FunctionRefs) {
   var $astname = $statement.astName;
   switch ($astname) { 
     case 'NullStmt':
     case 'BreakStmt':
      case 'NullDecl':
     break;
     
     case 'ExprStmt':
     case 'ReturnStmt':
	  getRefVarsInExpr($statement.getAstChild(0), $refVars,$FunctionRefs );
	  break;
	  
    case 'VarDecl':
	for ( var i = 0; i < $statement.astNumChildren; i++)
	    {
	       var $e = $statement.getAstChild(i);
	       TRACE( $astname + ", Decl [ " + i + " ] = " + $e.code + ", joinPointType =" + $e.astName);
	    }
	       
    break;
    
	case 'DeclStmt':
	    for ( var i = 0; i < $statement.astNumChildren; i++)
	    {
	       var $e = $statement.getAstChild(i);
	       TRACE( $astname + ", Decl [ " + i + " ] = " + $e.code + ", joinPointType =" + $e.joinPointType);
	       getRefVarsInExpr($e, $refVars,$FunctionRefs);
	    }
	break;
	
	 case 'IfStmt':
	 var $e = $statement.cond ;
	  TRACE( " if  cond = " + $e.code);
  	  getRefVarsInExpr($e, $refVars,$FunctionRefs);
          for ( var i = 2; i < $statement.astNumChildren; i++) {
	      var $e = $statement.getAstChild(i);
	      if ($e !== undefined){ // Added 30 Oct 2018
	      	TRACE( $astname + ", child [ " + i + ' ] = ' + $e.astName + ' ' + $e.code);
	        getRefVarsInStmt($e, $refVars,$FunctionRefs );
	        }
	   }

	  break;
	  
     case 'CompoundStmt':   // {  } 
     case 'ForStmt':
     case 'WhileStmt':
     case 'CXXForRangeStmt':  //  for(auto edgeId : Pred[w]) {  }
	   for ( var i = 0; i < $statement.astNumChildren; i++) {
	      var $e = $statement.getAstChild(i);
	      TRACE( $astname + ", child [ " + i + ' ] = ' + $e.astName);
	      getRefVarsInStmt($e, $refVars, $FunctionRefs);
	   }
	 break;
	   
     case 'WrapperStmt': // comment.
     break;
     
     case 'CXXOperatorCallExpr':
     	NYI("getRefVarsInStmt,  CXXOperatorCallExpr detected, code = " +  $statement.code);
 	 break;
    		
     default:
	  NYI(" getRefVarsInStmt, astname = " + $astname);
     }
}

/**     
   Return  (in $refVars) the set of symbols (variables, constants, objects) referenced 
   in an expression ($expr).
  */
function getRefVarsInExpr($expr, $refVars,$FunctionRefs) {
	var $jpt = $expr.joinPointType;
	TRACE(" getRefVarsInExpr = " + $expr.code + "; joinPointType = " + $jpt);
	switch ($jpt) {
       case 'binaryOp':	
		  var $e1 = $expr.left;
		  TRACE (" binaryOp e1 joinPointType = " + $e1.joinPointType );
	      getRefVarsInExpr($e1, $refVars,$FunctionRefs);
	      var $e2 = $expr.right;
	      TRACE (" binaryOp e2 joinPointType = " + $e2.joinPointType );
	      getRefVarsInExpr($e2, $refVars,$FunctionRefs);
          break;
       case 'unaryOp': 
	      var $e1 = $expr.operand;
		  TRACE(" unaryOp operand  joinPointType = " + $e1.joinPointType );
	      getRefVarsInExpr($e1, $refVars,$FunctionRefs);
	    break;
	 
	   case 'arrayAccess':
	   case 'expression':
          for (var i = 0; i < $expr.astNumChildren; i++){
            	getRefVarsInExpr($expr.getAstChild(i), $refVars,$FunctionRefs); 
          }
          break;
	      
	   case 'call':
	      var funcDecl = $expr.decl;
	      if ( isaFunction(funcDecl)) insertInUsingLocation($FunctionRefs, funcDecl);
	      for (var i = 1; i < $expr.astNumChildren; i++) {
                getRefVarsInExpr($expr.getAstChild(i), $refVars,$FunctionRefs); 
          }
          break;

	   case  'varref':
		  var $decl = $expr.vardecl;
		   insertInUsingLocation($refVars, $decl);
          break;

          
        case 'memberCall':  //  obj.m (...) / obj->m(...)
           var $e = $expr.base;
           TRACE("  case memberCall " + $e.code);
           if ( $e.code !== 'this')  getRefVarsInExpr($e , $refVars,$FunctionRefs); 
           var args = $expr.args;
           for (var i = 0; i < args.length; i++){
            	getRefVarsInExpr(args[i], $refVars,$FunctionRefs); 
          }
           break;
                       
	    case 'memberAccess':  //  obj -> member, obj.member
           var $e = $expr.base;
           TRACE( "   case memberAccess. base = " + $e.code);
           if ( $e.code !== 'this')  getRefVarsInExpr($e , $refVars,$FunctionRefs); 
	     break;
	     
	    case 'vardecl':
	       var $e = $expr.init;
	       TRACE("  case vardecl = " + $expr.code);
           if ( $e !== undefined )  getRefVarsInExpr($e , $refVars,$FunctionRefs);
	    break;
	    
        case 'deleteExpr':
	    case 'newExpr':
	    break;

	    default:
	       NYI(" getRefVarsInExpr::" + ", joinPointType = " + $jpt + ", code = " + $expr.code);
	}
}

/**
 Insert a statement ($stmt) in an array (tabDecl) using the location of the objects to
 ensure the unicity in tabDecl.
*/
function insertInUsingLocation(tabDecl, $stmt)
{
  for (var f of tabDecl)
   if (f.location ===  $stmt.location)  return;
   // if ($stmt.astName !== 'ImplicitValueInitExpr' ) TRACE ( " push = " + $stmt.code);
   tabDecl.push($stmt);
}


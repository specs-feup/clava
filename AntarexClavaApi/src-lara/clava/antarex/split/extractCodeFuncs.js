/*
   Author(s): Loïc Besnard (loic.besnard@irisa.fr). 
              IRISA-INRIA Rennes, France.
              
   This code has been developed during the ANTAREX project (http://www.antarex-project.eu).
   It is provided under the MIT license (https://opensource.org/licenses/MIT).
*/


var $EXTRACT_CODE_PRAGMA="EXTRACT";
var $ID_EXTRACTED_CODE="INRIA_EXTRACTED_CODE_";
var $CPT_EXTRACTED_CODE=-1;
var $CURRENT_ID_EXTRACTED_CODE;
var $ISOLATE_PRAGMA="ISOLATE";
var $TMP_PRAGMA='INRIA_TMP_PRAGMA';

/**
  @return true if a pragma ($apragma) is named $EXTRACT_CODE_PRAGMA
*/
function isBeginIsolateCodePragma($apragma){
  return ($apragma.name === $EXTRACT_CODE_PRAGMA);  
}

/** 
  @return a new identifier for the extraction of code.
  The created name is stored in the global variable $CURRENT_ID_EXTRACTED_CODE.
  The name is created from the global variables $EXTRACT_CODE_PRAGMA and $CPT_EXTRACTED_CODE.
*/
function mkNewExtractedCodeIdentifier() {
  $CPT_EXTRACTED_CODE++;
  $CURRENT_ID_EXTRACTED_CODE = $ID_EXTRACTED_CODE + $CPT_EXTRACTED_CODE;
  return $CURRENT_ID_EXTRACTED_CODE;
}

/**
 @return the current identifier for the extraction of code.
*/
function getCurrentExtractedCodeIdentifier() {
 return $CURRENT_ID_EXTRACTED_CODE;
}
/**
 @return the current identifier of the selector for the extraction of code.
*/
function  getExtractCodeSymbolSelectedId()
{
 return $CURRENT_ID_EXTRACTED_CODE + '_select';
}
/**  
	Replace a stamement ($stmt) by a call to $name + $params.
*/
function replaceCodeByCall( $stmt, $name, $params)
{
   $stmt.replaceWith ($name + $params + ";");
}
/**
  Declare a new function(or method) in a new file to encapsulate the code of a statement ($stmt).
  The name of the function is a parameter.
*/
function declareNewIsolateCodeFunctionOrMethod($stmt, name)
{
    var $currentFunction = getFunctionOrMethod($stmt);
    var params = getStrParams($currentFunction);
    var inter = 'void ' + name + params; //'()';
    var newname = declareNewMethod($currentFunction, name , inter );
    if (name === newname) $currentFunction.insert before inter + ';';
	var $nfile = mkNewFile( name +'.cpp', "");
    // reporting the pragmas except the tmp one and the $EXTRACT_CODE_PRAGMA
	var npragmas="";
	var $prags = $stmt.pragmas;
    for ( var $p of $prags) {
       if ($p.name !== $TMP_PRAGMA) 
       	if ($p.name !== $EXTRACT_CODE_PRAGMA) 
         if (npragmas === "" ) npragmas = $p.code; else npragmas = npragmas + '\n' + $p.code;
       }
	var vcode = $stmt.code;
	$nfile.insert before %{ 
	
 void [[newname]] [[ params ]] { 
  [[npragmas]]
  [[ vcode]]
 }
}%;
 ImportDeclarations($stmt, $nfile);
 ImportIncludes($stmt, $nfile);
}

/**
  Declare a new function or method in a new file defined by $funcOrMethodDecl.
*/
function declareIsolateFunctionOrMethod($funcOrMethodDecl, b)
{
  var name = getCurrentExtractedCodeIdentifier();
  var oname= $funcOrMethodDecl.name;
  if (b) renameFunctionMethod($funcOrMethodDecl, name);
  var $nfile = mkNewFile( name +'.cpp', "");
  $nfile.insert before $funcOrMethodDecl.code;
  var $stmt = $funcOrMethodDecl.body;
  ImportDeclarations($stmt, $nfile);
  ImportIncludes($stmt, $nfile);
  if (b) renameFunctionMethod($funcOrMethodDecl, oname);
 }
 

/**
  Generates n copies of the $target with a new name.
  Each copy is generated in a new file.
  @return the table that contains the new names of the function/method.
*/
function genIsolateCodeVersions($target, n) {
   var table=[];

   for (var i=0; i < n; i++){
     mkNewExtractedCodeIdentifier();
     declareIsolateFunctionOrMethod($target, n!==1);
     table.push(getCurrentExtractedCodeIdentifier());
   }
   return table;
}

/**
  Generate the selector of the version to use for the isolated function§method $target.
*/
function genSelectorCode($target, tabNewNames){
 var nb = tabNewNames.length;
 var $body =$target.body; 
 if (nb === 1)
 	{
 	  if (isaFunction($target)) $body.replaceWith(";"); else $target.detach();
 	}
 else {
   $target.insert before 'int ' +  getExtractCodeSymbolSelectedId() + '= 1;';
   var codeSelector = " { \n switch (" + getExtractCodeSymbolSelectedId() + " ) {" ;
   for (var i =1; i <= nb; i++) {
      var code= "\n   case " + i + ':' + "return " + mkCallToRenamed ($target, tabNewNames[i-1]) + ';';
      codeSelector = codeSelector + code;
   }
   codeSelector = codeSelector + "\n}" + "\n}" ;
   $body.replaceWith(codeSelector);
   }
 }

/**
  Declare the created functions/methods, copies of $target but with new names (tabNewNames[])
*/
function genDeclCreatedObjects($target, tabNewNames){
  var nb = tabNewNames.length;
  if (nb === 1) return;
  var $where;
  if (isaFunction($target)) $where = $target; else $where = $target.declarationJp;
  var returnType = getStrReturnType($target);
  var codeParams = getStrParams($target);
  for (var i =1; i <= nb; i++) {
    var codeDecl = returnType + ' '+  tabNewNames[i-1] + ' ' + codeParams + ';';
    $where.insert before codeDecl;
  }
}
 

 
/**
  @return true if a pragma ($apragma) is named $ISOLATE_PRAGMA
*/
function isBeginIsolatePragma($apragma){
  return ($apragma.name === $ISOLATE_PRAGMA);
}

function getAssociatedName($target) {
var $prags = $target.pragmas;
for ( var $p of $prags)
  if ($p.name === $TMP_PRAGMA) return $p.content;
return undefined;
}


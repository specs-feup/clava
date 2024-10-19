/*
   Author(s): Loïc Besnard (loic.besnard@irisa.fr). 
              IRISA-INRIA Rennes, France.
              
   This code has been developed during the ANTAREX project (http://www.antarex-project.eu).
   It is provided under the MIT license (https://opensource.org/licenses/MIT).
*/

import IdentReferences from "@specs-feup/antarex/api/utils/IdentReferences.js";

var LOC_DEBUG_MODE = false;
var $SPLIT_CPT_ID = 0;
var $SPLIT_NEW_ID = "FOO";

/**
  @return true if the identifier of a declaration ($decl) may not be replaced, false otherwise.
    Return true for 
       - global variable, 
       - reference to a member of a class.
*/
export function isfilteredDeclaration($decl) {
    if ($decl.getValue("hasGlobalStorage")) return true;
    if ($decl.getValue("isCXXClassMember")) return true;
    // if (isAConstantDecl($decl)) return  true;
    return false;
}

/**
  Substitute the name of the symbol of a declaration, element of $Decls, by a new name.
  The operation is not applied for some declarations (global one, member of classes,...).
  */
export function substituteSymbols($Decls) {
    TRACE_HERE("  substituteSymbols BEGIN");
    for (var $f of $Decls) {
        if (isfilteredDeclaration($f))
            TRACE_HERE(
                "  Nothing to do for " +
                    $f.code +
                    "  ( GlobalStorage, CXXClassMember,...) "
            );
        else {
            TRACE_HERE(
                " substitute For " + $f.code + " at location " + $f.location
            );
            substituteIdentDeclaration($f);
        }
    }
    TRACE_HERE("  substituteSymbols END");
}

/** 
  Assign a new ident to a declaration ($decl).
*/
export function substituteIdentDeclaration($decl) {
    $decl.setName(getNewId($decl.name));
}

/**
  Declare as global the declaration element of $Decls.
  The operation is not applied for some declarations (global one, member of classes,...).
 */
export function declNewSymbolsAsGlobals($Decls) {
    var op = "DeclNewSymbolsAsGlobals";

    TRACE_HERE("  " + op + " BEGIN");
    for (var $f of $Decls) {
        if (isfilteredDeclaration($f))
            TRACE_HERE(
                "  Nothing to do for " +
                    $f.code +
                    "  ( GlobalStorage, CXXClassMember,...) "
            );
        else {
            TRACE_HERE(" declaration = " + $f.code + " at " + $f.location);
            var $currentFunction = getFunctionOrMethod($f);
            $currentFunction.insertBefore(removeInitDecl($f.copy()));
            // $currentFunction.insert before ";";
        }
    }
    TRACE_HERE("  " + op + " END");
}

export function initMustBeKept($f) {
    if (isAConstantDecl($f)) return false;
    if ($f.astName !== "VarDecl") return false;
    if ($f.isInsideLoopHeader) return false;
    if (!$f.hasInit) return false;
    return true;
}

/**
  Keep initialisation assigned to a declaration, element of $Decls, as a new statement
  The operation is not applied for some declarations (global one, member of classes) 
  and not for an index variable of a loop.
  
  Example:
   For a declaration such that {...; int i = 88; ...}, it produces {...; i = 88; int i = 88; ...}
   The declaration is removed later.
*/
export function keepInitDeclarations($Decls) {
    var op = "KeepInitDeclarations";

    TRACE_HERE("  " + op + " BEGIN");
    for (var $f of $Decls) {
        TRACE_HERE(" Declaration = " + $f.code + " at " + $f.location);
        if (!isfilteredDeclaration($f))
            if (initMustBeKept($f)) {
                TRACE_HERE(" Declaration = " + $f.code + " at " + $f.location);
                var $father = $f.parent; // DeclStmt
                var $vinit = $f.init;
                var code = $vinit.code;
                // For aClass obj; and aClass a class, the added statement will be obj=aClass();
                if (isInitClassWithoutParameters($f)) code = code + "()";
                $father.insert("before", $f.name + " = " + code + ";");
            }
    }
    TRACE_HERE("  " + op + " END");
}

/**
  @return true if a declaration ($decl) is initialized with the default class constructor, false otherwise.
*/
export function isInitClassWithoutParameters($f) {
    if ($f.getValue("initStyle").name() !== "CALL_INIT") return false;
    var $vinit = $f.init;
    if ($vinit.astName !== "CXXConstructExpr") return false;
    return $vinit.astNumChildren === 0;
}

/**
  Remove the declaration, element of $Decls, from the code.
  The operation is not applied for some declarations (global one, member of classes).
*/
export function cleanCode($Decls) {
    TRACE_HERE("  cleanCode BEGIN");
    for (var $f of $Decls) {
        if (!isfilteredDeclaration($f)) {
            var $DeclStmt = $f.parent;
            if (!$f.isInsideLoopHeader) {
                if ($DeclStmt.astNumChildren === 1) $DeclStmt.detach();
                else $f.detach();
            } else {
                var $DeclStmt = $f.parent; // DeclStmt
                var $vloop = $DeclStmt.parent;
                if ($vloop.astIsInstance("ForStmt"))
                    if ($vloop.getAstChild(0).location === $DeclStmt.location)
                        $vloop.setInit($f.name + " = " + $f.init.code);
                    else NYI("cleanCode for " + $f.code + " at " + $f.location);
            }
        }
    }
    TRACE_HERE("  cleanCode END");
}

/**
  Remove the initial value assigned to a declaration.
*/
export function removeInitDecl($vdecl) {
    if (isAConstantDecl($vdecl)) return $vdecl;
    if ($vdecl.astName === "VarDecl") if ($vdecl.hasInit) $vdecl.init.detach();

    return $vdecl;
}

/**
  Remove the initialisations of a decl statement.
 */
export function removeInit($vstmt) {
    for (var $vdecl of $vstmt.astChildren) {
        removeInitDecl($vdecl);
    }
    return $vstmt;
}

export function getImportedIncludes($vFile, id) {
    var Includes = [];
    for (var x = 0; x < $vFile.astNumChildren; x++) {
        var $stmt = $vFile.getAstChild(x);
        if ($stmt.astId === id) break;
        if (
            $stmt.astName === "IncludeDecl" ||
            $stmt.astName === "TypeAliasDecl"
        ) {
            TRACE(" ImportIncludes, Include = " + $stmt.code);
            Includes.push($stmt);
        } else if ($stmt.astName === "DeclStmt") {
            //
            var ch = $stmt.getAstChild(0);
            if (ch.astName === "UsingDirectiveDecl") Includes.push($stmt);
        }
    }
    return Includes;
}

/**
  Import in a file ($afile) the includes of the file of a statement ($aStmt).
*/
export function ImportIncludes($aStmt, $afile) {
    var Includes = getImportedIncludes(
        $aStmt.getAncestor("file"),
        $aStmt.astId
    );
    // Adding declaration in the original file.
    for (f = Includes.length; f > 0; f--) {
        $afile.insertBefore(Includes[f - 1].code);
    }
}

/** Declaration of a new method in a class.

   @param $FunctionOrMethod the ast of a function or a method.
   @param nfunc the name of the new function or method to declare.
   @param inter the interface of the function/method to declare.
   
   @return the name to use for the definition. 
   
   If $FunctionOrMethod is a function, the declaration is not added and nfunc is returned,
   for a method a new method is declared in the class of the $FunctionOrMethod method, it returns 
   the string to use for the definition of the new method (ie C::nfunc, where C is the 
   name of the class of $FunctionOrMethod).
 */
export function declareNewMethod($FunctionOrMethod, nfunc, inter) {
    if (!isaMethod($FunctionOrMethod)) return nfunc;
    var $vdecl = $FunctionOrMethod.declarationJp;
    $vdecl.insert("before", inter + ";");
    addModifiedFiles($vdecl);
    // def for the definition of the method.
    return $FunctionOrMethod.record.name + "::" + nfunc;
}

/**
 Import in a file ($splittedfile) the declaration of the external references of a statement ( $st).
 These declarations are attributed "extern" in the $splittedfile file.
 The declaration imported by the file of ($st) are filtered (they are assumed known via includes).
*/
export function ImportDeclarations($st, $splittedfile) {
    var $vcode;
    var $referencedFunctions = [];
    tabDecl = getExternalDeclsOf($st, $referencedFunctions);
    var stFile = getFileOf($st);
    // Declare same declarations as externals (without initial value) in the new file.
    for (f in tabDecl) {
        var $elem = tabDecl[f];
        // But may introduce a conflict, if it exsists an another global
        // variable with the same name in the application.
        if (getFileOf($elem).filepath === stFile.filepath) {
            if ($elem.storageClass === "static")
                $elem.setValue("storageClass", "NONE");
            var $vstmt = $elem.copy(); // a varDecl
            if (isAConstantDecl($vstmt)) $vcode = $vstmt.code + ";";
            else {
                $vcode = removeInitDecl($vstmt).code + ";";
                if ($elem.storageClass !== "extern")
                    $vcode = "extern " + $vcode;
            }
            $splittedfile.insertBegin($vcode);
        }
    }

    for (refFunc of $referencedFunctions) {
        var vfile = getFileOf(refFunc);
        if (vfile !== undefined)
            if (vfile.filepath === stFile.filepath) {
                if (refFunc.isInline) $splittedfile.insertBegin(refFunc.code);
                else {
                    if (refFunc.isPublic)
                        $vcode = refFunc.getDeclaration(true) + ";";
                    else
                        $vcode = "extern " + refFunc.getDeclaration(true) + ";";
                    $splittedfile.insertBegin($vcode);
                }
            }
    }
}

/**
  @return a new identifier. 
*/
export function getNewId(name) {
    $SPLIT_CPT_ID++;
    return name + "_" + $SPLIT_NEW_ID + "_" + $SPLIT_CPT_ID;
}

export function TRACE_HERE(mess) {
    if (LOC_DEBUG_MODE) console.log(mess);
    else TRACE(mess);
}

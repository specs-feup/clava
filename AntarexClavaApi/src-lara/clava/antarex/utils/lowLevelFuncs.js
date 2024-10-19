/*
   Author(s): Loïc Besnard (loic.besnard@irisa.fr). 
              IRISA-INRIA Rennes, France.
              
   This code has been developed during the ANTAREX project (http://www.antarex-project.eu).
   It is provided under the MIT license (https://opensource.org/licenses/MIT).
*/

import * as sysfile from "./sysfile.js";

/**
  @return the file of an ast object.
*/
export function getFileOf($vtree) {
    return $vtree.getAncestor("file");
}
/**
  @return the function, the method or undefined of a statement ($stmt).
*/
export function getFunctionOrMethod($stmt) {
    var $obj = $stmt.getAncestor("function");
    if ($obj === undefined) $obj = $stmt.getAncestor("method");
    return $obj;
}

/**
  @return true if $vFunction is a method, false otherwise.
*/
export function isaMethod($vFunction) {
    return $vFunction.astName === "CXXMethodDecl";
}

/**
  @return true if $vFunction is a function, false otherwise.
*/
export function isaFunction($vFunction) {
    return $vFunction.astName === "FunctionDecl";
}

/**
 @return the name of the class of a method (m)
*/
export function getClassNameMethod(m) {
    return m.record.name;
}

/**
 @return true if a declaration ($vardecl) is a global one, false otherwise.
*/
export function isGlobalVariable($vardecl) {
    if ($vardecl.isParam) return false;
    return $vardecl.parent.parent.astName === "TranslationUnit"; // global ?
}

/**
  @return true if a statement ($vstmt) is a declaration of a constant, false otherwise.
*/
export function isAConstantDecl($vstmt) {
    if ($vstmt.astName !== "VarDecl") return false;
    $vardeclType = $vstmt.type;
    return $vardeclType.astIsInstance("QualType");
}

/**
  Renames a function or a method ($fm) by name.
*/
export function renameFunctionMethod($fm, name) {
    if (isaFunction($fm)) $fm.setName(name);
    else {
        var x = $fm.record.name;
        $fm.setValue("qualifiedName", x + "::" + name);
        $fm.setValue("declName", name);
    }
}

/**
  @return the code of the returned type of a function/method ($target).
*/
export function getStrReturnType($target) {
    return $target.functionType.returnType.code;
}

/**
 @return a string delc1,...decln where decli is the i-th formal parameter a function/method ($target).
*/
export function getStrParams0($params, bprefix) {
    var first = true;
    var codeParams = "";
    if (bprefix) codeParams = ",";
    for (var i = 0; i < $params.length; i++) {
        if (!first) codeParams = codeParams + ",";
        first = false;
        codeParams = codeParams + $params[i].code;
    }
    return codeParams;
}

/**
 @return a string (delc1,...decln) where decli is the i-th formal parameter a function/method ($target).
*/
export function getStrParams($target) {
    return "(" + getStrParams0($target.params, false) + ")";
}

/*  @return a string p1,...pn where the pi is a name of 
  the i-th parameter of a function ($target).
*/
export function getStrNameParams0($params, bprefix) {
    if ($params.length === 0) return "";
    var first = true;
    var codeParams = "";
    if (bprefix) codeParams = ",";
    for (var i = 0; i < $params.length; i++) {
        if (!first) codeParams = codeParams + ",";
        first = false;
        codeParams = codeParams + $params[i].name;
    }
    return codeParams;
}

/**
 @return a string (p1,...pn) where the pi is a name of 
  the i-th parameter of a function ($target).
*/
export function getStrNameParams($target) {
    return "(" + getStrNameParams0($target.params, false) + ")";
}

/**
  @return a call $name(p1,...pn) where the pi is a name of 
  the i-th parameter of a function ($function).
*/
export function mkCallToRenamed($function, $name) {
    return $name + getStrNameParams($function);
}

/**
  @return the name of the class of a method, empty string for a function.
*/
export function getStrClassOf($fm) {
    if (isaFunction($fm)) return "";
    return getClassNameMethod($fm);
}

/** Debugging */
export function printChildrenOf($statement) {
    for (var i = 0; i < $statement.astNumChildren; i++)
        console.log(" Child " + i + $statement.getAstChild(i).code);
}

/**
 Insert a element ($velem) in an array (tabDecl). It ensures the unicity of element in varray.
*/
export function insertElem(varray, velem) {
    for (var f of varray) if (f === velem) return;
    varray.push(velem);
}

export function getFormalParameters($target) {
    var $fm = getFunctionOrMethod($target);
    return getStrParams($fm);
}

export function getEffectiveParameters($target) {
    var $fm = getFunctionOrMethod($target);
    return getStrNameParams($fm);
}

export function isParameterDeclaration($aDecl) {
    return $aDecl.astName === "ParmVarDecl";
}

/**
 Insert a element ($velem) in an array (tabDecl). It ensures the unicity of element in varray.
*/
export function printArray(varray, message) {
    print(message + "(");
    var first = true;
    for (var f of varray) {
        if (first) {
            first = false;
        } else print(",");
        print(f.code);
    }
    console.log(")");
}

export function getFileSeparator() {
    return "/";
}

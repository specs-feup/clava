/*
   Author(s): Loïc Besnard (loic.besnard@irisa.fr). 
              IRISA-INRIA Rennes, France.
              
   This code has been developed during the ANTAREX project (http://www.antarex-project.eu).
   It is provided under the MIT license (https://opensource.org/licenses/MIT).
*/

import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";

/**  This module defines  
      - the management of the modified files to avoid the complete Clava.rebuild().
      - the system interface access.
  */

/** Current modified files managed by functions addModifiedFiles(), resetModifiedFiles()  and getModifiedFiles().
    To rebuild, call rebuildModifiedFiles() function.
    */
export var _INRIA_MODFIED_FILES_ = [];

/**
  Add a file ($target) to the modified ones.
*/
export function addModifiedFiles($target) {
    insertElem(_INRIA_MODFIED_FILES_, getFileOf($target));
}

/**
  @return the files added to the modified ones since the last resetModifiedFiles() call.
*/
export function getModifiedFiles() {
    return _INRIA_MODFIED_FILES_;
}

/**
  Reset the set of the modified files.
*/
export function resetModifiedFiles() {
    if (_INRIA_MODFIED_FILES_.length !== 0) _INRIA_MODFIED_FILES_.clear();
}

/**
  Rebuilt the modified files (elements of the global array _INRIA_MODFIED_FILES_.
*/
export function rebuildModifiedFiles() {
    rebuildFiles(getModifiedFiles());
}

/**
  Rebuilt the AST of the files of an array (varray).
*/
export function rebuildFiles(varray) {
    for (var afile of varray) {
        console.log(" **** rebuild = " + afile.filepath);
        afile.rebuild();
    }
}

/**
   Declare a new file in Clava, $fp/$name ($fp : file path, $name: the name of the file). 
*/
export function mkNewFile($name, $fp) {
    var nf = ClavaJoinPoints.file($name, $fp);
    Clava.addFile(nf);
    return nf;
}

/**
  @return the name of the file of a library built from libname.
  os DEPENDENT:
    -for linux: lib<libname>.so
*/
export function getOSLibName(libname) {
    return "lib" + libname + ".so";
    // libhost.so (Unix), host.dll (Windows), libhost.jnilib (mac OS).
}

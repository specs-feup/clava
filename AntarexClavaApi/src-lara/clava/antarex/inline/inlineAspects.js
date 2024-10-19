/*
   Author(s): Loïc Besnard (loic.besnard@irisa.fr). 
              IRISA-INRIA Rennes, France.
              
   This code has been developed during the ANTAREX project (http://www.antarex-project.eu).
   It is provided under the MIT license (https://opensource.org/licenses/MIT).
*/
import lowLevelFuncs from "@specs-feup/antarex/api/utils/lowLevelFuncs.js";
import inlineFuncs from "@specs-feup/antarex/api/inline/inlineFuncs.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

/**
  This aspect adds inline attribute for the function/methods attrributed by the pragma INLINE.
*/
export function inline() {
    var $externDecls;
    var found = false;
    // First step.

    for (const $pragma of Query.search("pragma")) {
        if (isInlinePragma($pragma)) {
            found = true;
            var $target = $pragma.target;
            if ($target.isInline) {
                console.log($target.getName() + " is already inlined !!!");
            } else if (inlineObject($target)) {
                MESSAGE(
                    "",
                    " >>> Inline of < " +
                        $target.getName() +
                        ", file =" +
                        $target.getAncestor("file").name +
                        " > is managed by the symbol: " +
                        $CURRENT_ID_INLINE,
                    ""
                );
                $pragma.detach();
            } else {
                ERROR(
                    " The pragma " +
                        $INLINE_PRAGMA +
                        " must be assigned to a function or method definition !"
                );
            }
        }
    }
    if (!found) {
        MESSAGE("", "No pragma " + $INLINE_PRAGMA + " found !!!", "");
    }
}

/**
 Initializing the inlining aspects.
*/
export function inline_initialize() {}

/**
 Finalizing the inlining aspects.
*/
export function inline_finalize() {}

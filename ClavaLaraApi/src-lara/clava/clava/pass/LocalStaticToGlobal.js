import PassTransformationError from "lara-js/api/lara/pass/PassTransformationError.js";
import SimplePass from "lara-js/api/lara/pass/SimplePass.js";
import PassResult from "lara-js/api/lara/pass/results/PassResult.js";
import { DeclStmt, StorageClass, Vardecl, } from "../../Joinpoints.js";
import ClavaJoinPoints from "../ClavaJoinPoints.js";
/**
 * Transforms local static variables into global variables.
 *
 * This means that code like this:
 *
 * ```c
 * int foo() {
 *    static int x = 10;
 *    return x;
 * }
 * ```
 *
 * Will be transformed into:
 *
 * ```c
 * int foo_static_x = 10;
 *
 * int foo() {
 *    return foo_static_x;
 * }
 * ```
 */
export default class LocalStaticToGlobal extends SimplePass {
    _name = "LocalStaticToGlobal";
    matchJoinpoint($jp) {
        // Only vardecls
        if (!($jp instanceof Vardecl)) {
            return false;
        }
        // With static storage
        if ($jp.storageClass !== StorageClass.STATIC) {
            return false;
        }
        // Inside functions - not sure if this is needed
        if ($jp.getAncestor("function") === undefined) {
            return false;
        }
        return true;
    }
    transformJoinpoint($jp) {
        const $function = $jp.getAncestor("function");
        if (!$function) {
            throw new PassTransformationError(this, $jp, "Expected ancestor of type function, found 'undefined'");
        }
        const newName = $function.name + "_static_" + $jp.name;
        $jp.name = newName;
        $jp.storageClass = StorageClass.NONE;
        const $declStmt = $jp.parent;
        if (!($declStmt instanceof DeclStmt)) {
            throw new PassTransformationError(this, $jp, "Expected declStmt, found '" + $declStmt.joinPointType + "'");
        }
        $jp.detach();
        $function.insertBefore($jp);
        // Remove declStmt if empty
        if ($declStmt.decls.length === 0) {
            $declStmt.detach();
        }
        return new PassResult(this, ClavaJoinPoints.emptyStmt());
    }
}
//# sourceMappingURL=LocalStaticToGlobal.js.map
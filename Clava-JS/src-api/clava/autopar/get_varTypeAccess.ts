import { debug } from "@specs-feup/lara/api/lara/core/LaraCore.js";
import {
    ArrayAccess,
    BuiltinType,
    Joinpoint,
    MemberAccess,
    PointerType,
    QualType,
    TypedefType,
    Varref,
} from "../../Joinpoints.js";
import { normalizeVarName } from "./allReplace.js";

/**************************************************************
 *
 *                       get_varTypeAccess
 *
 **************************************************************/
export default function get_varTypeAccess($varJP: Joinpoint, op?: string) {
    op = typeof op !== "undefined" ? op : "all";

    let varTypeAccess = null;
    let varUse = null;
    let varName = null;
    let vardecl = null;

    if (
        $varJP instanceof ArrayAccess &&
        ($varJP.type instanceof BuiltinType ||
            $varJP.type instanceof QualType ||
            $varJP.type instanceof TypedefType)
    ) {
        if ($varJP.arrayVar.joinPointType === "memberAccess") {
            varTypeAccess = "memberArrayAccess";
            varUse = $varJP.use;
            varName = normalizeVarName($varJP.code);
        } else if ($varJP.arrayVar.joinPointType === "varref") {
            varTypeAccess = "arrayAccess";
            vardecl = $varJP.arrayVar.vardecl;
            if (vardecl === undefined) {
                debug(
                    "autopar.get_varTypeAccess: Could not find vardecl of arrayVar@" +
                        $varJP.arrayVar.location
                );
                vardecl = null;
            }
            varUse = $varJP.use;
            varName = normalizeVarName($varJP.code);
        }
    }

    if (
        $varJP instanceof MemberAccess &&
        ($varJP.type instanceof BuiltinType || $varJP instanceof QualType)
    ) {
        varTypeAccess = "memberAccess";
        varUse = $varJP.use;
        varName = normalizeVarName($varJP.code);
    }

    if (
        $varJP instanceof Varref &&
        ($varJP.type instanceof BuiltinType ||
            $varJP.type instanceof QualType ||
            $varJP.type instanceof PointerType ||
            $varJP.type instanceof TypedefType)
    ) {
        varTypeAccess = "varref";
        varUse = $varJP.useExpr.use;
        vardecl = $varJP.vardecl;
        if (vardecl === undefined) {
            debug(
                "autopar.get_varTypeAccess: Could not find vardecl of var@" +
                    $varJP.location
            );
            vardecl = null;
        }

        varName = normalizeVarName($varJP.code);
    }

    if (op !== "all") return;

    return { varTypeAccess, varUse, varName, vardecl };
}

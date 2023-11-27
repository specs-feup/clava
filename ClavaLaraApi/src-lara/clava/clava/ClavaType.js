import { ParenType, PointerType, VariableArrayType, } from "../Joinpoints.js";
import ClavaJoinPoints from "./ClavaJoinPoints.js";
import ClavaJavaTypes from "./ClavaJavaTypes.js";
/**
 * Utility methods related with the type join points.
 *
 */
export default class ClavaType {
    /**
     * @param type - The type to visit
     * @param exprFunction - A function that receives an $expr join point
     *
     * @returns The $type after applying the given exprFunction to its $expr nodes. If any of the fields of the type is visited, a copy of the type is returned.
     */
    static visitExprInTypeCopy($type, exprFunction) {
        if ($type instanceof PointerType) {
            const $typeCopy = $type.copy();
            $typeCopy.setPointee(ClavaType.visitExprInTypeCopy($typeCopy.pointee, exprFunction));
            return $typeCopy;
        }
        if ($type instanceof ParenType) {
            const $typeCopy = $type.copy();
            $typeCopy.setDesugar(ClavaType.visitExprInTypeCopy($typeCopy.desugar, exprFunction));
            return $typeCopy;
        }
        if ($type instanceof VariableArrayType) {
            const $typeCopy = $type.copy();
            $typeCopy.setSizeExpr($typeCopy.sizeExpr.copy());
            exprFunction($typeCopy.sizeExpr);
            return $typeCopy;
        }
        return $type;
    }
    /**
     * @param type - A type join point that will be visited looking for $expr join points. Any visited nodes in the type (e.g., desugar) will be copied, so that the returned varrefs can be safely modified.
     * @param varrefs - An array (possibly empty) where the $varref join points found in the given type will be stored
     *
     * @returns A copy of the given $type, to which the varrefs refer to
     */
    static getVarrefsInTypeCopy($type, varrefs) {
        const exprFunction = function ($expr) {
            for (const $varref of $expr.getDescendantsAndSelf("varref")) {
                varrefs.push($varref);
            }
        };
        return ClavaType.visitExprInTypeCopy($type, exprFunction);
    }
    /**
     * Makes sure the given parameter is an expression join point.
     *
     * @param $expression - If a string, returns a literal expression with the code of the string. Otherwise, returns $expression
     * @param isOptional - If false and $expression is undefined, throws an exception. Otherwise, returns undefined if $expression is undefined.
     */
    static asExpression($expression, isOptional = false) {
        if ($expression === undefined) {
            if (isOptional) {
                return undefined;
            }
            else {
                throw "ClavaType.asExpression: $expression is undefined. If this is allowed, set 'isOptional' to true.";
            }
        }
        return $expression;
    }
    /**
     * Makes sure the given parameter is a statement join point.
     *
     * @param code - If a string, returns a literal statement with the code of the string. Otherwise, tries to transform the given node to a $statement
     * @param isOptional - If false and code is undefined, throws an exception. Otherwise, returns undefined if code is undefined.
     */
    static asStatement(code, isOptional = false) {
        if (code === undefined) {
            if (isOptional) {
                return undefined;
            }
            else {
                throw "ClavaType.asStatement: code is undefined. If this is allowed, set 'isOptional' to true.";
            }
        }
        const newStmtNode = ClavaJavaTypes.getClavaNodes().toStmt(code.node);
        return ClavaJoinPoints.toJoinPoint(newStmtNode);
    }
    /**
     * Makes sure the given parameter is a type join point.
     *
     * @param $type - If a string, returns a literal type with the code of the string. Otherwise, returns $type
     *
     * @deprecated This method does not do anything, it is only kept for compatibility with the old API
     */
    static asType($type) {
        return $type;
    }
    /**
     *  Makes sure the given parameter is a scope join point.
     *
     * @param code - If a string, returns a literal statement with the code of the string. Otherwise, returns $statement
     */
    static asScope(code) {
        const $newStmt = ClavaType.asStatement(code);
        const newScopeNode = ClavaJavaTypes.getClavaNodes().toCompoundStmt($newStmt?.node);
        return ClavaJoinPoints.toJoinPoint(newScopeNode);
    }
}
//# sourceMappingURL=ClavaType.js.map
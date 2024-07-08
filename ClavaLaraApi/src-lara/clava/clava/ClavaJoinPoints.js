import { unwrapJoinPoint, wrapJoinPoint, } from "lara-js/api/LaraJoinPoint.js";
import { arrayFromArgs, flattenArgsArray, } from "lara-js/api/lara/core/LaraCore.js";
import * as Joinpoints from "../Joinpoints.js";
import Clava from "./Clava.js";
import ClavaJavaTypes from "./ClavaJavaTypes.js";
/**
 * Utility methods related with the creation of new join points.
 *
 */
export default class ClavaJoinPoints {
    static toJoinPoint(node) {
        return wrapJoinPoint(ClavaJavaTypes.CxxJoinPoints.createFromLara(node));
    }
    /**
     * @returns True, if the two AST nodes are equal (internal representation. Not actual Joinpoint types), in the sense that the underlying AST nodes are also equal, according to their .equals() method (might return true for different AST nodes).
     * @internal
     * @deprecated Internal representations of join points should not be exposed to the public API.
     */
    static equals(jp1, jp2) {
        return jp1.equals(jp2);
    }
    static builtinType(code) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.builtinType(code));
    }
    static pointerFromBuiltin(code) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.pointerTypeFromBuiltin(code));
    }
    static pointer($type) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.pointerType(unwrapJoinPoint($type)));
    }
    /**
     * Builds an array type of constant dimensions.
     *
     * @param type - Represents the inner type of the array. If passed a string then it will be converted to a literal type.
     * @param dims - Represents the dimensions of the array.
     **/
    static constArrayType(type, ...dims) {
        if (!Array.isArray(dims)) {
            dims = arrayFromArgs(dims);
        }
        if (typeof type === "string") {
            return wrapJoinPoint(ClavaJavaTypes.AstFactory.constArrayType(type, Clava.getStandard(), dims));
        }
        else if (type instanceof Joinpoints.Type) {
            return wrapJoinPoint(ClavaJavaTypes.AstFactory.constArrayType(type.node, Clava.getStandard(), dims));
        }
        else {
            throw 'ClavaJoinPoints.constArrayType: illegal argument "type", needs to be either a string or a type join point';
        }
    }
    static variableArrayType($type, $sizeExpr) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.variableArrayType(unwrapJoinPoint($type), unwrapJoinPoint($sizeExpr)));
    }
    static exprLiteral(code, type) {
        if (type === undefined) {
            return wrapJoinPoint(ClavaJavaTypes.AstFactory.exprLiteral(code));
        }
        if (typeof type === "string") {
            type = this.builtinType(type);
        }
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.exprLiteral(code, unwrapJoinPoint(type)));
    }
    /**
     *
     * @param type - The type of the constructed object
     * @param constructorArguments - Arguments passed to the constructor function
     *
     */
    static cxxConstructExpr(type, ...constructorArguments) {
        const processedArguments = constructorArguments.map((arg) => {
            if (typeof arg === "string") {
                return this.exprLiteral(arg);
            }
            return arg;
        });
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.cxxConstructExpr(unwrapJoinPoint(type), unwrapJoinPoint(processedArguments)));
    }
    static varDecl(varName, init) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.varDecl(varName, unwrapJoinPoint(init)));
    }
    static varDeclNoInit(varName, type) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.varDeclNoInit(varName, unwrapJoinPoint(type)));
    }
    /**
     * Creates a new literal join point 'type'.
     *
     * @param typeString - The literal code of the type
     */
    static typeLiteral(typeString) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.typeLiteral(typeString));
    }
    /**
     * Creates a new join point 'file'.
     *
     * @param filename - Name of the source file. If filename represents a path to an already existing file, literally adds the contents of the file to the join point.
     * @param path - The path of the new file, relative to the output folder. Absolute paths are not allowed. This path will be required when including the file (e.g., #include "<path>/<filename>")
     */
    static file(filename, path = "") {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.file(unwrapJoinPoint(filename), path));
    }
    /**
     * @param filename - Name of the source file.
     * @param source - The contents of the source file.
     * @param path - The path of the new file, relative to the output folder. Absolute paths are not allowed. This path will be required when including the file (e.g., #include "<path>/<filename>")
     */
    static fileWithSource(filename, source, path = "") {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.file(filename, source, path));
    }
    /**
     * Creates a new literal join point 'stmt'.
     *
     * @param stmtString - The literal code of the statement.
     */
    static stmtLiteral(stmtString) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.stmtLiteral(stmtString));
    }
    static emptyStmt() {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.emptyStmt());
    }
    /**
     * Creates a new join point 'call'.
     *
     * @param $function - The function for which the call will refer to.
     * @param callArgs - The arguments of the function.
     */
    static call($function, ...callArgs) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.callFromFunction(unwrapJoinPoint($function), ...unwrapJoinPoint(flattenArgsArray(callArgs))));
    }
    /**
     * Creates a new join point 'call'.
     *
     * @param functionName - The name of the function to call.
     * @param $returnType - The return type of the function.
     * @param callArgs - The arguments of the function.
     */
    static callFromName(functionName, $returnType, ...callArgs) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.call(functionName, unwrapJoinPoint($returnType), ...flattenArgsArray(callArgs).map(unwrapJoinPoint)));
    }
    /**
     * Creates a new join point 'switch'
     *
     */
    static switchStmt($conditionExpr, ...cases) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.switchStmt(unwrapJoinPoint($conditionExpr), unwrapJoinPoint(flattenArgsArray(cases))));
    }
    static omp(directiveName) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.omp(directiveName));
    }
    static scope(...$jps) {
        $jps = flattenArgsArray($jps);
        if ($jps.length === 0) {
            return wrapJoinPoint(ClavaJavaTypes.AstFactory.scope());
        }
        const $stmts = $jps.map(($stmt) => {
            const $normalizedStmt = $stmt.stmt;
            if ($normalizedStmt === undefined) {
                throw new Error(`Could not convert ${$stmt.joinPointType} to a statement, and a scope only accepts statements`);
            }
            return $normalizedStmt;
        });
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.scope(...unwrapJoinPoint($stmts)));
    }
    static varRef(decl, $type) {
        if (typeof decl === "string") {
            return wrapJoinPoint(ClavaJavaTypes.AstFactory.varref(decl, unwrapJoinPoint($type)));
        }
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.varref(unwrapJoinPoint(decl)));
    }
    /**
     * @param declName - The name of the varref.
     * @deprecated use ClavaJoinPoints.varRef() instead
     */
    static varRefFromDecl($namedDecl) {
        return ClavaJoinPoints.varRef($namedDecl);
    }
    /**
     * @param $expr - An expression to return.
     */
    static returnStmt($expr) {
        if ($expr === undefined) {
            return wrapJoinPoint(ClavaJavaTypes.AstFactory.returnStmt());
        }
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.returnStmt(unwrapJoinPoint($expr)));
    }
    /**
     * Creates a new join point 'functionType'.
     *
     * @param $returnType - The return type of the function type.
     * @param argTypes - The types of the arguments of the function type.
     */
    static functionType($returnType, ...argTypes) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.functionType(unwrapJoinPoint($returnType), unwrapJoinPoint(flattenArgsArray(argTypes))));
    }
    /**
     * Creates a new join point 'function'.
     *
     * @param functionName - The name of the function.
     * @param $functionType - The type of the function.
     */
    static functionDeclFromType(functionName, $functionType) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.functionDeclFromType(functionName, unwrapJoinPoint($functionType)));
    }
    /**
     * Creates a new join point 'function'.
     * // TODO update docs
     *
     * @param functionName - The name of the function.
     * @param $returnType - The return type of the function
     * @param params - The parameters of the function.
     */
    static functionDecl(functionName, $returnType, ...params) {
        const $paramVarDecls = flattenArgsArray(params);
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.functionDecl(functionName, unwrapJoinPoint($returnType), unwrapJoinPoint($paramVarDecls)));
    }
    static assign($leftHand, $rightHand) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.assignment(unwrapJoinPoint($leftHand), unwrapJoinPoint($rightHand)));
    }
    static compoundAssign(op, $leftHand, $rightHand) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.compoundAssignment(op, unwrapJoinPoint($leftHand), unwrapJoinPoint($rightHand)));
    }
    /**
     * Creates a new join point 'if'.
     *
     * @param $condition - The condition of the if statement. If a string, it is converted to a literal expression.
     * @param $then - The body of the if
     * @param $else - The body of the else
     *
     */
    static ifStmt($condition, $then, $else) {
        if (typeof $condition === "string") {
            $condition = ClavaJoinPoints.exprLiteral($condition);
        }
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.ifStmt(unwrapJoinPoint($condition), unwrapJoinPoint($then), unwrapJoinPoint($else)));
    }
    /**
     * Creates a new join point 'binaryOp'.
     *
     * @param op - The binary operator kind.
     * @param $left - The left hand of the binary operator. If a string, it is converted to a literal expression.
     * @param $right - The right hand of the binary operator. If a string, it is converted to a literal expression.
     * @param $type - The return type of the operator. If a string, it is converted to a literal type.
     */
    static binaryOp(op, $left, $right, $type = "int") {
        if (typeof $left === "string") {
            $left = ClavaJoinPoints.exprLiteral($left);
        }
        if (typeof $right === "string") {
            $right = ClavaJoinPoints.exprLiteral($right);
        }
        if (typeof $type === "string") {
            $type = ClavaJoinPoints.typeLiteral($type);
        }
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.binaryOp(op, unwrapJoinPoint($left), unwrapJoinPoint($right), unwrapJoinPoint($type)));
    }
    static unaryOp(op, $expr, $type) {
        if (typeof $expr === "string") {
            $expr = ClavaJoinPoints.exprLiteral($expr);
        }
        if (typeof $type === "string") {
            $type = ClavaJoinPoints.typeLiteral($type);
        }
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.unaryOp(op, unwrapJoinPoint($expr), unwrapJoinPoint($type)));
    }
    /**
     * Creates a new join point 'ternaryOp'
     *
     * @param $cond - The condition of the operator
     * @param $trueExpr - The result when $cond evaluates to true
     * @param $falseExpr - The result when $cond evaluates to false
     * @param $type - The type of the operation
     * @returns The newly created join point
     */
    static ternaryOp($cond, $trueExpr, $falseExpr, $type) {
        if (typeof $cond === "string") {
            $cond = ClavaJoinPoints.exprLiteral($cond);
        }
        if (typeof $trueExpr === "string") {
            $trueExpr = ClavaJoinPoints.exprLiteral($trueExpr);
        }
        if (typeof $falseExpr === "string") {
            $falseExpr = ClavaJoinPoints.exprLiteral($falseExpr);
        }
        if (typeof $type === "string") {
            $type = ClavaJoinPoints.typeLiteral($type);
        }
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.ternaryOp(unwrapJoinPoint($cond), unwrapJoinPoint($trueExpr), unwrapJoinPoint($falseExpr), unwrapJoinPoint($type)));
    }
    /**
     * Creates a new join point 'expr' representing a parenthesis expression.
     *
     * @param $expr - The expression inside the parenthesis. If a string, it is converted to a literal expression.
     */
    static parenthesis($expr) {
        if (typeof $expr === "string") {
            $expr = ClavaJoinPoints.exprLiteral($expr);
        }
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.parenthesis(unwrapJoinPoint($expr)));
    }
    /**
     * @param doubleLiteral - The number that will be a double literal.
     */
    static doubleLiteral(doubleLiteral) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.doubleLiteral(doubleLiteral));
    }
    /**
     * @param integerLiteral - The number that will be a integer literal.
     */
    static integerLiteral(integerLiteral) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.integerLiteral(integerLiteral));
    }
    /**
     * @param $typedefDecl - A typedef declaration.
     */
    static typedefType($typedefDecl) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.typedefType(unwrapJoinPoint($typedefDecl)));
    }
    /**
     * @param $underlyingType - The underlying type of the typedef.
     * @param identifier - The name of the typedef.
     */
    static typedefDecl($underlyingType, identifier) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.typedefDecl(unwrapJoinPoint($underlyingType), identifier));
    }
    /**
     * @param $struct - a struct for the type
     * @returns An elaborated type for the given struct.
     */
    static structType($struct) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.structType(unwrapJoinPoint($struct)));
    }
    /**
     * Represents an explicit C-style cast (e.g., (double) a).
     *
     */
    static cStyleCast($type, $expr) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.cStyleCast(unwrapJoinPoint($type), unwrapJoinPoint($expr)));
    }
    /**
     * Creates an empty class with the given name and fields.
     *
     */
    static classDecl(className, ...fields) {
        const flattenedFields = flattenArgsArray(fields);
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.classDecl(className, unwrapJoinPoint(flattenedFields)));
    }
    /**
     * Creates a field for a class.
     *
     */
    static field(fieldName, $fieldType) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.field(fieldName, unwrapJoinPoint($fieldType)));
    }
    /**
     * Creates an access specifier (e.g., public:), for classes.
     *
     * @param accessSpecifier - One of public, protected, private or none.
     */
    static accessSpecifier(accessSpecifier) {
        // TODO: Make this an enum
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.accessSpecifier(accessSpecifier));
    }
    /**
     * Creates a new 'for' statement join point.
     *
     * @param $init - The initialization of the for statement. If a string, it is converted to a literal expression.
     * @param $condition - The condition of the for statement. If a string, it is converted to a literal expression.
     * @param $inc - The increment of the for statement. If a string, it is converted to a literal expression.
     * @param $body - The body of the for statement.
     *
     */
    static forStmt($init, $condition, $inc, $body) {
        if (typeof $init === "string") {
            $init = ClavaJoinPoints.stmtLiteral($init);
        }
        if (typeof $condition === "string") {
            $condition = ClavaJoinPoints.stmtLiteral($condition);
        }
        if (typeof $inc === "string") {
            $inc = ClavaJoinPoints.stmtLiteral($inc);
        }
        if (typeof $body === "string") {
            $body = ClavaJoinPoints.stmtLiteral($body);
        }
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.forStmt(unwrapJoinPoint($init), unwrapJoinPoint($condition), unwrapJoinPoint($inc), unwrapJoinPoint($body)));
    }
    static whileStmt($condition, $body) {
        if (typeof $condition === "string") {
            $condition = ClavaJoinPoints.stmtLiteral($condition);
        }
        if (typeof $body === "string") {
            $body = ClavaJoinPoints.stmtLiteral($body);
        }
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.whileStmt(unwrapJoinPoint($condition), unwrapJoinPoint($body)));
    }
    static param(name, $type) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.param(name, unwrapJoinPoint($type)));
    }
    /**
     * Tries to convert the given source into a join point, or throws an exception if it is not possible.
     *
     * If source is a string:
     *  - If it can be converted to a builtinType, returns a builtinType;
     *  - Otherwise, returns a typeLiteral;
     *
     * If source is a join point:
     *  - If is a $type, returns itself;
     *  - If the property .type is not undefined, returns .type;
     *  - Otherwise, throws an exception;
     *
     * @param source -
     * @returns A join point representing a type.
     */
    static type(source) {
        if (typeof source === "string") {
            if (ClavaJavaTypes.BuiltinKind.isBuiltinKind(source)) {
                return ClavaJoinPoints.builtinType(source);
            }
            return ClavaJoinPoints.typeLiteral(source);
        }
        if (source instanceof Joinpoints.Type) {
            return source;
        }
        if (source.type !== undefined) {
            return source.type;
        }
        throw new Error("ClavaJoinPoints.type: source is join point but is not a type, nor has a .type property: " +
            source.joinPointType);
    }
    static exprStmt($expr) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.exprStmt(unwrapJoinPoint($expr)));
    }
    /**
     * Creates an empty class with the given name and fields.
     *
     */
    static declStmt(...decls) {
        const flattenedDecls = flattenArgsArray(decls);
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.declStmt(unwrapJoinPoint(flattenedDecls)));
    }
    /**
     * Creates a comment join point from the given text. If text has one line, creates an inline comment, otherwise creates a multi-line comment.
     *
     * @param text - The text of the comment
     * @returns A comment join point
     *
     */
    static comment(text) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.comment(text));
    }
    static labelDecl(name) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.labelDecl(name));
    }
    static labelStmt(nameOrDecl) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.labelStmt(unwrapJoinPoint(nameOrDecl)));
    }
    static gotoStmt(labelDecl) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.gotoStmt(unwrapJoinPoint(labelDecl)));
    }
    /**
     * Creates a new literal join point 'decl'.
     *
     * @param declString - The literal code of the decl.
     */
    static declLiteral(declString) {
        return wrapJoinPoint(ClavaJavaTypes.AstFactory.declLiteral(declString));
    }
}
//# sourceMappingURL=ClavaJoinPoints.js.map
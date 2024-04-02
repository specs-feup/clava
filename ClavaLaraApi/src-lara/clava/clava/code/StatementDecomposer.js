import { debug } from "lara-js/api/lara/core/LaraCore.js";
import { BinaryOp, Call, Case, DeclStmt, EmptyStmt, ExprStmt, LabelStmt, MemberCall, ReturnStmt, Scope, Statement, TernaryOp, UnaryOp, Vardecl, } from "../../Joinpoints.js";
import ClavaJoinPoints from "../ClavaJoinPoints.js";
import DecomposeResult from "./DecomposeResult.js";
/**
 * Decomposes complex statements into several simpler ones.
 */
export default class StatementDecomposer {
    tempPrefix;
    startIndex;
    constructor(tempPrefix = "decomp_", startIndex = 0) {
        this.tempPrefix = tempPrefix;
        this.startIndex = startIndex;
    }
    newTempVarname() {
        const varName = `${this.tempPrefix}${this.startIndex}`;
        this.startIndex++;
        return varName;
    }
    isValidNode($jp) {
        // If preceeding statement is a CaseStmt or a LabelStmt it might generate invalid code if a declaration is inserted
        // Add empty statement after the label to avoid this situation
        if ($jp instanceof Statement && !($jp instanceof EmptyStmt)) {
            const $leftStmt = $jp.leftJp;
            if ($leftStmt !== undefined &&
                ($leftStmt instanceof Case || $leftStmt instanceof LabelStmt)) {
                debug(`StatementDecomposer: statement just before label, inserting empty statement after as a precaution`);
                $leftStmt.insertAfter(ClavaJoinPoints.emptyStmt());
                return true;
            }
            return true;
        }
        const parentStmt = $jp.getAncestor("statement");
        if (parentStmt !== undefined) {
            return this.isValidNode(parentStmt);
        }
        return true;
    }
    /**
     * If the given statement can be decomposed in two or more statements, replaces the statement with the decomposition.
     *
     * @param $stmt - A statement that will be decomposed.
     */
    decomposeAndReplace($stmt) {
        const stmts = this.decompose($stmt);
        // No statements to replace
        if (stmts.length === 0) {
            return;
        }
        // Replace stmt with array of statements
        $stmt.replaceWith(stmts);
    }
    /**
     * @param $stmt - A statement that will be decomposed.
     * @returns An array with the new statements, or an empty array if no decomposition could be made
     */
    decompose($stmt) {
        try {
            return this.decomposeStmt($stmt);
        }
        catch (e) {
            console.log(`StatementDecomposer: ${String(e)}`);
            return [];
        }
    }
    decomposeStmt($stmt) {
        // Unsupported
        if ($stmt instanceof Scope || $stmt.joinPointType === "statement") {
            debug(`StatementDecomposer: skipping scope or generic statement join point`);
            return [];
        }
        if (!this.isValidNode($stmt)) {
            return [];
        }
        if ($stmt instanceof ExprStmt) {
            return this.decomposeExprStmt($stmt);
        }
        if ($stmt instanceof ReturnStmt) {
            return this.decomposeReturnStmt($stmt);
        }
        if ($stmt instanceof DeclStmt) {
            return this.decomposeDeclStmt($stmt);
        }
        debug(`StatementDecomposer: not implemented for statement of type ${$stmt.joinPointType}`);
        return [];
    }
    decomposeExprStmt($stmt) {
        // Statement represents an expression
        const $expr = $stmt.expr;
        const { precedingStmts, succeedingStmts } = this.decomposeExpr($expr);
        return [...precedingStmts, ...succeedingStmts];
    }
    decomposeReturnStmt($stmt) {
        // Return may contain an expression
        const $expr = $stmt.returnExpr;
        if ($expr === undefined) {
            return [];
        }
        const { precedingStmts, $resultExpr } = this.decomposeExpr($expr);
        const $newReturnStmt = ClavaJoinPoints.returnStmt($resultExpr);
        return [...precedingStmts, $newReturnStmt];
    }
    decomposeDeclStmt($stmt) {
        // declStmt can have one or more declarations
        const $decls = $stmt.decls;
        return $decls.flatMap(($decl) => this.decomposeDecl($decl));
    }
    decomposeDecl($decl) {
        if (!($decl instanceof Vardecl)) {
            debug(`StatementDecomposer.decomposeDeclStmt: not implemented for decl of type ${$decl.joinPointType}`);
            return [ClavaJoinPoints.declStmt($decl)];
        }
        // If vardecl has init, decompose expression
        if ($decl.hasInit) {
            const decomposeResult = this.decomposeExpr($decl.init);
            //expr = newStmts.concat(decomposeResult.stmts);
            $decl.init = decomposeResult.$resultExpr;
            return [
                ...decomposeResult.precedingStmts,
                ClavaJoinPoints.declStmt($decl),
                ...decomposeResult.succeedingStmts,
            ];
        }
        return [ClavaJoinPoints.declStmt($decl)];
    }
    decomposeExpr($expr) {
        if (!this.isValidNode($expr)) {
            return new DecomposeResult([], $expr);
        }
        if ($expr instanceof BinaryOp) {
            return this.decomposeBinaryOp($expr);
        }
        if ($expr instanceof UnaryOp) {
            return this.decomposeUnaryOp($expr);
        }
        if ($expr instanceof TernaryOp) {
            return this.decomposeTernaryOp($expr);
        }
        if ($expr instanceof Call) {
            return this.decomposeCall($expr);
        }
        if ($expr.numChildren === 0) {
            return new DecomposeResult([], $expr);
        }
        debug(`StatementDecomposer: decomposition not implemented for type ${$expr.joinPointType}. Returning '${$expr.code}'as is`);
        return new DecomposeResult([], $expr);
    }
    decomposeCall($call) {
        const argResults = $call.args.map(($arg) => this.decomposeExpr($arg));
        const precedingStmts = argResults.flatMap((res) => res.precedingStmts);
        const succeedingStmts = argResults.flatMap((res) => res.succeedingStmts);
        const newArgs = argResults.map((res) => res.$resultExpr);
        const $newCall = this.copyCall($call, newArgs);
        //const $newCall = ClavaJoinPoints.call($call.function, ...newArgs);
        // Desugaring type, to avoid possible problems of code generation of more complex types
        // E.g. for vector.size(), currently is generating code without the qualifier
        const desugaredReturnType = $newCall.type.desugarAll;
        // If call is inside an exprStmt, and exprStmt is inside a scope, just convert new call to statement
        // The scope test is to avoid wrong code in situations such as loop headers
        if ($call.parent !== undefined &&
            $call.parent instanceof ExprStmt &&
            $call.parent.parent !== undefined &&
            $call.parent.parent instanceof Scope) {
            // Using .exprStmt() to ensure a new statement is created.
            // .stmt might not create a new statement, and interfere with detaching the previous stmt
            const newStmts = [...precedingStmts, ClavaJoinPoints.exprStmt($newCall)];
            return new DecomposeResult(newStmts, $newCall, []);
        }
        // Otherwise, create a variable to store the result of the call
        // and return the variable as the value expression
        const tempVarname = this.newTempVarname();
        const tempVarDecl = ClavaJoinPoints.varDeclNoInit(tempVarname, desugaredReturnType);
        const tempVarAssign = ClavaJoinPoints.assign(ClavaJoinPoints.varRef(tempVarDecl), $newCall);
        return new DecomposeResult([...precedingStmts, tempVarDecl.stmt, tempVarAssign.stmt], ClavaJoinPoints.varRef(tempVarDecl), succeedingStmts);
    }
    copyCall($call, newArgs) {
        // Instance method
        if ($call instanceof MemberCall) {
            // Copy node
            const $newCall = $call.copy();
            // Update args
            // TODO: use a kind of .setArgs that replaces all
            for (let i = 0; i < newArgs.length; i++) {
                $newCall.setArg(i, newArgs[i]);
            }
            return $newCall;
        }
        // Default
        return ClavaJoinPoints.call($call.function, ...newArgs);
    }
    decomposeBinaryOp($binaryOp) {
        if ($binaryOp.isAssignment) {
            return this.decomposeAssignment($binaryOp);
        }
        // Apply decompose to both sides
        const leftResult = this.decomposeExpr($binaryOp.left);
        const rightResult = this.decomposeExpr($binaryOp.right);
        // Create operation with result of decomposition
        const $newExpr = ClavaJoinPoints.binaryOp($binaryOp.kind, leftResult.$resultExpr, rightResult.$resultExpr, $binaryOp.type);
        // Create declaration statement with result to new temporary variable
        const tempVarname = this.newTempVarname();
        const tempVarDecl = ClavaJoinPoints.varDeclNoInit(tempVarname, $binaryOp.type);
        const tempVarAssign = ClavaJoinPoints.assign(ClavaJoinPoints.varRef(tempVarDecl), $newExpr);
        const precedingStmts = [
            ...leftResult.precedingStmts,
            ...rightResult.precedingStmts,
            tempVarDecl.stmt,
            tempVarAssign.stmt,
        ];
        const succeedingStmts = [
            ...leftResult.succeedingStmts,
            ...rightResult.succeedingStmts,
        ];
        return new DecomposeResult(precedingStmts, ClavaJoinPoints.varRef(tempVarDecl), succeedingStmts);
    }
    decomposeAssignment($assign) {
        // Get statements of right hand-side
        const rightResult = this.decomposeExpr($assign.right);
        const $newAssign = $assign.operator === "="
            ? ClavaJoinPoints.assign($assign.left, rightResult.$resultExpr)
            : ClavaJoinPoints.compoundAssign($assign.operator, $assign.left, rightResult.$resultExpr);
        const $assignExpr = ClavaJoinPoints.exprStmt($newAssign);
        return new DecomposeResult([...rightResult.precedingStmts, $assignExpr], $assign.left, rightResult.succeedingStmts);
    }
    decomposeTernaryOp($ternaryOp) {
        const condResult = this.decomposeExpr($ternaryOp.cond);
        const trueResult = this.decomposeExpr($ternaryOp.trueExpr);
        const falseResult = this.decomposeExpr($ternaryOp.falseExpr);
        const tempVarname = this.newTempVarname();
        const tempVarDecl = ClavaJoinPoints.varDeclNoInit(tempVarname, $ternaryOp.type);
        // assign the value of the new temp variable with an if-else statement
        // to maintain the semantics of only evaluating the expression that
        // falls on the right side of the ternary.
        // we do not want side-effects to be executed without regard to the branch
        // taken
        const $thenBody = ClavaJoinPoints.scope(...condResult.succeedingStmts, ...trueResult.precedingStmts, ClavaJoinPoints.assign(ClavaJoinPoints.varRef(tempVarDecl), trueResult.$resultExpr), ...trueResult.succeedingStmts);
        const $elseBody = ClavaJoinPoints.scope(...condResult.succeedingStmts, ...falseResult.precedingStmts, ClavaJoinPoints.assign(ClavaJoinPoints.varRef(tempVarDecl), falseResult.$resultExpr), ...falseResult.succeedingStmts);
        const $ifStmt = ClavaJoinPoints.ifStmt(condResult.$resultExpr, $thenBody, $elseBody);
        const precedingStmts = [
            tempVarDecl.stmt,
            ...condResult.precedingStmts,
            $ifStmt,
        ];
        return new DecomposeResult(precedingStmts, ClavaJoinPoints.varRef(tempVarDecl));
    }
    decomposeUnaryOp($unaryOp) {
        const kind = $unaryOp.kind;
        // only decompose increment / decrement operations, separating the change
        // from the result of the change
        if (kind !== "post_dec" &&
            kind !== "post_inc" &&
            kind !== "pre_dec" &&
            kind !== "pre_inc") {
            return new DecomposeResult([], $unaryOp, []);
        }
        switch (kind) {
            case "post_dec":
            case "post_inc": {
                const $innerExpr = $unaryOp.operand.copy();
                const succeedingStmts = [ClavaJoinPoints.exprStmt($unaryOp)];
                return new DecomposeResult([], $innerExpr, succeedingStmts);
            }
            case "pre_dec":
            case "pre_inc": {
                const $innerExpr = $unaryOp.operand.copy();
                const precedingStmts = [ClavaJoinPoints.exprStmt($unaryOp)];
                return new DecomposeResult(precedingStmts, $innerExpr, []);
            }
        }
    }
}
//# sourceMappingURL=StatementDecomposer.js.map
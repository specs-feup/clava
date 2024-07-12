import Pass from "lara-js/api/lara/pass/Pass.js";
import PassResult from "lara-js/api/lara/pass/results/PassResult.js";
import Query from "lara-js/api/weaver/Query.js";
import { BuiltinType, FunctionJp, ReturnStmt, } from "../../Joinpoints.js";
import ClavaJoinPoints from "../ClavaJoinPoints.js";
import DecomposeVarDeclarations from "./DecomposeVarDeclarations.js";
export default class SingleReturnFunction extends Pass {
    _name = "SingleReturnFunctions";
    useLocalLabel;
    constructor(useLocalLabel = false) {
        super();
        this.useLocalLabel = useLocalLabel;
    }
    _apply_impl($jp) {
        if (!($jp instanceof FunctionJp) || !$jp.isImplementation) {
            return this.new_result($jp, false);
        }
        const $body = $jp.body;
        const $returnStmts = Query.searchFrom($body, ReturnStmt).get();
        if ($returnStmts.length === 0 ||
            ($returnStmts.length === 1 && $body.lastChild instanceof ReturnStmt)) {
            return this.new_result($jp, false);
        }
        // C++ spec has some restrictions about jumping over initialized values that
        // would be invalidated by the generated code, so we need to decompose variable
        // declarations first
        new DecomposeVarDeclarations().apply($body);
        const $label = ClavaJoinPoints.labelDecl("__return_label");
        $body.insertEnd(ClavaJoinPoints.labelStmt($label));
        const returnType = $jp.returnType;
        const returnIsVoid = returnType instanceof BuiltinType && returnType.builtinKind === "Void";
        let $local = undefined;
        if (returnIsVoid) {
            $body.insertEnd(ClavaJoinPoints.returnStmt());
        }
        else {
            $local = $body.addLocal("__return_value", returnType);
            $body.insertEnd(ClavaJoinPoints.returnStmt(ClavaJoinPoints.varRef($local)));
        }
        for (const $returnStmt of $returnStmts) {
            if (!returnIsVoid) {
                $returnStmt.insertBefore(
                // null safety: $local is initialized whenever return is not void
                ClavaJoinPoints.assign(ClavaJoinPoints.varRef($local), $returnStmt.returnExpr));
            }
            $returnStmt.insertBefore(ClavaJoinPoints.gotoStmt($label));
            $returnStmt.detach();
        }
        // Local label declaration must appear at the beginning of the block
        if (this.useLocalLabel) {
            $body.insertBegin($label);
        }
        return this.new_result($jp, true);
    }
    new_result($jp, appliedPass) {
        return new PassResult(this, $jp, {
            appliedPass: appliedPass,
            insertedLiteralCode: false,
        });
    }
}
//# sourceMappingURL=SingleReturnFunction.js.map
import ClavaJoinPoints from "../ClavaJoinPoints.js";
/**
 * Simplifies assignments of the type `a += b` into the equivalent expression `a = a + b`
 * @param $complexAssignment - The expression to simplify
 */
export default function SimplifyAssignment($complexAssignment) {
    // early return if current node is not suitable for this transform
    if (!ops.has($complexAssignment.operator)) {
        return;
    }
    const $lValue = $complexAssignment.left;
    const $rValue = $complexAssignment.right;
    const $binaryOp = ClavaJoinPoints.binaryOp(ops.get($complexAssignment.operator), $lValue.copy(), $rValue, $complexAssignment.type);
    $complexAssignment.replaceWith(ClavaJoinPoints.assign($lValue, $binaryOp));
}
/**
 * Non-assignment counterparts of complex assignment operators (lookup table)
 */
const ops = new Map([
    ["*=", "*"],
    ["/=", "/"],
    ["%=", "%"],
    ["+=", "+"],
    ["-=", "-"],
    ["<<=", "<<"],
    [">>=", ">>"],
    ["&=", "&"],
    ["^=", "^"],
    ["|=", "|"],
]);
//# sourceMappingURL=SimplifyAssignment.js.map
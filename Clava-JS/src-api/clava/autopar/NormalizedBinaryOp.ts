/**************************************************************
 *
 *                       NormalizedBinaryOp
 *
 **************************************************************/
import Query from "@specs-feup/lara/api/weaver/Query.js";
import { BinaryOp, FileJp, FunctionJp } from "../../Joinpoints.js";

export default function NormalizedBinaryOp() {
    for (const $binaryOp of Query.search(FileJp)
        .search(FunctionJp)
        .search(BinaryOp, { astName: "CompoundAssignOperator" })) {
        let Op = null;
        if ($binaryOp.kind === "add") Op = "+";
        else if ($binaryOp.kind === "sub") Op = "-";
        else if ($binaryOp.kind === "mul") Op = "*";
        else if ($binaryOp.kind === "div") Op = "/";

        if (Op !== null) {
            $binaryOp.insert(
                "replace",
                $binaryOp.left.code +
                    "=" +
                    $binaryOp.left.code +
                    Op +
                    "(" +
                    $binaryOp.right.code +
                    ")"
            );
        }
    }
}

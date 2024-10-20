import Query from "@specs-feup/lara/api/weaver/Query.js";
import { Body, FileJp, FunctionJp, If, Loop } from "../../Joinpoints.js";

/**************************************************************
 *
 *                       RemoveNakedloops
 *
 **************************************************************/
export default function RemoveNakedloops() {
    for (const $body of Query.search(FileJp)
        .search(FunctionJp)
        .search(Loop)
        .search(Body, { naked: true })) {
        $body.setNaked(false);
    }

    for (const $if of Query.search(FileJp)
        .search(FunctionJp)
        .search(Body)
        .search(If)) {
        const $then = $if.then;
        if ($then.naked === true) {
            $then.setNaked(false);
        }
    }

    for (const $if of Query.search(FileJp)
        .search(FunctionJp)
        .search(Body)
        .search(If)) {
        const $else = $if.else;
        if ($else.naked === true) {
            $else.setNaked(false);
        }
    }
}

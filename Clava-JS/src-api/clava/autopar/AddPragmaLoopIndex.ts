/**************************************************************
 *
 *                       AddPragmaLoopIndex
 *
 **************************************************************/

import Query from "@specs-feup/lara/api/weaver/Query.js";
import { FileJp, Loop } from "../../Joinpoints.js";
import GetLoopIndex from "./GetLoopIndex.js";

export default function AddPragmaLoopIndex() {
    for (const $loop of Query.search(FileJp).search(Loop, { kind: "for" })) {
        const loopindex = GetLoopIndex($loop);
        $loop.body.insert("before", "//loopindex " + loopindex);
    }
}

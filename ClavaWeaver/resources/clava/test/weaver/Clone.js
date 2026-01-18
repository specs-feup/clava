import Query from "@specs-feup/lara/api/weaver/Query.js";
import { FunctionJp, FileJp } from "@specs-feup/clava/api/Joinpoints.js";

for (const $function of Query.search(FunctionJp, { hasDefinition: true })) {
    $function.clone("new_" + $function.name);
}

for (const $file of Query.search(FileJp)) {
    console.log($file.code);
}

import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

// Test CUDA kernel call
for (const $call of Query.search("function", "main").search("cudaKernelCall")) {
    console.log("kernel call before: " + $call.code);
    //console.log("Original: " + $call.config.map(node => node.code));
    $call.setConfigFromStrings(["20", "2048"]);
    //console.log("Modified: " + $call.config.map(node => node.code));
    console.log("kernel call after: " + $call.code);
}

// Find CUDA kernel
const $cudaKernel = Query.search("function", { isCudaKernel: true }).first();
console.log("CUDA kernel: " + $cudaKernel.name);

// Print attributes
for (const $attr of $cudaKernel.attrs) {
    console.log("Attr kind: " + $attr.kind);
}

// Add parameter to CUDA kernel
$cudaKernel.addParam("bool inst0");
console.log("After adding param: " + $cudaKernel.getDeclaration(false));

// Add argumento to call
const $cudaKernelCall = Query.search("function", "main")
    .search("cudaKernelCall")
    .first();
$cudaKernelCall.addArg("false", ClavaJoinPoints.typeLiteral("bool"));
console.log("Call after adding arg: " + $cudaKernelCall.code);

// Get properties
for (const $varref of Query.searchFrom($cudaKernel, "varref", {
    hasProperty: true,
})) {
    console.log("Varref: " + $varref.name);
    console.log("Cuda index: " + $varref.property);
}

import HLSAnalysis from "@specs-feup/clava/api/clava/hls/HLSAnalysis.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

const filter = {
    joinPointType: "function",
    qualifiedName: (name) => name.endsWith("_Kernel"),
};
const $fnKernelArray = Query.search("function", filter).get();

for (const $fnKernel of $fnKernelArray) {
    console.log($fnKernel.joinPointType + " --> " + $fnKernel.qualifiedName);
    HLSAnalysis.applyGenericStrategies($fnKernel);
}

console.log(Query.root().code);

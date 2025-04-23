import WeaverLauncher from "@specs-feup/clava/api/weaver/WeaverLauncher.js";

const weaverLauncher = new WeaverLauncher("cxx");
const weaverResult = weaverLauncher.execute("--help");
console.log("result:" + weaverResult);

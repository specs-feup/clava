laraImport("weaver.WeaverLauncher");

const weaverLauncher = new WeaverLauncher("cxx");
const weaverResult = weaverLauncher.execute("--help");
console.log("result:" + weaverResult);

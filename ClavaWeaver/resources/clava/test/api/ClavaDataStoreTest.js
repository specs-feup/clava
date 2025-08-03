import ClavaDataStore from "@specs-feup/clava/api/clava/util/ClavaDataStore.js";
import WeaverOptions from "@specs-feup/lara/api/weaver/WeaverOptions.js";

const dataStore = new ClavaDataStore(WeaverOptions.getData());
console.log("GET:" + dataStore.get("Disable Remote Dependencies"));
console.log("TYPE:" + dataStore.getType("Disable Remote Dependencies"));
dataStore.put("Disable Remote Dependencies", true);
console.log("GET AFTER PUT:" + dataStore.get("Disable Remote Dependencies"));
dataStore.put("Disable Remote Dependencies", false);
console.log("Disable info:" + dataStore.get("disable_info"));
console.log("System includes:" + dataStore.getSystemIncludes());
dataStore.setSystemIncludes("extra_system_include");
console.log("System includes after:" + dataStore.getSystemIncludes());
console.log("Flags:" + dataStore.getFlags());
const flags = dataStore.getFlags() + " -O3";
dataStore.setFlags(flags);
console.log("Flags after:" + dataStore.getFlags());

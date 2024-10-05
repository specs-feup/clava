laraImport("lara.util.DataStore");
laraImport("clava.util.ClavaDataStore");
laraImport("weaver.WeaverOptions");

const dataStore = new ClavaDataStore(WeaverOptions.getData());
console.log("GET:" + dataStore.get("javascript"));
console.log("TYPE:" + dataStore.getType("javascript"));
dataStore.put("javascript", false);
console.log("GET AFTER PUT:" + dataStore.get("javascript"));
dataStore.put("javascript", true);
console.log("Disable info:" + dataStore.get("disable_info"));
console.log("System includes:" + dataStore.getSystemIncludes());
dataStore.setSystemIncludes("extra_system_include");
console.log("System includes after:" + dataStore.getSystemIncludes());
console.log("Flags:" + dataStore.getFlags());
const flags = dataStore.getFlags() + " -O3";
dataStore.setFlags(flags);
console.log("Flags after:" + dataStore.getFlags());

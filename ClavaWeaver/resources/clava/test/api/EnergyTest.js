laraImport("lara.code.Energy");
laraImport("weaver.Query");

// Instrument call
const energy = new Energy();

for (const $call of Query.search("call")) {
    energy.measure($call, "Energy:");
}

for (const $file of Query.search("file")) {
    console.log($file.code);
}

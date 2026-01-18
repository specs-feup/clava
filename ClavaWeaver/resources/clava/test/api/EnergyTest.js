import Energy from "@specs-feup/clava/api/lara/code/Energy.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

// Instrument call
const energy = new Energy();

for (const $call of Query.search("call")) {
    energy.measure($call, "Energy:");
}

for (const $file of Query.search("file")) {
    console.log($file.code);
}

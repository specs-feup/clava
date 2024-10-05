laraImport("weaver.Query");

for (const $cilkFor of Query.search("cilkFor")) {
    console.log("CilkFor: " + $cilkFor.location);
}

for (const $cilkSpawn of Query.search("cilkSpawn")) {
    console.log("CilkSpawn: " + $cilkSpawn.location);
}

for (const $cilkSync of Query.search("cilkSync")) {
    console.log("CilkSync: " + $cilkSync.location);
}

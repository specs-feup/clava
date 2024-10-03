laraImport("weaver.Query");

for (const chain of Query.search("function").search("call").chain()) {
    const $function = chain["function"];
    const $call = chain["call"];

    console.log($function.name + " -> " + $call.name);
    console.log($function.location + " -> " + $call.location);
    const location = $call.decl.location;
    if (!location.includes(".h")) {
        console.log(location);
    }
}

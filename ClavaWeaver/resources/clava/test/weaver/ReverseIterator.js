laraImport("weaver.Query");

const cl = Query.search("class", "json_reverse_iterator").first();
console.log("Bases: " + cl.bases.map((base) => base.name));

laraImport("weaver.Query");

console.log("AClass fields");
for (const $field of Query.search("record", "AClass").search("field")) {
    console.log("Field: " + $field.name);
    console.log("Is public: " + $field.isPublic);
}

console.log("AStruct fields");
for (const $field of Query.search("record", "AStruct").search("field")) {
    console.log("Field: " + $field.name);
    console.log("Is public: " + $field.isPublic);
}

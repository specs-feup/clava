import Query from "@specs-feup/lara/api/weaver/Query.js";
import { Field, RecordJp } from "@specs-feup/clava/api/Joinpoints.js";

console.log("AClass fields");
for (const $field of Query.search(RecordJp, "AClass").search(Field)) {
    console.log("Field: " + $field.name);
    console.log("Is public: " + $field.isPublic);
}

console.log("AStruct fields");
for (const $field of Query.search(RecordJp, "AStruct").search(Field)) {
    console.log("Field: " + $field.name);
    console.log("Is public: " + $field.isPublic);
}

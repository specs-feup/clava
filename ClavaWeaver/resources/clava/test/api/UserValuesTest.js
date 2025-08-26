import Clava from "@specs-feup/clava/api/clava/Clava.js";
import { printObject } from "@specs-feup/lara/api/core/output.js";

let $program = Clava.getProgram();

$program.setUserField("test", "test string");
console.log("User field: " + $program.getUserField("test"));

let anArray = ["Hello", "World"];
$program.setUserField("anArray", anArray);

const aMap = { field1: "field1_value", field2: 2 };
$program.setUserField("aMap", JSON.stringify(aMap));

Clava.pushAst();

$program = Clava.getProgram();
console.log("User field after push: " + $program.getUserField("test"));
console.log("Array after push:");
printObject($program.getUserField("anArray"));
console.log("\nMap after push:");
printObject(JSON.parse($program.getUserField("aMap")));
console.log();
// Changes array
anArray = $program.getUserField("anArray");
anArray.push("pushed");
$program.setUserField("anArray", anArray);

Clava.popAst();

$program = Clava.getProgram();
console.log("Array after pop:");
printObject($program.getUserField("anArray"));

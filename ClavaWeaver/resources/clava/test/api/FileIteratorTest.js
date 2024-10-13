import FileIterator from "@specs-feup/clava/api/clava/util/FileIterator.js";
import Io from "@specs-feup/lara/api/lara/Io.js";
import Clava from "@specs-feup/clava/api/clava/Clava.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

// Write source files to a temporary folder
const tempFolder = Io.getTempFolder("ClavaFileIteratorTest");
Clava.writeCode(tempFolder);

// Iterate method 1
const fileIterator = new FileIterator(tempFolder);

let $file = fileIterator.next();
while ($file !== undefined) {
    console.log("Iterator 1");
    $file = fileIterator.next();
}

// Iterate method 2
const fileIterator2 = new FileIterator(tempFolder);

while (fileIterator2.hasNext()) {
    $file = fileIterator2.next();
    console.log("Iterator 2");
}

// Generic select after iterator
const fileIterator3 = new FileIterator(tempFolder);
fileIterator3.next();

for (const _ of Query.search("file")) {
    console.log("Single file");
}
Io.deleteFolder(tempFolder);

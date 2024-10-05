laraImport("weaver.Query");

for (const $function of Query.search("function")) {
    const fName = $function.name;
    if (fName != "Max" && fName != "MaxNoInline") {
        continue;
    }

    console.log($function.name + " is inline: " + $function.isInline);
}

// Set name
for (const $function of Query.search("file", "function.cpp").search(
    "function"
)) {
    const fName = $function.name;
    if (
        fName != "defOnly" &&
        fName != "declOnly" &&
        fName != "declAndDef" &&
        fName != "notCalled"
    ) {
        continue;
    }

    //console.log("FILE BEFORE SET:\n" + $file.code);
    const newName = fName + "New";
    $function.name = newName;
    //console.log("FILE AFTER SET:\n" + $file.code);

    console.log("Function new name: " + $function.name);
}

for (const $function of Query.search("function", "caller")) {
    console.log("Caller body: " + $function.body.code);
}

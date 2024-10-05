laraImport("weaver.Query");

for (const $function of Query.search("function", { hasDefinition: true })) {
    $function.clone("new_" + $function.name);
}

for (const $file of Query.search("file")) {
    console.log($file.code);
}

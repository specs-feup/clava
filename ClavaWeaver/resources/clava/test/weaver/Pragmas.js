laraImport("weaver.Query");

for (const $marker of Query.search("marker")) {
    const $scope = $marker.contents;
    $scope.insertBefore(`// Before scope - ${$marker.id}`);
    $scope.insertAfter(`// After scope - ${$marker.id}`);

    $scope.insertBegin("// Scope start - " + $marker.id);
    $scope.insertEnd("// Scope end - " + $marker.id);
}

for (const $file of Query.search("file")) {
    console.log($file.code);
}

for (const $marker of Query.search("marker", "foo")) {
    console.log('default marker attribute "id" is working: ' + $marker.id);
    console.log("marker contents: " + $marker.contents.code);
}

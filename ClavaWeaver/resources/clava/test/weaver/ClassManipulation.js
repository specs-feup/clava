laraImport("clava.ClavaJoinPoints");
laraImport("clava.Clava");
laraImport("weaver.Query");

function testCopyMethodDeclaration() {
    const $class = Query.search("class", "originalClass").first();

    const $method = Query.searchFrom($class, "method", "foo").first();

    const $newMethod = $method.clone("new_" + $method.name, false);

    const $newClass = ClavaJoinPoints.classDecl("newClass");
    $newClass.addMethod($newMethod);

    console.log("Test Copy Method Declaration");
    console.log("CLASS: " + $class.code);
    console.log("NEW CLASS: " + $newClass.code);
}

function testCopyMethodDefinition() {
    const $class = Query.search("class", "originalClass2").first();

    const $method = Query.search("method", {
        name: "foo2",
        hasDefinition: true,
    }).first();

    const $newMethod = $method.clone("new_" + $method.name, false);

    const $newClass = ClavaJoinPoints.classDecl("newClass2");
    $newClass.addMethod($newMethod);

    console.log("Test Copy Method Definition");
    console.log("CLASS: " + $class.code);
    console.log("METHOD: " + $method.code);
    console.log("NEW CLASS: " + $newClass.code);
    console.log("NEW METHOD: " + $newMethod.code);

    // If $newMethod.declarationJp is called before adding $newClass to the program,
    // it will returned undefined due to Clava caching queries to declarationJp/definitionJp
    console.log("METHOD DECL BEFORE ADDING CLASS: " + $newMethod.declarationJp);
    $class.getAncestor("file").insertAfter($newClass);
    console.log("METHOD DECL AFTER ADDING CLASS: " + $newMethod.declarationJp);
}

function testCopyTypedef() {
    // This should trigger a case where a copy() override in TypeDecl needs to handle a case with a typedef
    const $typedefCopy = Query.search("typedefDecl").first().deepCopy();
    console.log("Typedef: " + $typedefCopy.code);
}

function testAddField() {
    const $class = Query.search("class", "originalClass4").first();
    const $newField = ClavaJoinPoints.field(
        "b",
        ClavaJoinPoints.builtinType("double")
    );
    $class.addField($newField);
    console.log("CLASS WITH NEW FIELD: " + $class.code);
}

function testBases() {
    const pig = Query.search("class", "Pig").first();
    console.log("Pig bases: " + pig.bases[0].name);
}

testCopyMethodDeclaration();
testCopyMethodDefinition();
testCopyTypedef();
testAddField();
testBases();

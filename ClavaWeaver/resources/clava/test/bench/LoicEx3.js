laraImport("lara.Io");
laraImport("clava.Clava");
laraImport("clava.ClavaJoinPoints");
laraImport("weaver.Query");

function duplicate($vclass, idNewClass) {
    const $newClass = ClavaJoinPoints.classDecl(idNewClass);
    $vclass.insertAfter($newClass);
    for (let i = 0; i < $vclass.astNumChildren; i++) {
        const $vtchild = $vclass.getChild(i);
        const astName = $vtchild.astName;
        switch (astName) {
            case "CXXMethodDecl":
            case "CXXDestructorDecl":
            case "CXXConstructorDecl":
                duplicateMethod($vtchild, $newClass);
                break;
            case "FieldDecl": {
                const $nf = ClavaJoinPoints.field($vtchild.name, $vtchild.type);
                $newClass.addField($nf);
                break;
            }
            default: {
                //  AccessSpecDecl , InlineComment
                const $vcopy = $vtchild.deepCopy();
                if ($newClass.astNumChildren == 0) {
                    $newClass.setFirstChild($vcopy);
                } else {
                    $newClass.lastChild.insertAfter($vcopy);
                }
            }
        }
    }
    return $newClass;
}

function duplicateMethod($m, $newClass) {
    function setNameConstructorDestructorMethod($nm) {
        if ($nm.astIsInstance("CXXConstructorDecl"))
            $nm.setName($newClass.name);
        else if ($nm.astIsInstance("CXXDestructorDecl"))
            $nm.setName("~" + $newClass.name);
    }

    if (!$m.hasDefinition) {
        $m = $m.definitionJp;
    }
    const $newMethod = $m.clone($m.name, false);
    setNameConstructorDestructorMethod($newMethod);
    $newClass.addMethod($newMethod);
    $newClass.insertAfter($newMethod);
}

function printLinksDeclarationDefinition($Class) {
    const methods = $Class.methods;
    // console.log( " class = " + $Class.name);
    for (const $m of methods) {
        console.log(" ==================================");
        console.log(
            " Declaration of the method = " +
                $m.code +
                " of the class " +
                $Class.name
        );
        if ($m.definitionJp !== undefined) {
            const $vdef = $m.definitionJp;
            console.log(" Associated definition = " + $vdef.code);
            console.log(
                " The class of the record field is = " + $vdef.record.name
            );
        }
    }
}

const $vclass = Query.search("class").first();
const $newClass1 = duplicate($vclass, "FIRST_DUPLICATION");
const $newClass2 = duplicate($vclass, "SECOND_DUPLICATION");
printLinksDeclarationDefinition($newClass1);
printLinksDeclarationDefinition($newClass2);

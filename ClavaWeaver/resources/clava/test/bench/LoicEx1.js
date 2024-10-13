import Io from "@specs-feup/lara/api/lara/Io.js";
import Clava from "@specs-feup/clava/api/clava/Clava.js";
import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

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
    function setNameMethod($nm) {
        if ($nm.astIsInstance("CXXConstructorDecl"))
            $nm.setName($newClass.name);
        else if ($nm.astIsInstance("CXXDestructorDecl"))
            $nm.setName("~" + $newClass.name);
    }

    if (!$m.hasDefinition) {
        $m = $m.definitionJp;
    }

    // Set new name, of if name is the same, automatically removes method from the class
    const $newMethod = $m.clone($m.name, false);
    
    setNameMethod($newMethod);
    $newClass.addMethod($newMethod);
    $newClass.insertAfter($newMethod);
}

function printLinksDeclarationDefinition($Class) {
    const methods = $Class.methods;
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
            if ($vdef.record !== undefined) {
                console.log(
                    " The class of the record field is = " + $vdef.record.name
                );
            } else {
                console.log(
                    " The class of the record field is = " + $vdef.record
                );
            }
        }
    }
}

const $vclass = Query.search("class").first();
const $newClass1 = duplicate($vclass, "FIRST_DUPLICATION");
const $newClass2 = duplicate($vclass, "SECOND_DUPLICATION");
printLinksDeclarationDefinition($newClass1);
printLinksDeclarationDefinition($newClass2);
printLinksDeclarationDefinition($vclass);

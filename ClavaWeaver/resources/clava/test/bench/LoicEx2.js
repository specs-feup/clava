import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";

function replaceTypesTemplatesInCall($function) {
    let cpt = 0;
    const vcalls = $function.calls;
    for (const aCall of vcalls) {
        const $vargTemplate = undefined;
        try {
            const $fc = aCall.children[0];
            console.log(" === The first child of the call is : " + $fc.code);
            const targs = $fc.getValue("templateArguments");
            for (const x of targs) {
                let $vargTemplate = undefined;
                try {
                    $vargTemplate = x.getValue("expr");
                    console.log(" *** expression detected = " + x);
                } catch (e) {}

                if ($vargTemplate === undefined)
                    try {
                        $vargTemplate = x.getValue("type");
                        console.log(" *** type  detected  = " + x);
                        try {
                            x.setValue(
                                "type",
                                ClavaJoinPoints.typeLiteral("newType" + cpt)
                            );
                            cpt++;
                            console.log(
                                " === AFTER MY TRANSFO, The first child of the call is : " +
                                    $fc.code
                            );
                        } catch (e) {
                            console.log(e);
                        }
                    } catch (e) {}
            }
        } catch (e) {}
    }
}

for (const $function of Query.search("function").get()) {
    replaceTypesTemplatesInCall($function);
}

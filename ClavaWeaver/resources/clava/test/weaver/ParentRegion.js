laraImport("weaver.Query");

for (const $vardecl of Query.search("vardecl")) {
    printMessage($vardecl);
}

for (const $varref of Query.search("varref")) {
    printMessage($varref);
}

for (const $function of Query.search("function")) {
    printMessage($function);
}

function printMessage($jp) {
    const $currentRegion = $jp.currentRegion;
    const $parentRegion = $jp.parentRegion;

    const initMessage =
        $jp.joinPointType +
        " '" +
        $jp.name +
        "' is in region '" +
        $currentRegion.joinPointType +
        "' at line " +
        $jp.line;

    if ($parentRegion === undefined) {
        console.log(initMessage + " and does not have a parentRegion");
        return;
    }

    console.log(
        initMessage +
            ", parentRegion is a '" +
            $parentRegion.joinPointType +
            "' at line " +
            $parentRegion.line
    );
}

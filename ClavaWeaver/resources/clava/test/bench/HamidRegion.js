//import Query from "@specs-feup/lara/api/weaver/Query";
laraImport("weaver.Query");

for (const chain of Query.search("file", "hamid_region.c")
    .search("function")
    .search("body")
    .search("arrayAccess")
    .chain()) {
    const $function = chain["function"];
    const $arrayAccess = chain["arrayAccess"];

    console.log("function.name : " + $function.name);
    console.log("\t" + "arrayAccess.code : " + $arrayAccess.code);
    console.log(
        "\t" + "arrayAccess.arrayVar.code : " + $arrayAccess.arrayVar.code
    );

    // 1
    console.log(
        "\t" +
            "vardecl.code : " +
            $arrayAccess.arrayVar.getDescendantsAndSelf("varref")[0].vardecl
                .code
    );
    console.log(
        "\t" +
            "vardecl.currentRegion : " +
            $arrayAccess.arrayVar.getDescendantsAndSelf("varref")[0].vardecl
                .currentRegion
    );

    // 2
    console.log(
        "\t" +
            "arrayAccess.arrayVar.vardecl.code : " +
            $arrayAccess.arrayVar.vardecl.code
    );
    console.log(
        "\t" +
            "arrayAccess.arrayVar.vardecl.currentRegion : " +
            $arrayAccess.arrayVar.vardecl.currentRegion
    );
}

for (const $vardecl of Query.search("file", "hamid_region.h").search(
    "vardecl"
)) {
    console.log("header vardecl.currentRegion : " + $vardecl.currentRegion);
}

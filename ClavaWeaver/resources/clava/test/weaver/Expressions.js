import Query from "@specs-feup/lara/api/weaver/Query.js";

// MemberAccess chains
for (const $memberAccess of Query.search("function", "analyse").search(
    "memberAccess"
)) {
    console.log(
        "MemberChain Types:" +
            $memberAccess.memberChain.map((m) => m.joinPointType)
    );
    console.log("MemberChain Names:" + $memberAccess.memberChainNames);
}

// Obtain corresponding declaration for each varref

for (const $varref of Query.search("function", "analyse").search(
    "varref"
)) {
    const vardecl = $varref.vardecl;
    console.log("varref:" + $varref.name);
    console.log("has decl:" + (vardecl != undefined));
}

for (const $newExpr of Query.search("newExpr")) {
    console.log("NewExpr: " + $newExpr.code);
}

for (const $deleteExpr of Query.search("deleteExpr")) {
    console.log("DeleteExpr: " + $deleteExpr.code);
}

import Query from "@specs-feup/lara/api/weaver/Query.js";

/**
 * Declares a variable globally.
 *
 * @param {string} varName - the name of the variable
 *
 * @private
 * @aspect
 * */
export default function ExamonCollectorDeclareGlobal(varName) {
    var type = ClavaJoinPoints.typeLiteral("struct collector_val");
    var init = "{ NULL, NULL, false, 0, 0, 0, {0}, {0} }";

    for (const $file of Query.search("file")) {
        $file.addGlobal(varName, type, init);
    }
}

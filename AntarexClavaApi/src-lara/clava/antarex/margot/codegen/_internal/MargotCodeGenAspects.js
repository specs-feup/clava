import Query from "@specs-feup/lara/api/weaver/Query.js";

/**
 * Calls the initialization function of mARGOt at the start of the main function.
 *
 * @param {MargotStringsGen} gen - the code generation instance
 * @aspect
 * */
export default function MargotInitMain(gen) {
    for (const chain of Query.search("file").search("function").chain()) {
        const $body = chain["function"].body;
        const $file = chain["file"];

        $body.insertBegin(gen.init());
        $file.addInclude(gen.inc(), true);
    }
}

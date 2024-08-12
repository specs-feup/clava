import EdgeData from "lara-js/api/lara/graphs/EdgeData.js";
export default class ScgEdgeData extends EdgeData {
    /**
     * The calls that contributed to this edge
     */
    $calls = [];
    get calls() {
        return this.$calls;
    }
    inc($call) {
        this.$calls.push($call);
    }
    toString() {
        return this.$calls.length.toString();
    }
}
//# sourceMappingURL=ScgEdgeData.js.map
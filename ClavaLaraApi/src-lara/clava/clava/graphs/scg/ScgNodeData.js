import NodeData from "@specs-feup/lara/api/lara/graphs/NodeData.js";
import Query from "@specs-feup/lara/api/weaver/Query.js";
import { Call } from "../../../Joinpoints.js";
export default class ScgNodeData extends NodeData {
    /**
     * The function represented by this node
     */
    $function;
    constructor($function) {
        super();
        // Should use only canonical functions
        this.$function = $function.canonical;
    }
    get function() {
        return this.$function;
    }
    toString() {
        return this.$function.signature;
    }
    /**
     * @returns true, if the function represented by this node has an available implementation, false otherwise
     */
    hasImplementation() {
        return this.$function.isImplementation;
    }
    hasCalls() {
        return Query.searchFrom(this.$function, Call).get().length > 0;
    }
}
//# sourceMappingURL=ScgNodeData.js.map
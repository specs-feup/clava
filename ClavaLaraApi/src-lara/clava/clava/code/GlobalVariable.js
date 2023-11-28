import ClavaJoinPoints from "../ClavaJoinPoints.js";
/**
 * Adds and manages global variables.
 */
export default class GlobalVariable {
    filesWithGlobal = new Set();
    varName;
    $type;
    initValue;
    constructor(varName, $type, initValue) {
        this.varName = varName;
        this.$type = $type;
        this.initValue = initValue;
    }
    /**
     * @returns A reference to the global variable defined by this object.
     */
    getRef($reference) {
        // Check file for the reference point
        const $file = $reference.getAncestor("file");
        if ($file === undefined) {
            console.log(`GlobalVariable.getRef: Could not find the file for the reference point ${$reference.location}`);
        }
        else {
            // Check if file already has this global variable declared
            const fileId = $file.jpId;
            if (!this.filesWithGlobal.has(fileId)) {
                this.filesWithGlobal.add(fileId);
                $file.addGlobal(this.varName, this.$type, this.initValue);
            }
        }
        // Create varref
        return ClavaJoinPoints.varRef(this.varName, this.$type);
    }
}
//# sourceMappingURL=GlobalVariable.js.map
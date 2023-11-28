import MpiAccessPattern from '../MpiAccessPattern.js';
/**
 * Access to a scalar variable.
 */
export default class ScalarPattern extends MpiAccessPattern {
    sendMaster($varJp, totalIterations) {
        throw "Not yet implemented";
    }
    receiveMaster($varJp, totalIterations) {
        throw "Not yet implemented";
    }
    sendWorker($varJp, totalIterations) {
        throw "Not yet implemented";
    }
    receiveWorker($varJp, totalIterations) {
        throw "Not yet implemented";
    }
    outputDeclWorker($varJp, totalIterations) {
        throw "Not yet implemented";
    }
}
//# sourceMappingURL=ScalarPattern.js.map
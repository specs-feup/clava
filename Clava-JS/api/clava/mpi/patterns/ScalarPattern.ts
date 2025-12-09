import { Varref } from '../../../Joinpoints.js';
import MpiAccessPattern from '../MpiAccessPattern.js';

/**
 * Access to a scalar variable.
 */
export default class ScalarPattern extends MpiAccessPattern {
  sendMaster($varJp: Varref, totalIterations: string): string {
    throw "Not yet implemented";
  }

  receiveMaster($varJp: Varref, totalIterations: string): string {
    throw "Not yet implemented";
  }

  sendWorker($varJp: Varref, totalIterations: string): string {
    throw "Not yet implemented";
  }

  receiveWorker($varJp: Varref, totalIterations: string): string {
    throw "Not yet implemented";
  }

  outputDeclWorker($varJp: Varref, totalIterations: string): string {
    throw "Not yet implemented";
  }
}

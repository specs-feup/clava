import { Varref } from "../../Joinpoints.js";

/**
 * Represents an MPI access pattern.
 *
 */
export default abstract class MpiAccessPattern {
  abstract sendMaster($varJp: Varref, totalIterations: string): string;

  abstract receiveMaster($varJp: Varref, totalIterations: string): string;

  abstract sendWorker($varJp: Varref, totalIterations: string): string;

  abstract receiveWorker($varJp: Varref, totalIterations: string): string;

  abstract outputDeclWorker($varJp: Varref, totalIterations: string): string;
}

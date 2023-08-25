import EnergyBase from "lara-js/api/lara/code/EnergyBase.js";

import Clava from "../../clava/Clava.js";
import Logger from "./Logger.js";

import IdGenerator from "lara-js/api/lara/util/IdGenerator.js";
import PrintOnce from "lara-js/api/lara/util/PrintOnce.js";
import { FileJp, Joinpoint } from "../../Joinpoints.js";

export default class Energy extends EnergyBase<Joinpoint> {
  /**
   * Current SpecsRapl library measures uJ.
   */
  getPrintUnit() {
    return "uJ";
  }

  measure($start: Joinpoint, prefix: string = "", $end: Joinpoint = $start) {
    //Check for valid joinpoints and additional conditions

    if (!this.measureValidate($start, $end, "function")) {
      return;
    }

    // Message about dependency
    PrintOnce.message(
      "Woven code has dependency to project SpecsRapl, which can be found at https://github.com/specs-feup/specs-c-libs"
    );

    // Adds SpecsRapl include
    Clava.getProgram().addExtraIncludeFromGit(
      "https://github.com/specs-feup/specs-c-libs.git",
      "SpecsRapl/include/"
    );

    // Adds SpecsRapl source
    Clava.getProgram().addExtraSourceFromGit(
      "https://github.com/specs-feup/specs-c-libs.git",
      "SpecsRapl/src/"
    );

    Clava.getProgram().addExtraLib("-pthread");

    const logger = new Logger(false, this.filename);

    // Add include
    const $file = $start.getAncestor("file") as FileJp | undefined;
    if ($file === undefined) {
      console.log(
        "Could not find the corresponding file of the given joinpoint: " + $start.joinPointType
      );
      return;
    }
    $file.addInclude("rapl.h", false);
    const energyVar = IdGenerator.next("clava_energy_output_");
    const energyVarStart = energyVar + "_start";
    const energyVarEnd = energyVar + "_end";

    const codeBefore = Energy.energy_rapl_measure(energyVarStart);
    const codeAfter = Energy.energy_rapl_measure(energyVarEnd);
    $start.insert("before", codeBefore);

    logger.append(prefix).appendLongLong(energyVarEnd + " - " + energyVarStart);
    if (this.printUnit) {
      logger.append(this.getPrintUnit());
    }
    logger.ln();
    logger.log($end);
    $end.insert("after", codeAfter);
  }

  private static energy_rapl_measure(energyVar: string): string {
    return `long long ${energyVar} = rapl_energy();`;
  }
}

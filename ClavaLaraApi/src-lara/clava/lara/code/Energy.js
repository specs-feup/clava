laraImport("clava.Clava");

laraImport("lara.Compilation");
laraImport("lara.code.EnergyBase");
laraImport("lara.code.Logger");
laraImport("lara.util.IdGenerator");
laraImport("lara.util.PrintOnce");

/**
 * Current SpecsRapl library measures uJ.
 */
Energy.prototype.getPrintUnit = function () {
  return "uJ";
};

Energy.prototype.measure = function ($start, prefix = "", $end = $start) {
  const $actualEnd = $end ?? $start;
  const actualPrefix = prefix ?? "";

  //Check for valid joinpoints and additional conditions
  if (!this._measureValidate($start, $end, "function")) {
    return;
  }

  // Message about dependency
  PrintOnce.message(
    "Woven code has dependency to project SpecsRapl, which can be found at https://github.com/specs-feup/specs-c-libs"
  );

  // Adds SpecsRapl include
  Compilation.addExtraIncludeFromGit(
    "https://github.com/specs-feup/specs-c-libs.git",
    "SpecsRapl/include/"
  );
  //Clava.getProgram().addExtraIncludeFromGit("https://github.com/specs-feup/specs-c-libs.git", "SpecsRapl/include/");

  // Adds SpecsRapl source
  Compilation.addExtraSourceFromGit(
    "https://github.com/specs-feup/specs-c-libs.git",
    "SpecsRapl/src/"
  );
  //Clava.getProgram().addExtraSourceFromGit("https://github.com/specs-feup/specs-c-libs.git", "SpecsRapl/src/");

  Compilation.addExtraLib("-pthread");

  const logger = new Logger(false, this.filename);

  // Add include
  const $file = $start.ancestor("file");
  if ($file === undefined) {
    println(
      `Could not find the corresponding file of the given joinpoint: ${$jp}`
    );
    return;
  }
  $file.addInclude("rapl.h", false);
  const energyVar = IdGenerator.next("clava_energy_output_");
  const energyVarStart = `${energyVar}_start`;
  const energyVarEnd = `${energyVar}_end`;

  const codeBefore = _energy_rapl_measure(energyVarStart);
  const codeAfter = _energy_rapl_measure(energyVarEnd);
  $start.insert("before", codeBefore);

  logger
    .append(actualPrefix)
    .appendLongLong(`${energyVarEnd} - ${energyVarStart}`);
  if (this.printUnit) {
    logger.append(this.getPrintUnit());
  }
  logger.ln();
  logger.log($actualEnd);
  $actualEnd.insert("after", codeAfter);
};

function _energy_rapl_measure(energyVar) {
  return `long long ${energyVar} = rapl_energy();`;
}

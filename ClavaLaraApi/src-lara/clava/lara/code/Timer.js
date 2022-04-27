laraImport("lara.code.TimerBase");
laraImport("lara.code.Logger");

laraImport("lara.util.IdGenerator");
laraImport("lara.Platforms");
laraImport("lara.util.TimeUnits");

laraImport("clava.ClavaJoinPoints");

laraImport("lara.util.StringSet");

Timer.prototype.addedDefines = new StringSet();

/**
 * Times the code of a given section.
 *
 * @param {$joinpoint} $start Starting point of the time measure
 * @param {string} [prefix] Message that will appear before the time measure. If undefined, empty string will be used.
 * @param {$joinpoint} [$end] Ending point of the time measure. If undefined, measure is done around starting point.
 */
Timer.prototype.time = function ($start, prefix = "", $end = $start) {
  const actualPrefix = prefix ?? "";
  const $actualend = $end ?? $start;
  if (!this._timeValidate($start, $enactualendd, "function")) {
    return;
  }

  const $file = $start.ancestor("file");

  if ($file === undefined) {
    println(
      `Could not find the corresponding file of the given joinpoint: ${$jp}`
    );
    return;
  } else if ($file.isCxx) {
    return this._time_cpp($start, actualPrefix, $actualend);
  } else {
    return this._time_c($start, actualPrefix, $actualend);
  }

  //return this;
  //PrintOnce.message("Timer.time: not implemented yet for C");
};

Timer.prototype._time_cpp = function ($start, prefix, $end) {
  const actualPrefix = prefix ?? "";
  const $actualend = $end ?? $start;

  if (this.timeUnits.unit == this.timeUnits._timerUnit.DAYS) {
    throw "Timer Exception: Timer metrics not implemented for DAYS in C++";
  }
  const logger = new Logger(false, this.filename);

  const $file = $start.ancestor("file");

  // Add include
  $file.addInclude("chrono", true);

  const startVar = IdGenerator.next("clava_timing_start_");
  const endVar = IdGenerator.next("clava_timing_end_");

  const codeTic = _timer_cpp_now(startVar);
  const codeToc = _timer_cpp_now(endVar);

  const cppUnit = this.timeUnits.getCppTimeUnit();

  // Create literal node with calculation of time interval
  const $timingResult = ClavaJoinPoints.exprLiteral(
    _timer_cpp_calc_interval(startVar, endVar, cppUnit)
  );

  // Declare variable for time interval, which uses calculation as initialization
  const timeIntervalVar = IdGenerator.next("clava_timing_duration_");
  const $timingResultDecl = ClavaJoinPoints.varDecl(
    timeIntervalVar,
    $timingResult
  );

  // Build message
  logger.append(actualPrefix).appendDouble(timeIntervalVar);
  if (this.printUnit) {
    logger.append(this.timeUnits.getUnitsString());
  }
  logger.ln();

  // Check if $start is a scope
  if ($start.instanceOf("scope")) {
    // Insert code
    $start.insertBegin(codeTic);
  } else {
    // Insert code
    $start.insertBefore(codeTic);
  }

  let afterJp = undefined;

  // Check if $end is a scope
  if ($actualend.instanceOf("scope")) {
    // 'insertEnd' insertions must be done in sequential order
    $actualend.insertEnd(codeToc);
    afterJp = $actualend.insertEnd($timingResultDecl);
  } else {
    // 'insertAfter' insertions must be done in reverse order
    afterJp = $actualend.insertAfter($timingResultDecl);
    $actualend.insertAfter(codeToc);
  }

  // // If $start/$end parent do not have a statement ancestor, use insertBegin/End instead
  // if (
  //   $start.ancestor("statement") === undefined ||
  //   $end.ancestor("statement") === undefined
  // ) {
  //   // Insert code
  //   $start.insertBegin(codeTic);

  //   // 'end' insertions must be done in sequential order
  //   $actualend.insertEnd(codeToc);
  //   $actualend.insertEnd($timingResultDecl);
  // } else {
  //   // Insert code
  //   $start.insertBefore(codeTic);

  //   // 'after' insertions must be done in reverse order
  //   $actualend.insertAfter($timingResultDecl);
  //   $actualend.insertAfter(codeToc);
  // }

  // Log time information
  if (this.print) {
    logger.log($timingResultDecl);
    afterJp = logger.getAfterJp();
  }

  this._setAfterJp(afterJp);

  return timeIntervalVar;
};

Timer.prototype._time_c = function ($start, prefix, $end) {
  const actualPrefix = prefix ?? "";
  const $actualend = $end ?? $start;

  const logger = new Logger(false, this.filename);

  $file = $start.ancestor("file");

  // Declare variable for time interval, which uses calculation as initialization
  const timeIntervalVar = IdGenerator.next("clava_timing_duration_");

  // code fragments depend on the platform
  let codeBefore, codeAfter;
  let $timingResultDecl;

  if (Platforms.isWindows()) {
    //use QueryPerformanceCounter
    // Add includes
    $file.addInclude("time.h", true);
    $file.addInclude("windows.h", true);

    // get variable names
    const startVar = IdGenerator.next("clava_timing_start_");
    const endVar = IdGenerator.next("clava_timing_end_");
    const frequencyVar = IdGenerator.next("clava_timing_frequency_");

    codeBefore = _timer_c_windows_declare_vars_now(
      startVar,
      endVar,
      frequencyVar
    );
    codeAfter = _timer_c_windows_get_final_time(endVar);

    // Create literal node with calculation of time interval
    const $timingResult = ClavaJoinPoints.exprLiteral(
      _timer_c_windows_calc_interval(
        startVar,
        endVar,
        frequencyVar,
        this.timeUnits.getMagnitudeFactorFromSeconds()
      ),
      ClavaJoinPoints.builtinType("double")
    );

    $timingResultDecl = ClavaJoinPoints.varDecl(timeIntervalVar, $timingResult);
  } else if (Platforms.isLinux()) {
    // Add includes
    $file.exec.addInclude("time.h", true);

    // If C99 or C11 standard, needs define at the beginning of the file
    // https://stackoverflow.com/questions/42597685/storage-size-of-timespec-isnt-known
    const needsDefine =
      Clava.getStandard() === "c99" || Clava.getStandard() === "c11";
    if (needsDefine && !this.addedDefines.has($file.location)) {
      $file.exec.insertBegin("#define _POSIX_C_SOURCE 199309L");
      this.addedDefines.add($file.location);
    }

    // get variable names
    const startVar = IdGenerator.next("clava_timing_start_");
    const endVar = IdGenerator.next("clava_timing_end_");

    codeBefore = _timer_c_linux_declare_vars_now(startVar, endVar);
    codeAfter = _timer_c_linux_get_final_time(endVar);

    // Create literal node with calculation of time interval
    const $timingResult = ClavaJoinPoints.exprLiteral(
      _timer_c_linux_calc_interval(
        startVar,
        endVar,
        this.timeUnits.getMagnitudeFactorFromSeconds()
      ),
      ClavaJoinPoints.builtinType("double")
    );

    $timingResultDecl = ClavaJoinPoints.varDecl(timeIntervalVar, $timingResult);
  } else {
    throw "Timer Exception: Platform not supported (Windows and Linux only)";
  }

  // Build message
  logger.append(actualPrefix).appendDouble(timeIntervalVar);
  if (this.printUnit) {
    logger.append(this.timeUnits.getUnitsString());
  }
  logger.ln();

  // Check if $start is a scope
  if ($start.instanceOf("scope")) {
    // Insert code
    $start.insertBegin(codeBefore);
  } else {
    // Insert code
    $start.insertBefore(codeBefore);
  }

  let afterJp = undefined;
  // Check if $end is a scope
  if ($actualend.instanceOf("scope")) {
    // 'insertEnd' insertions must be done in sequential order
    $actualend.insertEnd(codeAfter);
    afterJp = $actualend.insertEnd($timingResultDecl);
  } else {
    // 'insertAfter' insertions must be done in reverse order
    afterJp = $actualend.insertAfter($timingResultDecl);
    $actualend.insertAfter(codeAfter);
  }

  // // If $start/$end parent do not have a statement ancestor, use insertBegin/End instead
  // if (
  //   $start.ancestor("statement") === undefined ||
  //   $actualend.ancestor("statement") === undefined
  // ) {
  //   // Insert code
  //   $start.insertBegin(codeBefore);

  //   // 'end' insertions must be done in sequential order
  //   $actualend.insertEnd(codeAfter);
  //   $actualend.insertEnd($timingResultDecl);
  // } else {
  //   // Insert code
  //   $start.insertBefore(codeBefore);

  //   // 'after' insertions must be done in reverse order
  //   $actualend.insertAfter($timingResultDecl);
  //   $actualend.insertAfter(codeAfter);
  // }

  // Log time information
  if (this.print) {
    logger.log($timingResultDecl);
    afterJp = logger.getAfterJp();
  }

  this._setAfterJp(afterJp);

  return timeIntervalVar;
};

//C codedefs
// Windows
function _timer_c_windows_declare_vars_now(
  timeStartVar,
  timeEndVar,
  timeFrequencyVar
) {
  return `
  	LARGE_INTEGER ${timeStartVar}, ${timeEndVar}, ${timeFrequencyVar};
	  QueryPerformanceFrequency(&${timeFrequencyVar});
	  QueryPerformanceCounter(&${timeStartVar});
  `;
}

function _timer_c_windows_get_final_time(timeEndVar) {
  return `QueryPerformanceCounter(&${timeEndVar});`;
}

function _timer_c_windows_calc_interval(
  timeStartVar,
  timeEndVar,
  timeFrequencyVar,
  factorConversion
) {
  return `((${timeEndVar}.QuadPart-${timeStartVar}.QuadPart) / (double)${timeFrequencyVar}.QuadPart) * (${factorConversion})`;
}

//Linux
function _timer_c_linux_declare_vars_now(timeStartVar, timeEndVar) {
  return `
	  struct timespec ${timeStartVar}, ${timeEndVar};
	  clock_gettime(CLOCK_MONOTONIC, &${timeStartVar});
  `;
}

function _timer_c_linux_get_final_time(timeEndVar) {
  return `clock_gettime(CLOCK_MONOTONIC, &${timeEndVar});`;
}

function _timer_c_linux_calc_interval(
  timeStartVar,
  timeEndVar,
  factorConversion
) {
  return `((${timeEndVar}.tv_sec + ((double) ${timeEndVar}.tv_nsec / 1000000000)) - (${timeStartVar}.tv_sec + ((double) ${timeStartVar}.tv_nsec / 1000000000))) * (${factorConversion})`;
}

//Cpp codedefs
function _timer_cpp_now(timeVar) {
  return `std::chrono::high_resolution_clock::time_point ${timeVar} = std::chrono::high_resolution_clock::now();`;
}

function _timer_cpp_calc_interval(startVar, endVar, unit) {
  return `std::chrono::duration_cast<std::chrono::${unit}>(${endVar} - ${startVar}).count()`;
}

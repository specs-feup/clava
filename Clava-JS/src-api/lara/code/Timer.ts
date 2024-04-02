import Platforms from "lara-js/api/lara/Platforms.js";
import TimerBase from "lara-js/api/lara/code/TimerBase.js";
import IdGenerator from "lara-js/api/lara/util/IdGenerator.js";
import { TimerUnit } from "lara-js/api/lara/util/TimeUnits.js";
import { FileJp, Joinpoint, Scope } from "../../Joinpoints.js";
import Clava from "../../clava/Clava.js";
import ClavaJoinPoints from "../../clava/ClavaJoinPoints.js";
import Logger from "./Logger.js";

export default class Timer extends TimerBase<Joinpoint> {
  addedDefines: Set<string> = new Set<string>();

  /**
   * Times the code of a given section.
   *
   * @param $start - Starting point of the time measure
   * @param prefix - Message that will appear before the time measure. If undefined, empty string will be used.
   * @param $end - Ending point of the time measure. If undefined, measure is done around starting point.
   */
  time($start: Joinpoint, prefix: string = "", $end: Joinpoint = $start) {
    if (!this._timeValidate($start, $end, "function")) {
      return;
    }

    const $file = $start.getAncestor("file") as FileJp | undefined;

    if ($file === undefined) {
      console.log(
        "Could not find the corresponding file of the given joinpoint: " +
          $start.joinPointType
      );
      return;
    } else if ($file.isCxx) {
      return this._time_cpp($start, prefix, $end);
    } else {
      return this._time_c($start, prefix, $end);
    }
  }

  _time_cpp($start: Joinpoint, prefix: string = "", $end: Joinpoint = $start) {
    if (this.timeUnits.unit == TimerUnit.DAYS) {
      throw "Timer Exception: Timer metrics not implemented for DAYS in C++";
    }
    const cppUnit = this.timeUnits.getCppTimeUnit()!;

    const logger = new Logger(false, this.filename);

    const $file = $start.getAncestor("file") as FileJp;

    // Add include
    $file.addInclude("chrono", true);

    const startVar = IdGenerator.next("clava_timing_start_");
    const endVar = IdGenerator.next("clava_timing_end_");

    const $codeTic = ClavaJoinPoints.stmtLiteral(this._timer_cpp_now(startVar));
    const $codeToc = ClavaJoinPoints.stmtLiteral(this._timer_cpp_now(endVar));
    const $insertionTic = $codeTic;
    const $insertionToc = $codeToc;

    // Declare variable for time interval, which uses calculation as initialization
    const timeIntervalVar = IdGenerator.next("clava_timing_duration_");
    // Create literal node with calculation of time interval
    const $timingResult = ClavaJoinPoints.exprLiteral(
      this._timer_cpp_calc_interval(startVar, endVar, cppUnit, timeIntervalVar)
    );

    // Build message
    logger
      .append(prefix)
      .appendLong(this._timer_cpp_print_interval(cppUnit, timeIntervalVar));
    if (this.printUnit) {
      logger.append(this.timeUnits.getUnitsString());
    }
    logger.ln();

    // Check if $start is a scope
    if ($start instanceof Scope) {
      $start.insertBegin($insertionTic);
    } else {
      $start.insertBefore($insertionTic);
    }

    const $startVarDecl = ClavaJoinPoints.stmtLiteral(
      this._timer_cpp_define_time_var(startVar)
    );
    const $endVarDecl = ClavaJoinPoints.stmtLiteral(
      this._timer_cpp_define_time_var(endVar)
    );
    const $timingResultDecl = ClavaJoinPoints.varDeclNoInit(
      timeIntervalVar,
      ClavaJoinPoints.typeLiteral(
        "std::chrono::high_resolution_clock::duration"
      )
    );
    $insertionTic.insertBefore($startVarDecl);
    $insertionTic.insertBefore($endVarDecl);
    $insertionTic.insertBefore($timingResultDecl);

    let afterJp = undefined;

    // Check if $end is a scope
    if ($end instanceof Scope) {
      $end.insertEnd($insertionToc);
    } else {
      $end.insertAfter($insertionToc);
    }
    $codeToc.insertAfter($timingResult);

    afterJp = $timingResult;

    // Log time information
    if (this.print) {
      logger.log($timingResult);
      afterJp = logger.getAfterJp();
    }

    this.setAfterJp(afterJp);

    return timeIntervalVar;
  }

  _time_c($start: Joinpoint, prefix: string = "", $end: Joinpoint = $start) {
    const logger = new Logger(false, this.filename);

    const $file = $start.getAncestor("file") as FileJp;

    let $varDecl, $codeBefore, $codeAfter, $timingResult;

    // Declare variable for time interval, which uses calculation as initialization
    const timeIntervalVar = IdGenerator.next("clava_timing_duration_");
    const $timingResultDecl = ClavaJoinPoints.varDeclNoInit(
      timeIntervalVar,
      ClavaJoinPoints.builtinType("double")
    );

    if (Platforms.isWindows()) {
      //use QueryPerformanceCounter
      // Add includes
      $file.addInclude("time.h", true);
      $file.addInclude("windows.h", true);

      // get variable names
      const startVar = IdGenerator.next("clava_timing_start_");
      const endVar = IdGenerator.next("clava_timing_end_");
      const frequencyVar = IdGenerator.next("clava_timing_frequency_");

      $varDecl = ClavaJoinPoints.stmtLiteral(
        this._timer_c_windows_declare_vars(startVar, endVar, frequencyVar)
      );
      $codeBefore = ClavaJoinPoints.stmtLiteral(
        this._timer_c_windows_get_time(startVar)
      );
      $codeAfter = ClavaJoinPoints.stmtLiteral(
        this._timer_c_windows_get_time(endVar)
      );

      // Create literal node with calculation of time interval
      $timingResult = ClavaJoinPoints.exprLiteral(
        this._timer_c_windows_calc_interval(
          startVar,
          endVar,
          timeIntervalVar,
          frequencyVar,
          String(this.timeUnits.getMagnitudeFactorFromSeconds())
        ),
        $timingResultDecl.type
      );
    } else if (Platforms.isLinux()) {
      // Add includes
      $file.addInclude("time.h", true);

      // If C99 or C11 standard, needs define at the beginning of the file
      // https://stackoverflow.com/questions/42597685/storage-size-of-timespec-isnt-known
      const needsDefine =
        Clava.getStandard() === "c99" || Clava.getStandard() === "c11";
      if (needsDefine && !this.addedDefines.has($file.location)) {
        $file.insertBegin("#define _POSIX_C_SOURCE 199309L");
        this.addedDefines.add($file.location);
      }

      // get variable names
      const startVar = IdGenerator.next("clava_timing_start_");
      const endVar = IdGenerator.next("clava_timing_end_");

      $varDecl = ClavaJoinPoints.stmtLiteral(
        this._timer_c_linux_declare_vars(startVar, endVar)
      );
      $codeBefore = ClavaJoinPoints.stmtLiteral(
        this._timer_c_linux_get_time(startVar)
      );
      $codeAfter = ClavaJoinPoints.stmtLiteral(
        this._timer_c_linux_get_time(endVar)
      );

      // Create literal node with calculation of time interval
      $timingResult = ClavaJoinPoints.exprLiteral(
        this._timer_c_linux_calc_interval(
          startVar,
          endVar,
          timeIntervalVar,
          String(this.timeUnits.getMagnitudeFactorFromSeconds())
        ),
        $timingResultDecl.type
      );
    } else {
      throw "Timer Exception: Platform not supported (Windows and Linux only)";
    }

    // Build message
    logger.append(prefix).appendDouble(timeIntervalVar);
    if (this.printUnit) {
      logger.append(this.timeUnits.getUnitsString());
    }
    logger.ln();

    const $insertionTic = $codeBefore;
    const $insertionToc = $codeAfter;

    // Check if $start is a scope
    if ($start instanceof Scope) {
      // Insert code
      $start.insertBegin($insertionTic);
    } else {
      // Insert code
      $start.insertBefore($insertionTic);
    }
    $insertionTic.insertBefore($varDecl);
    $insertionTic.insertBefore($timingResultDecl);

    let afterJp = undefined;

    // Check if $end is a scope
    if ($end instanceof Scope) {
      $end.insertEnd($insertionToc);
    } else {
      $end.insertAfter($insertionToc);
    }
    afterJp = $codeAfter.insertAfter($timingResult);

    afterJp = $timingResult;

    // Log time information
    if (this.print) {
      logger.log(afterJp);
      afterJp = logger.getAfterJp();
    }

    this.setAfterJp(afterJp);

    return timeIntervalVar;
  }

  //C codedefs
  // Windows
  private _timer_c_windows_declare_vars(
    timeStartVar: string,
    timeEndVar: string,
    timeFrequencyVar: string
  ) {
    return `LARGE_INTEGER ${timeStartVar}, ${timeEndVar}, ${timeFrequencyVar};
QueryPerformanceFrequency(&${timeFrequencyVar});`;
  }

  private _timer_c_windows_get_time(timeVar: string) {
    return `QueryPerformanceCounter(&${timeVar})`;
  }

  private _timer_c_windows_calc_interval(
    timeStartVar: string,
    timeEndVar: string,
    timeDiffenceVar: string,
    timeFrequencyVar: string,
    factorConversion: string
  ) {
    return `${timeDiffenceVar} = ((${timeEndVar}.QuadPart - ${timeStartVar}.QuadPart) / (double)${timeFrequencyVar}.QuadPart) * (${factorConversion})`;
  }

  //Linux
  private _timer_c_linux_declare_vars(
    timeStartVar: string,
    timeEndVar: string
  ) {
    return `struct timespec ${timeStartVar}, ${timeEndVar};`;
  }

  private _timer_c_linux_get_time(timeVar: string) {
    return `clock_gettime(CLOCK_MONOTONIC, &${timeVar});`;
  }

  private _timer_c_linux_calc_interval(
    timeStartVar: string,
    timeEndVar: string,
    timeDiffenceVar: string,
    factorConversion: string
  ) {
    return `${timeDiffenceVar} = ((${timeEndVar}.tv_sec + ((double) ${timeEndVar}.tv_nsec / 1000000000)) - (${timeStartVar}.tv_sec + ((double) ${timeStartVar}.tv_nsec / 1000000000))) * (${factorConversion})`;
  }

  //Cpp codedefs
  private _timer_cpp_define_time_var(timeVar: string) {
    return `std::chrono::high_resolution_clock::time_point ${timeVar};`;
  }

  private _timer_cpp_now(timeVar: string) {
    return `${timeVar} = std::chrono::high_resolution_clock::now();`;
  }

  private _timer_cpp_calc_interval(
    startVar: string,
    endVar: string,
    unit: string,
    differentialVar: string
  ) {
    return `${differentialVar} = ${endVar} - ${startVar}`;
  }

  private _timer_cpp_print_interval(unit: string, differentialVar: string) {
    return `std::chrono::duration_cast<std::chrono::${unit}>(${differentialVar}).count()`;
  }
}

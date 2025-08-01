import Platforms from "@specs-feup/lara/api/lara/Platforms.js";
import TimerBase from "@specs-feup/lara/api/lara/code/TimerBase.js";
import IdGenerator from "@specs-feup/lara/api/lara/util/IdGenerator.js";
import { TimerUnit } from "@specs-feup/lara/api/lara/util/TimeUnits.js";
import { Scope } from "../../Joinpoints.js";
import Clava from "../../clava/Clava.js";
import ClavaJoinPoints from "../../clava/ClavaJoinPoints.js";
import Logger from "./Logger.js";
export default class Timer extends TimerBase {
    addedDefines = new Set();
    /**
     * Times the code of a given section.
     *
     * @param $start - Starting point of the time measure
     * @param prefix - Message that will appear before the time measure. If undefined, empty string will be used.
     * @param $end - Ending point of the time measure. If undefined, measure is done around starting point.
     */
    time($start, prefix = "", $end = $start) {
        if (!this._timeValidate($start, $end, "function")) {
            return;
        }
        const $file = $start.getAncestor("file");
        if ($file === undefined) {
            console.log("Could not find the corresponding file of the given joinpoint: " +
                $start.joinPointType);
            return;
        }
        else if ($file.isCxx) {
            return this._time_cpp($start, prefix, $end);
        }
        else {
            return this._time_c($start, prefix, $end);
        }
    }
    _time_cpp($start, prefix = "", $end = $start) {
        if (this.timeUnits.unit == TimerUnit.DAYS) {
            throw "Timer Exception: Timer metrics not implemented for DAYS in C++";
        }
        const cppUnit = this.timeUnits.getCppTimeUnit();
        const logger = new Logger(false, this.filename);
        const $file = $start.getAncestor("file");
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
        const $timingResult = ClavaJoinPoints.exprLiteral("long long " +
            this._timer_cpp_calc_interval(startVar, endVar, cppUnit, timeIntervalVar));
        // Build message
        logger.append(prefix).appendLong(timeIntervalVar);
        if (this.printUnit) {
            logger.append(this.timeUnits.getUnitsString());
        }
        logger.ln();
        // Check if $start is a scope
        if ($start instanceof Scope) {
            $start.insertBegin($insertionTic);
        }
        else {
            $start.insertBefore($insertionTic);
        }
        const $startVarDecl = ClavaJoinPoints.stmtLiteral(this._timer_cpp_define_time_var(startVar));
        const $endVarDecl = ClavaJoinPoints.stmtLiteral(this._timer_cpp_define_time_var(endVar));
        $insertionTic.insertBefore($startVarDecl);
        $insertionTic.insertBefore($endVarDecl);
        let afterJp = undefined;
        // Check if $end is a scope
        if ($end instanceof Scope) {
            $end.insertEnd($insertionToc);
        }
        else {
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
    _time_c($start, prefix = "", $end = $start) {
        const logger = new Logger(false, this.filename);
        const $file = $start.getAncestor("file");
        let $varDecl, $codeBefore, $codeAfter, $timingResult;
        // Declare variable for time interval, which uses calculation as initialization
        const timeIntervalVar = IdGenerator.next("clava_timing_duration_");
        const $timingResultDecl = ClavaJoinPoints.varDeclNoInit(timeIntervalVar, ClavaJoinPoints.builtinType("double"));
        if (Platforms.isWindows()) {
            //use QueryPerformanceCounter
            // Add includes
            $file.addInclude("time.h", true);
            $file.addInclude("windows.h", true);
            // get variable names
            const startVar = IdGenerator.next("clava_timing_start_");
            const endVar = IdGenerator.next("clava_timing_end_");
            const frequencyVar = IdGenerator.next("clava_timing_frequency_");
            $varDecl = ClavaJoinPoints.stmtLiteral(this._timer_c_windows_declare_vars(startVar, endVar, frequencyVar));
            $codeBefore = ClavaJoinPoints.stmtLiteral(this._timer_c_windows_get_time(startVar));
            $codeAfter = ClavaJoinPoints.stmtLiteral(this._timer_c_windows_get_time(endVar));
            // Create literal node with calculation of time interval
            $timingResult = ClavaJoinPoints.exprLiteral(this._timer_c_windows_calc_interval(startVar, endVar, timeIntervalVar, frequencyVar, String(this.timeUnits.getMagnitudeFactorFromSeconds())), $timingResultDecl.type);
        }
        else if (Platforms.isLinux()) {
            // Add includes
            $file.addInclude("time.h", true);
            // If C99 or C11 standard, needs define at the beginning of the file
            // https://stackoverflow.com/questions/42597685/storage-size-of-timespec-isnt-known
            const needsDefine = Clava.getStandard() === "c99" || Clava.getStandard() === "c11";
            if (needsDefine && !this.addedDefines.has($file.location)) {
                $file.insertBegin("#define _POSIX_C_SOURCE 199309L");
                this.addedDefines.add($file.location);
            }
            // get variable names
            const startVar = IdGenerator.next("clava_timing_start_");
            const endVar = IdGenerator.next("clava_timing_end_");
            $varDecl = ClavaJoinPoints.stmtLiteral(this._timer_c_linux_declare_vars(startVar, endVar));
            $codeBefore = ClavaJoinPoints.stmtLiteral(this._timer_c_linux_get_time(startVar));
            $codeAfter = ClavaJoinPoints.stmtLiteral(this._timer_c_linux_get_time(endVar));
            // Create literal node with calculation of time interval
            $timingResult = ClavaJoinPoints.exprLiteral(this._timer_c_linux_calc_interval(startVar, endVar, timeIntervalVar, String(this.timeUnits.getMagnitudeFactorFromSeconds())), $timingResultDecl.type);
        }
        else {
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
        }
        else {
            // Insert code
            $start.insertBefore($insertionTic);
        }
        $insertionTic.insertBefore($varDecl);
        $insertionTic.insertBefore($timingResultDecl);
        let afterJp = undefined;
        // Check if $end is a scope
        if ($end instanceof Scope) {
            $end.insertEnd($insertionToc);
        }
        else {
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
    _timer_c_windows_declare_vars(timeStartVar, timeEndVar, timeFrequencyVar) {
        return `LARGE_INTEGER ${timeStartVar}, ${timeEndVar}, ${timeFrequencyVar};
QueryPerformanceFrequency(&${timeFrequencyVar});`;
    }
    _timer_c_windows_get_time(timeVar) {
        return `QueryPerformanceCounter(&${timeVar});`;
    }
    _timer_c_windows_calc_interval(timeStartVar, timeEndVar, timeDiffenceVar, timeFrequencyVar, factorConversion) {
        return `${timeDiffenceVar} = ((${timeEndVar}.QuadPart - ${timeStartVar}.QuadPart) / (double)${timeFrequencyVar}.QuadPart) * (${factorConversion})`;
    }
    //Linux
    _timer_c_linux_declare_vars(timeStartVar, timeEndVar) {
        return `struct timespec ${timeStartVar}, ${timeEndVar};`;
    }
    _timer_c_linux_get_time(timeVar) {
        return `clock_gettime(CLOCK_MONOTONIC, &${timeVar});`;
    }
    _timer_c_linux_calc_interval(timeStartVar, timeEndVar, timeDiffenceVar, factorConversion) {
        return `${timeDiffenceVar} = ((${timeEndVar}.tv_sec + ((double) ${timeEndVar}.tv_nsec / 1000000000)) - (${timeStartVar}.tv_sec + ((double) ${timeStartVar}.tv_nsec / 1000000000))) * (${factorConversion})`;
    }
    //Cpp codedefs
    _timer_cpp_define_time_var(timeVar) {
        return `std::chrono::high_resolution_clock::time_point ${timeVar};`;
    }
    _timer_cpp_now(timeVar) {
        return `${timeVar} = std::chrono::high_resolution_clock::now();`;
    }
    _timer_cpp_calc_interval(startVar, endVar, unit, differentialVar) {
        return `${differentialVar} = std::chrono::duration_cast<std::chrono::${unit}>(${endVar} - ${startVar}).count()`;
    }
}
//# sourceMappingURL=Timer.js.map
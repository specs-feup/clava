import Strings from "lara-js/api/lara/Strings.js";
import Metric from "lara-js/api/lara/metrics/Metric.js";
import MetricResult from "lara-js/api/lara/metrics/MetricResult.js";
import { TimerUnit } from "lara-js/api/lara/util/TimeUnits.js";
import Timer from "../code/Timer.js";
/**
 * Measures execution time of an application.
 */
export default class ExecutionTimeMetric extends Metric {
    prefix;
    constructor(prefix = "time:") {
        super("Execution Time");
        this.prefix = prefix;
    }
    instrument($start, $end = $start) {
        const timer = new Timer(TimerUnit.NANOSECONDS);
        timer.setPrintUnit(false);
        timer.time($start, this.prefix, $end);
    }
    report(processExecutor) {
        const processOutput = processExecutor.getConsoleOutput();
        if (processOutput === undefined) {
            throw new Error("No process output found");
        }
        const value = Strings.extractValue(this.prefix, processOutput);
        if (value === undefined) {
            throw new Error("No value found");
        }
        return new MetricResult(parseFloat(value), this.getUnit());
    }
    getImport() {
        return "lara.metrics.ExecutionTimeMetric";
    }
    getUnit() {
        return "ns";
    }
}
//# sourceMappingURL=ExecutionTimeMetric.js.map
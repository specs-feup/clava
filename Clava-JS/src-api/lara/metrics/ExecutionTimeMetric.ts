import Strings from "lara-js/api/lara/Strings.js";
import Metric from "lara-js/api/lara/metrics/Metric.js";
import MetricResult from "lara-js/api/lara/metrics/MetricResult.js";
import ProcessExecutor from "lara-js/api/lara/util/ProcessExecutor.js";
import { TimerUnit } from "lara-js/api/lara/util/TimeUnits.js";
import { Joinpoint } from "../../Joinpoints.js";
import Timer from "../code/Timer.js";

/**
 * Measures execution time of an application.
 */
export default class ExecutionTimeMetric extends Metric<Joinpoint> {
  private prefix: string;

  constructor(prefix: string = "time:") {
    super("Execution Time");

    this.prefix = prefix;
  }

  instrument($start: Joinpoint, $end: Joinpoint = $start) {
    const timer = new Timer(TimerUnit.NANOSECONDS);
    timer.setPrintUnit(false);
    timer.time($start, this.prefix, $end);
  }

  report(processExecutor: ProcessExecutor) {
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

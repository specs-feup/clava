import Strings from "@specs-feup/lara/api/lara/Strings.ts";
import Metric from "@specs-feup/lara/api/lara/metrics/Metric.ts";
import MetricResult from "@specs-feup/lara/api/lara/metrics/MetricResult.ts";
import ProcessExecutor from "@specs-feup/lara/api/lara/util/ProcessExecutor.ts";
import { TimerUnit } from "@specs-feup/lara/api/lara/util/TimeUnits.ts";
import { Joinpoint } from "../../Joinpoints.ts";
import Timer from "../code/Timer.ts";

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

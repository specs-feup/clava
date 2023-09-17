import Strings from "lara-js/api/lara/Strings.js";
import Metric from "lara-js/api/lara/metrics/Metric.js";
import MetricResult from "lara-js/api/lara/metrics/MetricResult.js";
import ProcessExecutor from "lara-js/api/lara/util/ProcessExecutor.js";
import { Joinpoint } from "../../Joinpoints.js";
import Energy from "../code/Energy.js";

/**
 * Measures energy consumed during an application.
 */
export default class EnergyMetric extends Metric<Joinpoint> {
  private prefix: string;
  energy: Energy;

  constructor(prefix: string = "energy:") {
    super("Energy");

    this.energy = new Energy();
    this.prefix = prefix;
  }

  instrument($start: Joinpoint, $end: Joinpoint = $start) {
    this.energy.setPrintUnit(false);
    this.energy.measure($start, this.prefix, $end);
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

    return new MetricResult(parseFloat(value), this.energy.getPrintUnit());
  }

  getImport(): string {
    return "lara.metrics.EnergyMetric";
  }

  getUnit(): string {
    return this.energy.getPrintUnit();
  }
}

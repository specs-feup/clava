import Strings from "lara-js/api/lara/Strings.js";
import Metric from "lara-js/api/lara/metrics/Metric.js";
import MetricResult from "lara-js/api/lara/metrics/MetricResult.js";
import Energy from "../code/Energy.js";
/**
 * Measures energy consumed during an application.
 */
export default class EnergyMetric extends Metric {
    prefix;
    energy;
    constructor(prefix = "energy:") {
        super("Energy");
        this.energy = new Energy();
        this.prefix = prefix;
    }
    instrument($start, $end = $start) {
        this.energy.setPrintUnit(false);
        this.energy.measure($start, this.prefix, $end);
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
        return new MetricResult(parseFloat(value), this.energy.getPrintUnit());
    }
    getImport() {
        return "lara.metrics.EnergyMetric";
    }
    getUnit() {
        return this.energy.getPrintUnit();
    }
}
//# sourceMappingURL=EnergyMetric.js.map
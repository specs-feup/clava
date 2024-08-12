import Clava from "../Clava.js";
import ProcessExecutor from "lara-js/api/lara/util/ProcessExecutor.js";
import VitisHlsReportParser from "./VitisHlsReportParser.js";
import Tool from "lara-js/api/lara/tool/Tool.js";
import ToolUtils from "lara-js/api/lara/tool/ToolUtils.js";
import Io from "lara-js/api/lara/Io.js";

export default class VitisHls extends Tool {
  topFunction: string;
  platform: string;
  clock!: number;
  vitisDir: string = "VitisHLS";
  vitisProjName: string = "VitisHLSClavaProject";
  sourceFiles: string[] = [];
  flowTarget: "vitis" | "vivado" = "vivado";

  constructor(
    topFunction: string,
    clock: number = 10,
    platform: string = "xcvu5p-flva2104-1-e",
    disableWeaving: boolean = false
  ) {
    super("VITIS-HLS", disableWeaving);

    this.topFunction = topFunction;
    this.platform = platform;
    this.setClock(clock);
  }

  setTopFunction(topFunction: string) {
    this.topFunction = topFunction;
    return this;
  }

  setPlatform(platform: string) {
    this.platform = platform;
    return this;
  }

  setClock(clock: number) {
    if (clock <= 0) {
      throw new Error(
        `${this.getTimestamp()} Clock value must be a positive integer!`
      );
    } else {
      this.clock = clock;
    }
    return this;
  }

  setFlowTarget(target: "vitis" | "vivado") {
    this.flowTarget = target;
    return this;
  }

  addSource(file: string) {
    this.sourceFiles.push(file);
    return this;
  }

  private getTimestamp() {
    const curr = new Date();
    const res = `[${
      this.toolName
    } ${curr.getHours()}:${curr.getMinutes()}:${curr.getSeconds()}]`;
    return res;
  }

  synthesize(verbose: boolean = true) {
    console.log(`${this.getTimestamp()} Setting up Vitis HLS executor`);

    this.clean();
    this.generateTclFile();
    this.executeVitis(verbose);
    return Io.isFile(this.getSynthesisReportPath());
  }

  clean() {
    Io.deleteFolderContents(this.vitisDir);
  }

  private getSynthesisReportPath() {
      return this.vitisDir + "/" + this.vitisProjName + "/solution1/syn/report/csynth.xml";
  }

  private executeVitis(verbose: boolean) {
    console.log(`${this.getTimestamp()} Executing Vitis HLS`);

    const pe = new ProcessExecutor();
    pe.setWorkingDir(this.vitisDir);
    pe.setPrintToConsole(verbose);
    pe.execute("vitis_hls", "-f", "script.tcl");

    console.log(`${this.getTimestamp()} Finished executing Vitis HLS`);
  }

  private getTclInputFiles() {
    let str = "";
    const weavingFolder = ToolUtils.parsePath(Clava.getWeavingFolder());

    // make sure the files are woven
    Io.deleteFolderContents(weavingFolder);
    Clava.writeCode(weavingFolder);

    // if no files were added, we assume that every woven file should be used
    if (this.sourceFiles.length == 0) {
      console.log(
        `${this.getTimestamp()} No source files specified, assuming current AST is the input`
      );
      for (const file of Io.getFiles(Clava.getWeavingFolder())) {
        const exts = [".c", ".cpp", ".h", ".hpp"];
        const res = exts.some((ext) => file.name.includes(ext));
        if (res) str += "add_files " + weavingFolder + "/" + file.name + "\n";
      }
    } else {
      for (const file of this.sourceFiles) {
        str += "add_files " + weavingFolder + "/" + file + "\n";
      }
    }
    return str;
  }

  private generateTclFile() {
    const cmd = `
open_project ${this.vitisProjName}
set_top ${this.topFunction}
${this.getTclInputFiles()}
open_solution "solution1" -flow_target ${this.flowTarget}
set_part { ${this.platform}}
create_clock -period ${this.clock} -name default
csynth_design
exit
    `;

    Io.writeFile(this.vitisDir + "/script.tcl", cmd);
  }

  getSynthesisReport() {
    console.log(`${this.getTimestamp()} Processing synthesis report`);

    const parser = new VitisHlsReportParser(this.getSynthesisReportPath());
    const json = parser.getSanitizedJSON();

    console.log(`${this.getTimestamp()} Finished processing synthesis report`);
    return json;
  }

  preciseStr(n: number, decimalPlaces?: number) {
    return (+n).toFixed(decimalPlaces);
  }

  prettyPrintReport(report: ReturnType<VitisHlsReportParser["getSanitizedJSON"]>) {
    const period = this.preciseStr(report["clockEstim"], 2);
    const freq = this.preciseStr(report["fmax"], 2);

    const out = `
----------------------------------------
Vitis HLS synthesis report

Targeted a ${report["platform"]} with target clock ${freq} ns

Achieved an estimated clock of ${period} ns (${freq} MHz)

Estimated latency for top function ${report["topFun"]}:
Worst case: ${report["latencyWorst"]} cycles
  Avg case: ${report["latencyAvg"]} cycles
 Best case: ${report["latencyBest"]} cycles

Estimated execution time:
Worst case: ${report["execTimeWorst"]} s
  Avg case: ${report["execTimeAvg"]} s
 Best case: ${report["execTimeBest"]} s

Resource usage:
FF:   ${report["FF"]} (${this.preciseStr(report["perFF"] * 100, 2)}%)
LUT:  ${report["LUT"]} (${this.preciseStr(report["perLUT"] * 100, 2)}%)
BRAM: ${report["BRAM"]} (${this.preciseStr(report["perBRAM"] * 100, 2)}%)
DSP:  ${report["DSP"]} (${this.preciseStr(report["perDSP"] * 100, 2)}%)
----------------------------------------`;
    console.log(out);
  }
}

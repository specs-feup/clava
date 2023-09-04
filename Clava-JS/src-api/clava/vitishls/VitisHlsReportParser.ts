import Io from "lara-js/api/lara/Io.js";

export default class VitisHlsReportParser {
  private reportPath: string;

  constructor(reportPath: string) {
    this.reportPath = reportPath;
  }

  private xmlToJson(xml: string) {
    //parses only the "leaves" of the XML string, which is enough for us. For now.
    const regex =
      /(?:<([a-zA-Z'-\d_]*)(?:\s[^>]*)*>)((?:(?!<\1).)*)(?:<\/\1>)|<([a-zA-Z'-]*)(?:\s*)*\/>/gm;

    const json: Record<string, any> = {};
    for (const match of xml.matchAll(regex)) {
      const key = match[1] || match[3];
      const val = match[2] && this.xmlToJson(match[2]);
      json[key] = (val && Object.keys(val).length ? val : match[2]) || null;
    }
    return json;
  }

  getSanitizedJSON() {
    const raw = this.getRawJSON();

    const fmax = this.calculateMaxFrequency(
      raw["EstimatedClockPeriod"] as number
    );
    const execTimeWorst = this.calculateExecutionTime(
      raw["Worst-caseLatency"] as number,
      fmax
    );
    const execTimeAvg = this.calculateExecutionTime(
      raw["Average-caseLatency"] as number,
      fmax
    );
    const execTimeBest = this.calculateExecutionTime(
      raw["Best-caseLatency"] as number,
      fmax
    );
    const hasFixedLatency: boolean =
      raw["Best-caseLatency"] === raw["Worst-caseLatency"];

    return {
      platform: raw["Part"] as string,
      topFun: raw["TopModelName"] as string,

      clockTarget: raw["TargetClockPeriod"] as number,
      clockEstim: raw["EstimatedClockPeriod"] as number,
      fmax: fmax,

      latencyWorst: raw["Worst-caseLatency"] as number,
      latencyAvg: raw["Average-caseLatency"] as number,
      latencyBest: raw["Best-caseLatency"] as number,
      hasFixedLatency: hasFixedLatency,
      execTimeWorst: execTimeWorst,
      execTimeAvg: execTimeAvg,
      execTimeBest: execTimeBest,

      FF: raw["FF"] as number,
      LUT: raw["LUT"] as number,
      BRAM: raw["BRAM_18K"] as number,
      DSP: raw["DSP"] as number,

      availFF: raw["AVAIL_FF"] as number,
      availLUT: raw["AVAIL_LUT"] as number,
      availBRAM: raw["AVAIL_BRAM"] as number,
      availDSP: raw["AVAIL_DSP"] as number,

      perFF: (raw["FF"] as number) / (raw["AVAIL_FF"] as number),
      perLUT: (raw["LUT"] as number) / (raw["AVAIL_LUT"] as number),
      perBRAM: (raw["BRAM_18K"] as number) / (raw["AVAIL_BRAM"] as number),
      perDSP: (raw["DSP"] as number) / (raw["AVAIL_DSP"] as number),
    };
  }

  private getRawJSON() {
    const xml = Io.readFile(this.reportPath);
    return this.xmlToJson(xml);
  }

  calculateMaxFrequency(clockEstim: number): number {
    return (1 / clockEstim) * 1000;
  }

  calculateExecutionTime(latency: number, freqMHz: number): number {
    const freqHz = freqMHz * 1e6;
    return latency / freqHz;
  }
}

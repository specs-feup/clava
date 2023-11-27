import AnalyserResult from "./AnalyserResult.js";

describe("AnalyserResult", () => {
  describe("analyse", () => {
    it("throws an exception for not being implemented", () => {
      const analyserResultTest = new AnalyserResult(
        "correct",
        "test",
        "test",
        undefined
      );
      expect(analyserResultTest.analyse()).toThrow("Not implemented");
    });
  });

  describe("getName", () => {
    it("returns the name of the AnalyserResult", () => {
      const analyserResultTest = new AnalyserResult(
        "correct",
        "test",
        "test",
        undefined
      );
      expect(analyserResultTest.getName()).toBe("correct");
    });
  });

  describe("getNode", () => {
    it("returns the node of the AnalyserResult", () => {
      const analyserResultTest = new AnalyserResult(
        "test",
        "correct",
        "test",
        undefined
      );
      expect(analyserResultTest.getNode()).toBe("correct");
    });
  });
});

import { describe, it, expect } from "bun:test";
import AnalyserResult from "./AnalyserResult.js";
import ClavaJoinPoints from "@specs-feup/clava/api/clava/ClavaJoinPoints.js";

describe("AnalyserResult", () => {
  describe("getName", () => {
    it("returns the name of the AnalyserResult", () => {
      const analyserResultTest = new AnalyserResult(
        "correct",
        ClavaJoinPoints.emptyStmt(),
        "test",
        undefined
      );
      expect(analyserResultTest.getName()).toBe("correct");
    });
  });

  describe("getNode", () => {
    it("returns the node of the AnalyserResult", () => {
      const jp = ClavaJoinPoints.emptyStmt();
      const analyserResultTest = new AnalyserResult(
        "test",
        jp,
        "test",
        undefined
      );
      expect(analyserResultTest.getNode()).toBe(jp);
    });
  });
});

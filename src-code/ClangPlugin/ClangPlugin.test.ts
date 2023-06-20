import { jest } from "@jest/globals";
import fs from "fs";
import path from "path";

import ClangPlugin from "./ClangPlugin.js";

describe("ClangPlugin", () => {
  describe("validateClangExecutable", () => {
    it("should return the clang executable name when given a valid command", async () => {
      const executableName = "clang -o main main.c";

      await expect(ClangPlugin.validateClangExecutable(executableName))
        .resolves;
    });

    it("should throw an error when given an invalid command", async () => {
      const executableName = "gcc -o main main.c";

      await expect(
        ClangPlugin.validateClangExecutable(executableName)
      ).rejects.toThrowError("Could not find clang executable");
    });
  });

  describe("getClangVersionNumberFromExecutable", () => {
    it("should return the clang version number when given a valid clang executable", async () => {
      const clangExecutable = "clang++-14";
      const expectedVersionNumber = "14.0.0";

      const actualVersionNumber =
        await ClangPlugin.getClangVersionNumberFromExecutable(clangExecutable);

      expect(actualVersionNumber).toBe(expectedVersionNumber);
    });

    it("should throw an error when given an invalid clang executable", async () => {
      const clangExecutable = "invalid-clang-executable";

      await expect(
        ClangPlugin.getClangVersionNumberFromExecutable(clangExecutable)
      ).rejects.toThrowError("Invalid executable");
    });
  });

  describe("getClangVersion", () => {
    it("should return the clang version number when given a valid clang executable", async () => {
      const clangExecutable = "clang-14";
      const expectedVersion = "14.0.0";
      const actualVersion = await ClangPlugin.getClangVersion(clangExecutable);
      expect(actualVersion).toBe(expectedVersion);
    });

    it("should throw an error when given an invalid clang executable", async () => {
      const clangExecutable = "gcc";
      await expect(
        ClangPlugin.getClangVersion(clangExecutable)
      ).rejects.toThrowError("Could not find clang executable");
    });
  });

  describe("getAvailablePlugins", () => {
    it("should return a map of available clang plugins", async () => {
      const projectDir = process.cwd();
      const testDir = path.join(projectDir, "clang-plugin-binaries");

      const expectedMap = {
        "14.0.0": `${testDir}/ClangASTDumper_14.0.0.so`,
        //"15.0.0": `${testDir}/ClangASTDumper_15.0.0.so`,
      };

      expect(ClangPlugin.clangPluginDir).toBe("clang-plugin-binaries");
      expect(ClangPlugin.pluginNamePrefix).toBe("ClangASTDumper");

      const actualMap = await ClangPlugin.getAvailablePlugins();

      expect(actualMap).toEqual(expectedMap);
      jest.restoreAllMocks();
    });

    it("should throw an error when the clang-plugin-binaries directory does not exist", async () => {
      jest.spyOn(fs, "existsSync").mockClear().mockReturnValue(false);

      await expect(ClangPlugin.getAvailablePlugins()).rejects.toThrowError(
        "Could not find 'clang-plugin-binaries' directory"
      );

      jest.restoreAllMocks();
    });
  });
});

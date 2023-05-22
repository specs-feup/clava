import "mocha";
import * as chai from "chai";
import chaiAsPromised from "chai-as-promised";
const expect = chai.expect;
chai.use(chaiAsPromised);
import sinon from "sinon";
import fs from "fs";
import path from "path";

import ClangPlugin from "../../src/ClangPlugin/ClangPlugin";

describe("ClangPlugin", () => {
  describe("validateClangExecutable", () => {
    it("should return the clang executable name when given a valid command", async () => {
      const executableName = "clang -o main main.c";

      await expect(ClangPlugin.validateClangExecutable(executableName)).to.be
        .fulfilled;
    });

    it("should throw an error when given an invalid command", async () => {
      const executableName = "gcc -o main main.c";

      await expect(
        ClangPlugin.validateClangExecutable(executableName)
      ).rejectedWith(Error, "Could not find clang executable");
    });
  });

  describe("getClangVersionNumberFromExecutable", () => {
    it("should return the clang version number when given a valid clang executable", async () => {
      const clangExecutable = "clang++-14";
      const expectedVersionNumber = "14.0.0";

      const actualVersionNumber =
        await ClangPlugin.getClangVersionNumberFromExecutable(clangExecutable);

      expect(actualVersionNumber).to.equal(expectedVersionNumber);
    });

    it("should throw an error when given an invalid clang executable", async () => {
      const clangExecutable = "invalid-clang-executable";

      await expect(
        ClangPlugin.getClangVersionNumberFromExecutable(clangExecutable)
      ).rejectedWith(Error, "Invalid executable");
    });
  });

  describe("getClangVersion", () => {
    it("should return the clang version number when given a valid clang executable", async () => {
      const clangExecutable = "clang-14";
      const expectedVersion = "14.0.0";
      const actualVersion = await ClangPlugin.getClangVersion(clangExecutable);
      expect(actualVersion).to.equal(expectedVersion);
    });

    it("should throw an error when given an invalid clang executable", async () => {
      const clangExecutable = "gcc";
      await expect(
        ClangPlugin.getClangVersion(clangExecutable)
      ).to.be.rejectedWith("Could not find clang executable");
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

      //sinon.stub(fs, "existsSync").returns(true);
      sinon.stub(ClangPlugin, "pluginNamePrefix").get(() => "ClangASTDumper");
      sinon
        .stub(ClangPlugin, "clangPluginDir")
        .get(() => "clang-plugin-binaries");

      const actualMap = await ClangPlugin.getAvailablePlugins();

      expect(actualMap).to.deep.equal(expectedMap);
      sinon.restore();
    });

    it("should throw an error when the clang-plugin-binaries directory does not exist", async () => {
      sinon.stub(fs, "existsSync").returns(false);

      await expect(ClangPlugin.getAvailablePlugins()).to.be.rejectedWith(
        "Could not find 'clang-plugin-binaries' directory"
      );

      sinon.restore();
    });
  });
});

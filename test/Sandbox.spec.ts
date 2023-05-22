import "mocha";
import * as chai from "chai";
import chaiAsPromised from "chai-as-promised";
const expect = chai.expect;
chai.use(chaiAsPromised);

import Sandbox from "../src/Sandbox";

describe("Sandbox", () => {
  describe("sanitizeCommand", () => {
    it("should return the original command if it does not contain any invalid characters", async () => {
      const command = ["echo", "Hello, world!"];
      const result = await Sandbox.sanitizeCommand(command);
      expect(result).to.deep.equal(command);
    });

    it("should throw an error if the command contains a semicolon", async () => {
      const command = ["echo", "Hello; world!"];
      await expect(Sandbox.sanitizeCommand(command)).to.be.rejectedWith(
        "Invalid command"
      );
    });

    it("should throw an error if the command contains an ampersand", async () => {
      const command = ["echo", "Hello & world!"];
      await expect(Sandbox.sanitizeCommand(command)).to.be.rejectedWith(
        "Invalid command"
      );
    });

    it("should throw an error if the command contains a vertical bar", async () => {
      const command = ["echo", "Hello | world!"];
      await expect(Sandbox.sanitizeCommand(command)).to.be.rejectedWith(
        "Invalid command"
      );
    });
  });

  describe("splitCommandArgsEnv", () => {
    it("should split the command and arguments from the environment variables", async () => {
      const commandArgs = [
        "VAR1=value1",
        "VAR2=value2",
        "echo",
        "Hello, world!",
        "--name",
        "John",
      ];
      const [command, args, env] = await Sandbox.splitCommandArgsEnv(
        commandArgs
      );
      expect(command).to.equal("echo");
      expect(args).to.deep.equal(["Hello, world!", "--name", "John"]);
      expect(env).to.deep.equal(["VAR1=value1", "VAR2=value2"]);
    });

    it("should handle commands with no arguments", async () => {
      const commandArgs = ["VAR1=value1", "VAR2=value2", "echo"];
      const [command, args, env] = await Sandbox.splitCommandArgsEnv(
        commandArgs
      );
      expect(command).to.equal("echo");
      expect(args).to.deep.equal([]);
      expect(env).to.deep.equal(["VAR1=value1", "VAR2=value2"]);
    });

    it("should handle commands with no environment variables", async () => {
      const commandArgs = ["echo", "Hello, world!", "--name", "John"];
      const [command, args, env] = await Sandbox.splitCommandArgsEnv(
        commandArgs
      );
      expect(command).to.equal("echo");
      expect(args).to.deep.equal(["Hello, world!", "--name", "John"]);
      expect(env).to.deep.equal([]);
    });
  });
});

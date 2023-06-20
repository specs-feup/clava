import { jest } from "@jest/globals";
import Sandbox from "./Sandbox";

describe("Sandbox", () => {
  describe("sanitizeCommand", () => {
    it("should return the original command if it does not contain any invalid characters", async () => {
      const command = ["echo", "Hello, world!"];
      const result = await Sandbox.sanitizeCommand(command);
      expect(result).toEqual(command);
    });

    it("should throw an error if the command contains a semicolon", async () => {
      const command = ["echo", "Hello; world!"];
      await expect(Sandbox.sanitizeCommand(command)).rejects.toThrowError(
        "Invalid command"
      );
    });

    it("should throw an error if the command contains an ampersand", async () => {
      const command = ["echo", "Hello & world!"];
      await expect(Sandbox.sanitizeCommand(command)).rejects.toThrowError(
        "Invalid command"
      );
    });

    it("should throw an error if the command contains a vertical bar", async () => {
      const command = ["echo", "Hello | world!"];
      await expect(Sandbox.sanitizeCommand(command)).rejects.toThrowError(
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
      expect(command).toBe("echo");
      expect(args).toEqual(["Hello, world!", "--name", "John"]);
      expect(env).toEqual(["VAR1=value1", "VAR2=value2"]);
    });

    it("should handle commands with no arguments", async () => {
      const commandArgs = ["VAR1=value1", "VAR2=value2", "echo"];
      const [command, args, env] = await Sandbox.splitCommandArgsEnv(
        commandArgs
      );
      expect(command).toBe("echo");
      expect(args).toEqual([]);
      expect(env).toEqual(["VAR1=value1", "VAR2=value2"]);
    });

    it("should handle commands with no environment variables", async () => {
      const commandArgs = ["echo", "Hello, world!", "--name", "John"];
      const [command, args, env] = await Sandbox.splitCommandArgsEnv(
        commandArgs
      );
      expect(command).toBe("echo");
      expect(args).toEqual(["Hello, world!", "--name", "John"]);
      expect(env).toEqual([]);
    });
  });
});

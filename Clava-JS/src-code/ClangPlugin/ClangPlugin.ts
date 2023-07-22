import fs from "fs";
import path from "path";
import Sandbox from "../Sandbox.js";
import { fileURLToPath } from "url";

export default class ClangPlugin {
  static clangPluginDir = "clang-plugin-binaries";
  static pluginNamePrefix = "ClangASTDumper";

  /**
   * Validate that the executable provided is indeed a clang executable
   *
   * **Sanitize the input before calling this method**
   *
   * @param executableName - String containing the command the user uses to compile their code normally
   * @returns String containing the clang executable name
   */
  static validateClangExecutable(executableName: string): Promise<void> {
    return new Promise((resolve, reject) => {
      const commandRegex = /(?:^|\s)(\S*clang\S*)(?=\s|$)/;
      const clangExecutable = commandRegex.exec(executableName);
      if (clangExecutable) {
        resolve();
      } else {
        reject(new Error("Could not find clang executable"));
      }
    });
  }

  /**
   * Gets the clang version number from a clang executable
   *
   * **Sanitize the input before calling this method**
   *
   * @param clangExecutable - String containing the clang executable name
   * @returns String containing the clang version number (e.g. 14.0.0)
   */
  static getClangVersionNumberFromExecutable(
    clangExecutable: string
  ): Promise<string> {
    return new Promise((resolve, reject) => {
      try {
        const output = Sandbox.executeSandboxedCommand(clangExecutable, [
          "--version",
        ]);

        const versionRegex = /clang version (\d+\.\d+\.\d+)/;
        const version = versionRegex.exec(output);
        if (version) {
          resolve(version[1]);
        } else {
          reject(new Error("Could not find clang version"));
        }
      } catch (error) {
        reject(error);
      }
    });
  }

  /**
   * Gets the clang version number from a compiler executable
   *
   * **Sanitize the input before calling this method**
   *
   * @param executableName - String containing the executable the user uses to compile their code normally
   * @returns String containing the clang version number (e.g. 14.0.0)
   */
  static async getClangVersion(executableName: string): Promise<string> {
    await this.validateClangExecutable(executableName);
    return this.getClangVersionNumberFromExecutable(executableName);
  }

  /**
   *  Searches the 'clang-plugin-binaries' directory for available clang plugins
   *  and returns a map of the available plugins.
   *
   * @returns Map of available clang plugins
   */
  static getAvailablePlugins(): Promise<{ [version: string]: string }> {
    return new Promise((resolve, reject) => {
      const basedir = path.dirname(
        path.dirname(path.dirname(fileURLToPath(import.meta.url)))
      );
      const dir = path.join(basedir, this.clangPluginDir);

      const regexPattern = new RegExp(
        `${this.pluginNamePrefix}_(\\d+\\.\\d+\\.\\d+)\\.so`
      );

      if (!fs.existsSync(dir)) {
        reject(new Error("Could not find 'clang-plugin-binaries' directory"));
      }
      const files = fs.readdirSync(dir);
      const map: { [version: string]: string } = {};
      for (const file of files) {
        const match = file.match(regexPattern);
        if (match) {
          const version = match[1];
          map[version] = path.join(dir, file);
        }
      }
      resolve(map);
    });
  }

  /**
   * Gets the absolute path to the clang plugin for a compiler
   *
   * @param executableName - Name of the compiler executable
   * @returns The absolute path to the clang plugin or throws an error if no compatible plugin found
   */
  static async getPluginPath(executableName: string): Promise<string> {
    const [clangVersion, availablePlugins] = await Promise.all([
      this.getClangVersion(executableName),
      this.getAvailablePlugins(),
    ]);

    if (clangVersion in availablePlugins) {
      return availablePlugins[clangVersion];
    } else {
      throw new Error("Could not find plugin for provided clang executable");
    }
  }

  /**
   * Executes the clang plugin in a sandboxed environment with the given arguments
   * and returns a pipe to the output.
   *
   * @param commandList - Command used to execute the compiler by the user
   * @param pluginPath - Path to the clang plugin aligned with the compiler version
   * @param args - Arguments to pass to the clang plugin
   */
  static async executeClangPlugin(
    commandList: (string | number)[]
  ): Promise<string> {
    const sanitizedCommand = await Sandbox.sanitizeCommand(commandList);

    const [command, args, env] = await Sandbox.splitCommandArgsEnv(
      sanitizedCommand
    );
    const pluginPath = await this.getPluginPath(command);

    const envMap = env.reduce((acc, curr) => {
      const [key, value] = curr.split("=");
      return acc.set(key, value);
    }, new Map<string, string>());

    const commandArgs = [
      ...args.map((arg) => this.#ensureAbsolutePath(arg.toString())),
      "-Xclang",
      "-load",
      "-Xclang",
      pluginPath,
    ];

    return Sandbox.executeSandboxedCommand(command, commandArgs, envMap);
  }

  /**
   * Ensures that the argument is an absolute path if it exists
   * otherwise returns the argument itself
   *
   * @param argument - String containing the argument to check
   * @returns The absolute path to the argument if it exists, otherwise the argument itself
   */
  static #ensureAbsolutePath(argument: string): string {
    if (fs.existsSync(argument)) {
      return path.resolve(argument);
    } else {
      return argument;
    }
  }
}

import * as fs from "fs";
import * as path from "path";
import Sandbox from "../Sandbox.js";
import { URL } from "url";

export default class ClangPlugin {
  static clangPluginDir = "clang-plugin-binaries";
  static pluginNamePrefix = "ClangASTDumper";

  /**
   * Gets the clang executable name from a compilation command.
   *
   * **Sanitize the input before calling this method**
   *
   * @param executionCommand String containing the command the user uses to compile their code normally
   * @returns String containing the clang executable name
   */
  static getClangExecutable(executionCommand: string): string {
    const commandRegex = /(?:^|\s)(\S*clang\S*)(?=\s|$)/;
    const clangExecutable = executionCommand.match(commandRegex);
    if (!clangExecutable) {
      throw new Error("Could not find clang executable");
    }
    return clangExecutable[1];
  }

  /**
   * Gets the clang version number from a clang executable
   *
   * **Sanitize the input before calling this method**
   *
   * @param clangExecutable String containing the clang executable name
   * @returns String containing the clang version number (e.g. 14.0.0)
   */
  static getClangVersionNumberFromExecutable(clangExecutable: string): string {
    const output = Sandbox.executeSandboxedCommand(
      clangExecutable,
      "--version"
    );

    const versionRegex = /clang version (\d+\.\d+\.\d+)/;
    const version = output.match(versionRegex);
    if (!version) {
      throw new Error("Could not find clang version");
    }
    return version[1];
  }

  /**
   * Gets the clang version number from a compilation command
   *
   * **Sanitize the input before calling this method**
   *
   * @param executionCommand String containing the command the user uses to compile their code normally
   * @returns String containing the clang version number (e.g. 14.0.0)
   */
  static getClangVersion(executionCommand: string): string {
    const clangExecutable = this.getClangExecutable(executionCommand);
    return this.getClangVersionNumberFromExecutable(clangExecutable);
  }

  /**
   *  Searches the 'clang-plugin-binaries' directory for available clang plugins
   *  and returns a map of the available plugins.
   *
   * @returns Map of available clang plugins
   */
  static getAvailablePlugins(): { [version: string]: string } {
    const basedir = path.dirname(
      path.dirname(path.dirname(new URL(import.meta.url).pathname))
    );
    const dir = path.join(basedir, this.clangPluginDir);

    const regexPattern = new RegExp(
      `${this.pluginNamePrefix}_(\\d+\\.\\d+\\.\\d+)\\.so`
    );

    if (!fs.existsSync(dir)) {
      throw new Error("Could not find 'clang-plugin-binaries' directory");
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
    return map;
  }

  /**
   * Gets the absolute path to the clang plugin for the compiler used in a compilation command
   *
   * @param command Command used to execute the compiler
   * @returns The absolute path to the clang plugin or throws an error if no compatible plugin found
   */
  static getPluginPath(command: string): string {
    const sanitizedCommand = Sandbox.sanitizeCommand(command);
    const clangVersion = this.getClangVersion(sanitizedCommand);

    const availablePlugins = this.getAvailablePlugins();

    if (clangVersion in availablePlugins) {
      return availablePlugins[clangVersion];
    } else {
      throw new Error("Could not find plugin for clang version");
    }
  }

  /**
   * Executes the clang plugin in a sandboxed environment with the given arguments
   * and returns a pipe to the output.
   *
   * @param command Command used to execute the compiler by the user
   * @param pluginPath Path to the clang plugin aligned with the compiler version
   * @param args Arguments to pass to the clang plugin
   */
  static executeClangPlugin(
    command: string,
    pluginPath: string,
    ...args: string[]
  ) {}
}

import { ChildProcess, spawn, spawnSync } from "child_process";
import { addActiveChildProcess } from "lara-js/dist/ChildProcessHandling.js";

export default class Sandbox {
  /**
   * Executes a command in a restricted environment and returns the output as a string
   *
   * **Sanitize the input before calling this method**
   *
   * @param command - String containing the command to be executed
   * @param args - Array of strings containing the arguments to be passed to the command
   * @param env - Object containing the environment variables to be set for the command
   * @returns Output of the command as a string
   */
  static executeSandboxedCommand(
    command: string,
    args: string[] = [],
    env: Map<string, string> = new Map()
  ): string {
    // Set up a new child process with a restricted environment
    const child = spawnSync(command, args, {
      env: {
        PATH: "/usr/local/bin:/usr/bin:/bin", // Set a limited PATH environment variable
        HOME: "/tmp", // Set a limited HOME environment variable
        USER: "sandbox", // Set a limited user for the child process
        ...Object.fromEntries(env),
      },
      cwd: "/tmp", // Set a restricted current working directory
      stdio: ["ignore", "pipe", "ignore", "ignore"],
    });

    if (child.error) {
      throw new Error("Invalid executable");
    } else if (child.status !== 0) {
      throw new Error(
        `Command exited with status ${String(
          child.status
        )}: ${child.stderr.toString()}`
      );
    }

    // Return the output of the child process as a string
    return child.stdout.toString();
  }

  /**
   * Executes a command in a restricted environment and returns the output as a string
   *
   * **Sanitize the input before calling this method**
   *
   * @param command - String containing the command to be executed
   * @param args - Array of strings containing the arguments to be passed to the command
   * @param env - Object containing environment variables to be set for the child process
   * @returns Output of the command as a string
   */
  static executeSandboxedCommandAsync(
    command: string,
    args: string[] = [],
    env: Record<string, unknown> = {}
  ): ChildProcess {
    // Set up a new child process with a restricted environment
    const child = spawn(command, args, {
      env: {
        PATH: "/usr/local/bin:/usr/bin:/bin", // Set a limited PATH environment variable
        HOME: "/tmp", // Set a limited HOME environment variable
        USER: "sandbox", // Set a limited user for the child process
        ...env,
      },
      cwd: "/tmp", // Set a restricted current working directory
      stdio: ["ignore", "pipe", "ignore", "ignore"],
    });
    addActiveChildProcess(child);

    return child;
  }

  /**
   * Ensure command string does not contain any characters that could be used
   *
   * @param command - String containing the command to be sanitized
   * @returns The original command or throws an error if invalid
   */
  static sanitizeCommand(
    command: (string | number)[]
  ): Promise<(string | number)[]> {
    return new Promise((resolve, reject) => {
      /**
       * This regular expression, /^[^;&|]*$/, is a pattern that matches any
       * string that does not contain certain characters often used in shell
       * commands. Specifically, it matches strings that do not contain
       * semicolons (;), ampersands (&), or vertical bars (|).
       */
      const pattern = /^[^;&|]*$/;

      if (!pattern.test(command.join(" "))) {
        reject(new Error("Invalid command"));
      }

      resolve(command);
    });
  }

  static splitCommandArgsEnv(
    commandArgs: (string | number)[]
  ): Promise<[string, (string | number)[], string[]]> {
    return new Promise((resolve) => {
      // regex to match environment variable declarations
      const envRegex = /^([a-zA-Z_][a-zA-Z0-9_]*)=(.*)$/;

      // filter environment variables
      const env = commandArgs.filter(
        (arg) => typeof arg === "string" && envRegex.test(arg)
      ) as string[];

      // filter command and arguments
      const commandArgsWithoutEnv = commandArgs.filter(
        (arg) => !env.includes(String(arg))
      );

      // take the first argument as the command
      const commandCandidate = commandArgsWithoutEnv.shift();
      const command = commandCandidate ? String(commandCandidate) : "";

      resolve([command, commandArgsWithoutEnv, env]);
    });
  }
}

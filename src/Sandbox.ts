import { spawnSync, spawn } from "child_process";

export default class Sandbox {
  /**
   * Executes a command in a restricted environment and returns the output as a string
   *
   * **Sanitize the input before calling this method**
   *
   * @param command String containing the command to be executed
   * @param args Array of strings containing the arguments to be passed to the command
   * @returns Output of the command as a string
   */
  static executeSandboxedCommand(command: string, ...args: string[]): string {
    // Set up a new child process with a restricted environment
    const child = spawnSync(command, [...args], {
      env: {
        PATH: "/usr/local/bin:/usr/bin:/bin", // Set a limited PATH environment variable
        HOME: "/tmp", // Set a limited HOME environment variable
        USER: "sandbox", // Set a limited user for the child process
      },
      cwd: "/tmp", // Set a restricted current working directory
      stdio: ["ignore", "pipe", "ignore", "ignore"],
    });

    // Return the output of the child process as a string
    return child.stdout.toString();
  }

  static executeSandboxedCommandAsync(
    command: string,
    ...args: string[]
  ): Promise<string> {
    return new Promise((resolve, reject) => {
      const child = spawn(command, [...args], {
        env: {
          PATH: "/usr/local/bin:/usr/bin:/bin", // Set a limited PATH environment variable
          HOME: "/tmp", // Set a limited HOME environment variable
          USER: "sandbox", // Set a limited user for the child process
        },
        cwd: "/tmp", // Set a restricted current working directory
        stdio: ["ignore", "pipe", "ignore", "ignore"],
      });
    });
  }

  /**
   * Ensure command string does not contain any characters that could be used
   *
   * @param command String containing the command to be sanitized
   * @returns The original command or throws an error if invalid
   */
  static sanitizeCommand(command: string): string {
    /**
     * This regular expression, /^[^;&|]*$/, is a pattern that matches any
     * string that does not contain certain characters often used in shell
     * commands. Specifically, it matches strings that do not contain
     * semicolons (;), ampersands (&), or vertical bars (|).
     */
    const pattern = /^[^;&|]*$/;

    if (!pattern.test(command)) {
      throw new Error("Invalid command");
    }

    return command;
  }
}

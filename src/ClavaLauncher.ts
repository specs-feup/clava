import Debug from "debug";
import * as path from "path";
import * as chokidar from "chokidar";
import { OptionValues } from "commander";
import { ChildProcess, fork } from "child_process";

const debug = Debug("clava:main");
let activeProcesses: Array<ChildProcess> = [];

function capitalizeFirstLetter(string: string) {
  return string.charAt(0).toUpperCase() + string.slice(1);
}

export async function main(args: OptionValues) {
  debug("Clava execution arguments: %O", args);
  executeClava(args);

  if (args.watch) {
    for (const directory of args.watch) {
      debug("Watching directory: " + directory);
    }

    chokidar
      .watch(args.watch, { ignoreInitial: true })
      .on("all", (event: string, filepath: string) => {
        try {
          return filesystemEventHandler(event, filepath, args);
        } catch (error) {
          console.error(error);
        }
      });
  }
}

function filesystemEventHandler(
  event: string,
  filepath: string,
  args: OptionValues
) {
  debug(`Source file event: ${capitalizeFirstLetter(event)} '${filepath}'`);
  executeClava(args);
}

function executeClava(args: OptionValues) {
  const activeProcess = activeProcesses.shift();
  if (activeProcess?.exitCode === null) {
    if (activeProcess.kill("SIGKILL")) {
      debug("Killed active process");
    } else {
      throw new Error("Could not kill active process");
    }
  }

  activeProcesses.push(fork(path.join("dist", "Clava.js"), args));
}

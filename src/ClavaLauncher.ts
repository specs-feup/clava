import Debug from "debug";
import * as path from "path";
import * as chokidar from "chokidar";
import { fork } from "child_process";
import {
  addActiveChildProcess,
  activeChildProcesses,
  listenForTerminationSignals,
} from "./ChildProcessHandling.js";

listenForTerminationSignals();

const debug = Debug("clava:main");

function capitalizeFirstLetter(string: string) {
  return string.charAt(0).toUpperCase() + string.slice(1);
}

export function main(args: Record<string, unknown>): void {
  debug("Clava execution arguments: %O", args);
  void executeClava(args);

  if (args.watch) {
    for (const directory of args.watch as string[]) {
      debug(`Watching directory: ${directory}`);
    }

    chokidar
      .watch(args.watch as string[], { ignoreInitial: true })
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
  args: Record<string, unknown>
) {
  debug(`Source file event: ${capitalizeFirstLetter(event)} '${filepath}'`);
  void executeClava(args);
}

let midExecution = false;
async function executeClava(args: Record<string, unknown>) {
  if (midExecution) return;
  midExecution = true;
  const activeProcess = Object.values(activeChildProcesses)[0];

  if (activeProcess?.exitCode === null) {
    const promise = new Promise((resolve) => {
      activeProcess.once("exit", (code) => {
        resolve(code);
      });
    });
    if (activeProcess.kill()) {
      await promise;
      debug("Killed active process");
    } else {
      throw new Error("Could not kill active process");
    }
  }

  addActiveChildProcess(
    fork(path.join("dist", "Clava.js"), [JSON.stringify(args)])
  );
  midExecution = false;
}

import Debug from "debug";
import * as path from "path";
import * as chokidar from "chokidar";
import { ChildProcess, fork } from "child_process";
import {
  addActiveChildProcess,
  activeChildProcesses,
} from "./ChildProcessHandling.js";

const debug = Debug("clava:main");

function capitalizeFirstLetter(string: string) {
  return string.charAt(0).toUpperCase() + string.slice(1);
}

export async function main(args: { [key: string]: any }) {
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
  args: { [key: string]: any }
) {
  debug(`Source file event: ${capitalizeFirstLetter(event)} '${filepath}'`);
  executeClava(args);
}

let midExecution = false;
async function executeClava(args: { [key: string]: any }) {
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

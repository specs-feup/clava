import Debug from "debug";
import * as path from "path";
import * as chokidar from "chokidar";
import { ChildProcess, fork } from "child_process";
import { Config } from "./index.js";

const debug = Debug("clava:main");
let activeProcesses: Array<ChildProcess> = [];

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

async function executeClava(args: { [key: string]: any }) {
  const activeProcess = activeProcesses.shift();
  if (activeProcess?.exitCode === null) {
    const promise = new Promise((resolve) => {
      activeProcess.once("exit", (code) => {
        resolve(code);
      });
    });
    if (activeProcess.kill("SIGKILL")) {
      await promise;
      debug("Killed active process");
    } else {
      throw new Error("Could not kill active process");
    }
  }

  activeProcesses.push(
    fork(path.join("dist", "Clava.js"), [JSON.stringify(args)])
  );
}

// Kill all active processes on exit
process.on("SIGINT", async () => {
  for (const activeProcess of activeProcesses) {
    const promise = new Promise((resolve) => {
      activeProcess.once("exit", (code) => {
        resolve(code);
      });
    });
    activeProcess.kill("SIGKILL");
    await promise;
  }

  process.exit(0);
});

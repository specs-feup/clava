import { fork, spawn } from "child_process";

export const activeChildProcesses: Record<
  number,
  ReturnType<typeof fork> | ReturnType<typeof spawn>
> = {};

export function addActiveChildProcess(
  child: ReturnType<typeof fork> | ReturnType<typeof spawn>
) {
  activeChildProcesses[child.pid!] = child;

  // Listen for the 'exit' event of the child process
  child.on("exit", (code, signal) => {
    delete activeChildProcesses[child.pid!];
  });
}

export async function handleExit() {
  const closingChildren: Promise<any>[] = [];

  for (const child of Object.values(activeChildProcesses)) {
    const promise = new Promise((resolve) => {
      child.once("exit", (code) => {
        resolve(code);
      });
    });
    closingChildren.push(promise);

    child.kill();
  }
  await Promise.all(closingChildren);
  console.log("Closed all child processes");
  process.exit();
}

// Listen for termination signals
process.on("SIGINT", handleExit);
process.on("SIGTERM", handleExit);
process.on("SIGQUIT", handleExit);
process.on("uncaughtException", handleExit);
process.on("unhandledRejection", handleExit);

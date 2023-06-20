import AbstractDumper from "./AbstractDumper.js";

import { spawn } from "child_process";

export default class ClangJSONDumper implements AbstractDumper {
  async dump(): Promise<Record<string, unknown>> {
    const child = spawn(
      "clang -Xclang -ast-dump=json CxxTestSources/test1/main.cpp",
      { stdio: ["ignore", "pipe", "inherit"], windowsHide: true, shell: true }
    );

    child.on("error", (error) => {
      console.error(`Error: ${error.message}`);
    });

    child.on("close", (code) => {
      console.log(`Parsing complete. Process exited with code ${String(code)}`);
    });

    let data = "";
    for await (const chunk of child.stdout) {
      data += chunk;
    }

    return JSON.parse(data) as Record<string, unknown>;
  }
}

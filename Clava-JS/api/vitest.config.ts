import { defineProject } from "vitest/config";
import path from "node:path";
import { fileURLToPath } from "node:url";

const root = path.dirname(fileURLToPath(import.meta.url));

export default defineProject({
  root,
  test: {
    experimental: {
      viteModuleRunner: false,
    },
    fileParallelism: false,
    globals: true,
    name: "api",
    pool: "forks",
    setupFiles: ["../vitest/setupClavaWeaver.ts"],
  },
});

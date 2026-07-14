import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    coverage: {
      include: ["**/*[^.d].(t|j)s"],
      provider: "v8",
      reporter: ["text", "lcov"],
    },
    maxWorkers: 1,
    projects: ["api/vitest.config.ts", "code/vitest.config.ts"],
  },
});

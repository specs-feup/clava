import { defineConfig } from "jest";

export default defineConfig({
  preset: "ts-jest/presets/default-esm",
  testEnvironment: "node",
  notify: true,
  notifyMode: "always",
  //verbose: true,
  collectCoverage: false,
  coverageDirectory: "coverage",
  coverageReporters: ["text", "lcov"],
  collectCoverageFrom: ["**/*[^.d].(t|j)s"],
  coverageProvider: "v8",
  moduleNameMapper: {
    "(.+)\\.js": "$1",
  },
  projects: ["api", "code"],
});

import { defineConfig } from "jest";
import { weaverConfig } from "../code/WeaverConfiguration.ts";

export default defineConfig({
  preset: "ts-jest/presets/default-esm",
  testEnvironment: "@specs-feup/lara/jest/jestEnvironment.ts",
  globalSetup: "<rootDir>/../jest/jestGlobalSetup.ts",
  globalTeardown: "<rootDir>/../jest/jestGlobalTeardown.ts",
  setupFiles: ["@specs-feup/lara/jest/setupFiles/sharedJavaModule.ts"],
  setupFilesAfterEnv: ["@specs-feup/lara/jest/setupFiles/importSideEffects.ts"],
  moduleNameMapper: {
    "(.+)\\.js": "$1",
  },
  testEnvironmentOptions: {
    weaverConfig,
  },
});

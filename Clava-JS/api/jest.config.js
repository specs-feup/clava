import { weaverConfig } from "../code/WeaverConfiguration.ts";

const config = {
  preset: "ts-jest/presets/default-esm",
  testEnvironment: "@specs-feup/lara/jest/jestEnvironment.js",
  globalSetup: "@specs-feup/lara/jest/jestGlobalSetup.js",
  globalTeardown: "@specs-feup/lara/jest/jestGlobalTeardown.js",
  setupFiles: ["@specs-feup/lara/jest/setupFiles/sharedJavaModule.js"],
  setupFilesAfterEnv: ["@specs-feup/lara/jest/setupFiles/importSideEffects.js"],
  moduleNameMapper: {
    "(.+)\\.js": "$1",
  },
  testEnvironmentOptions: {
    weaverConfig,
  },
};

export default config;

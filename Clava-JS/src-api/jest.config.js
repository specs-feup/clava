import { weaverConfig } from "../code/WeaverConfiguration.js";

const config = {
  preset: "ts-jest/presets/default-esm",
  testEnvironment: "lara-js/jest/jestEnvironment.js",
  globalSetup: "lara-js/jest/jestGlobalSetup.js",
  globalTeardown: "lara-js/jest/jestGlobalTeardown.js",
  setupFiles: ["lara-js/jest/setupFiles/sharedJavaModule.js"],
  moduleNameMapper: {
    "(.+)\\.js": "$1",
  },
  testEnvironmentOptions: {
    weaverConfig,
  },
};

export default config;

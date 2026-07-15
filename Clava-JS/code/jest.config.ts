import { defineConfig } from "jest";

export default defineConfig({
  preset: "ts-jest/presets/default-esm",
  testEnvironment: "node",
  moduleNameMapper: {
    "(.+)\\.js": "$1",
  },
});

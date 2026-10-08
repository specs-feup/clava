import { createWeaverVitestConfig } from "@specs-feup/lara/vitest/weaverVitestConfig.ts";
import { defineConfig, mergeConfig } from "vitest/config";
import { weaverConfig } from "./code/WeaverConfiguration.ts";

export default mergeConfig(
  createWeaverVitestConfig(weaverConfig),
  defineConfig({ test: { exclude: ["**/dist/**"] } }),
);

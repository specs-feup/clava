import { createWeaverVitestConfig } from "@specs-feup/lara/vitest/weaverVitestConfig.ts";
import { weaverConfig } from "../../../Clava-JS/code/WeaverConfiguration.ts";
import { fileURLToPath } from "node:url";

const jarPath = process.env.CLAVA_SUITE_JAR_PATH ?? weaverConfig.jarPath;
const sourceRoot = process.env.CLAVA_SUITE_SOURCE_ROOT
  ?? fileURLToPath(new URL("../../../Clava-JS/", import.meta.url));

export default {
  ...createWeaverVitestConfig({
    ...weaverConfig,
    jarPath,
  }),
  root: sourceRoot,
};

import { createWeaverVitestConfig } from "@specs-feup/lara/vitest/weaverVitestConfig.ts";
import { weaverConfig } from "../../../Clava-JS/code/WeaverConfiguration.ts";
import { fileURLToPath } from "node:url";

const jarPath = process.env.CLAVA_SUITE_JAR_PATH ?? weaverConfig.jarPath;

export default {
  ...createWeaverVitestConfig({
    ...weaverConfig,
    jarPath,
  }),
  root: fileURLToPath(new URL("../../../Clava-JS/", import.meta.url)),
};

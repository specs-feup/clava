import { createWeaverVitestConfig } from "@specs-feup/lara/vitest/weaverVitestConfig.ts";
import { weaverConfig } from "./code/WeaverConfiguration.ts";

export default createWeaverVitestConfig(weaverConfig, {
  javaOptionsEnvironmentVariable: "CLAVA_JS_JAVA_OPTIONS",
});

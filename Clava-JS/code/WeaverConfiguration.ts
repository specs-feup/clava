import type WeaverConfiguration from "@specs-feup/lara/code/WeaverConfiguration.ts";
import path from "path";
import { fileURLToPath } from "url";

export const weaverConfig: WeaverConfiguration = {
  weaverName: "clava",
  weaverPrettyName: "Clava",
  weaverFileName: "@specs-feup/lara/code/Weaver.ts",
  jarPath: path.join(
    path.dirname(path.dirname(fileURLToPath(import.meta.url))),
    "./java-binaries/"
  ),
  javaWeaverQualifiedName: "pt.up.fe.specs.clava.weaver.CxxWeaver",
  importForSideEffects: ["@specs-feup/clava/api/Joinpoints.ts", "@specs-feup/clava/code/sideEffects.ts"],
};

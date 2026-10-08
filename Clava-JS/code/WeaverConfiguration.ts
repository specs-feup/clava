import type WeaverConfiguration from "@specs-feup/lara/code/WeaverConfiguration.ts";
import path from "path";
import { fileURLToPath } from "url";

const moduleDirectory = path.dirname(fileURLToPath(import.meta.url));
const packageDirectory =
  path.basename(path.dirname(moduleDirectory)) === "dist"
    ? path.dirname(path.dirname(moduleDirectory))
    : path.dirname(moduleDirectory);

export const weaverConfig: WeaverConfiguration = {
  weaverName: "clava",
  weaverPrettyName: "Clava",
  weaverFileName: "@specs-feup/lara/code/Weaver.ts",
  jarPath: path.join(packageDirectory, "java-binaries"),
  javaWeaverQualifiedName: "pt.up.fe.specs.clava.weaver.CxxWeaver",
  importForSideEffects: ["@specs-feup/clava/api/Joinpoints.ts", "@specs-feup/clava/code/sideEffects.ts"],
};

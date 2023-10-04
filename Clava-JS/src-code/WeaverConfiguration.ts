import WeaverConfiguration from "lara-js/code/WeaverConfiguration.js";
import path from "path";
import { fileURLToPath, pathToFileURL } from "url";

export const weaverConfig: WeaverConfiguration = {
  weaverName: "clava",
  weaverPrettyName: "Clava",
  weaverFileName: "Weaver.js",
  jarFilePath: pathToFileURL(
    path.join(
      path.dirname(path.dirname(fileURLToPath(import.meta.url))),
      "./java-binaries/ClavaWeaver.jar"
    )
  ).toString(),
  javaWeaverQualifiedName: "pt.up.fe.specs.clava.weaver.CxxWeaver",
};

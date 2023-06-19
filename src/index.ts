#!/usr/bin/env node
import WeaverLauncher from "lara-js/dist/WeaverLauncher.js";
import WeaverConfiguration from "lara-js/dist/WeaverConfiguration.js";

const weaverConfig: WeaverConfiguration = {
  weaverName: "clava",
  weaverPrettyName: "Clava",
  weaverFileName: "Weaver.js",
  jarFilePath: "../../../SPeCS/ClavaWeaver.jar",
  javaWeaverQualifiedName: "pt.up.fe.specs.clava.weaver.CxxWeaver",
};

const weaverLauncher = new WeaverLauncher(weaverConfig);

await weaverLauncher.execute();

#!/usr/bin/env node
import WeaverLauncher from "lara-js/WeaverLauncher.js";
import WeaverConfiguration from "lara-js/WeaverConfiguration.js";

const weaverConfig: WeaverConfiguration = {
  weaverName: "clava",
  weaverPrettyName: "Clava",
  weaverFileName: "Weaver.js",
  jarFilePath: "../../../SPeCS/ClavaWeaver.jar",
  javaWeaverQualifiedName: "pt.up.fe.specs.clava.weaver.CxxWeaver",
};

const weaverLauncher = new WeaverLauncher(weaverConfig);

await weaverLauncher.execute();

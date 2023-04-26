// @ts-ignore
import java from "java";
import Debug from "debug";
import { OptionValues } from "commander";
import { promisify } from "util";

const debug = Debug("clava:weaver");
java.asyncOptions = {
  asyncSuffix: undefined,
  syncSuffix: "Sync",
  promiseSuffix: "",
  promisify: promisify,
};
java.classpath.push("../../../.local/bin/Clava/Clava.jar");

const args: OptionValues = process.argv.slice(2);

debug("Clava execution arguments: %O", args);

await java.callStaticMethod(
  "pt.up.fe.specs.clava.weaver.ClavaWeaverLauncher",
  "main",
  [""]
);

process.exit(0);

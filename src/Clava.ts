// @ts-ignore
import java from "java";
import Debug from "debug";
import { promisify } from "util";
import { hideBin } from "yargs/helpers";

const debug = Debug("clava:weaver");
java.asyncOptions = {
  asyncSuffix: undefined,
  syncSuffix: "Sync",
  promiseSuffix: "",
  promisify: promisify,
};
java.classpath.push("../../../.local/bin/Clava/Clava.jar");

const args: { [key: string]: any } = hideBin(process.argv);

debug("Clava execution arguments: %O", args);

await java.callStaticMethod(
  "pt.up.fe.specs.clava.weaver.ClavaWeaverLauncher",
  "main",
  [""]
);

process.exit(0);

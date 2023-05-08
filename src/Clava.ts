import * as fs from "fs";
import * as path from "path";
// @ts-ignore
import java from "java";
import Debug from "debug";
import { promisify } from "util";
import { isValidFileExtension } from "./FileExtensions.js";
import { hideBin } from "yargs/helpers";
import ClangPlugin from "./ClangPlugin/ClangPlugin.js";

const debug = Debug("clava:weaver");
const args: { [key: string]: any } = JSON.parse(hideBin(process.argv)[0]);

java.asyncOptions = {
  asyncSuffix: "",
  syncSuffix: "Sync",
  promiseSuffix: "P",
  promisify: promisify,
};
java.classpath.push("../../../.local/bin/Clava/Clava.jar");
await java.ensureJvm();

debug("Clava execution arguments: %O", args);

/*
const javaClass = await java.import(
  "pt.up.fe.specs.clava.weaver.ClavaWeaverLauncher"
);

await javaClass.mainP(["testscript.js"]);
*/

const javaLangSystem = java.import("java.lang.System");
await javaLangSystem.out.printlnP("JAVA Code execution...");

if (
  fs.existsSync(args.scriptFile) &&
  isValidFileExtension(path.extname(args.scriptFile))
) {
  await import(path.resolve(args.scriptFile))
    .then(() => {
      debug("Execution completed successfully.");
    })
    .catch((error) => {
      debug(error);
    });
} else {
  console.error("Invalid file path or file type.");
}

const out = await ClangPlugin.executeClangPlugin(args._);
console.log(out);

debug("Exiting...");
process.exit(0);

import * as fs from "fs";
import * as path from "path";
// @ts-ignore
import java from "java";
import Debug from "debug";
import { promisify } from "util";
import { isValidFileExtension } from "./FileExtensions.js";
import { hideBin } from "yargs/helpers";
import ClangPlugin from "./ClangPlugin/ClangPlugin.js";
import Sandbox from "./Sandbox.js";

const debug = Debug("clava:weaver");
const args: { [key: string]: any } = JSON.parse(hideBin(process.argv)[0]);

java.asyncOptions = {
  asyncSuffix: "Async",
  syncSuffix: "",
  promiseSuffix: "P",
  promisify: promisify,
};
java.classpath.push("../../../.local/bin/Clava/Clava.jar");
await java.ensureJvm();

debug("Clava execution arguments: %O", args);

const JavaArrayList = java.import("java.util.ArrayList");
const JavaFile = java.import("java.io.File");
const JavaCxxWeaver = java.import("pt.up.fe.specs.clava.weaver.CxxWeaver");
const JavaLaraIDataStore = java.import(
  "org.lara.interpreter.joptions.config.interpreter.LaraIDataStore"
);
const JavaDataStore = java.import(
  "org.suikasoft.jOptions.Interfaces.DataStore"
);
const LaraiKeys = java.import(
  "org.lara.interpreter.joptions.config.interpreter.LaraiKeys"
);

const fileList = new JavaArrayList();
//const [command, clangArgs, env] = await Sandbox.splitCommandArgsEnv(args._[1]);
const clangArgs = args._.slice(1);
clangArgs.forEach((arg: string | number) => {
  fileList.add(new JavaFile(arg));
});

const weaver = new JavaCxxWeaver();
weaver.setWeaver();

const datastore = await new JavaDataStore.newInstanceP("CxxWeaverDataStore");
datastore.set(LaraiKeys.LARA_FILE, new JavaFile("placeholderFileName"));

const laraIDataStore = new JavaLaraIDataStore(null, datastore, weaver);
weaver.begin(
  fileList,
  new JavaFile(JavaCxxWeaver.getWovenCodeFoldername()),
  laraIDataStore.getWeaverArgs()
);

console.log("AST: %O", weaver.getRootJp().getAst().root);

Object.defineProperty(globalThis, "clava", {
  value: {
    rootJp: weaver.getRootJp(),
  },
  enumerable: false,
  configurable: true,
  writable: true,
});

console.log("Hi");

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

/*const out = await ClangPlugin.executeClangPlugin(args._);
console.log(out); */

weaver.close();

debug("Exiting...");
process.exit(0);

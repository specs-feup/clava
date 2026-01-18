import JavaTypes from "@specs-feup/lara/api/lara/util/JavaTypes.js";
import Weaver from "@specs-feup/lara/api/weaver/Weaver.js";

import path from "node:path";
import os from "node:os";
import pkg from "../package.json" with { type: "json" };

const CxxWeaverOptions = JavaTypes.getType(
  "pt.up.fe.specs.clava.weaver.options.CxxWeaverOption"
);

const CodeParser = JavaTypes.getType(
  "pt.up.fe.specs.clang.codeparser.CodeParser"
);

const datastore = Weaver.getWeaverEngine().getData().get();

datastore.set(CxxWeaverOptions.DISABLE_CLAVA_INFO, true);
datastore.set(
  CodeParser.DUMPER_FOLDER,
  new JavaTypes.File(getVersionedCacheDir())
);

/** Code to obtain temporary folder **/

function getVersionedCacheDir(): string {
  // Use name+version to isolate different installed versions
  return path.join(getCacheBaseDir(), pkg.name, pkg.version);
}

function getCacheBaseDir(): string {
  // OS conventions
  if (process.platform === "win32") {
    return (
      process.env.LOCALAPPDATA || path.join(os.homedir(), "AppData", "Local")
    );
  }
  if (process.platform === "darwin") {
    return path.join(os.homedir(), "Library", "Caches");
  }
  // Linux / others
  return process.env.XDG_CACHE_HOME || path.join(os.homedir(), ".cache");
}

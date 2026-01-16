import JavaTypes from "@specs-feup/lara/api/lara/util/JavaTypes.js";
import Weaver from "@specs-feup/lara/api/weaver/Weaver.js";

import path from "node:path";
import { fileURLToPath } from "node:url";
import fs from "node:fs";
import os from "node:os";

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

export function getVersionedCacheDir(): string {
  const pkg = readSelfPackageJson();

  // Use name+version to isolate different installed versions
  const dir = path.join(getCacheBaseDir(), pkg.name, pkg.version);

  fs.mkdirSync(dir, { recursive: true });

  return dir;
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

function readSelfPackageJson(): { name: string; version: string } {
  const here = path.dirname(fileURLToPath(import.meta.url));
  let dir = here;

  while (true) {
    const candidate = path.join(dir, "package.json");
    if (fs.existsSync(candidate)) {
      return JSON.parse(fs.readFileSync(candidate, "utf8"));
    }
    const parent = path.dirname(dir);
    if (parent === dir)
      throw new Error("Could not find package.json above " + here);
    dir = parent;
  }
}

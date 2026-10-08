import assert from "node:assert/strict";
import { existsSync } from "node:fs";
import { spawnSync } from "node:child_process";
import path from "node:path";
import { fileURLToPath } from "node:url";

const apiSubpaths = [
  "@specs-feup/clava/api/clava/graphs/cfg/CfgNodeType.ts",
  "@specs-feup/clava/api/clava/graphs/cfg/CfgNodeType.js",
  "@specs-feup/clava/api/clava/graphs/cfg/CfgNodeType",
];

for (const subpath of apiSubpaths) {
  const { default: CfgNodeType } = await import(subpath);
  assert.equal(CfgNodeType.START.name, "START");
}

const configurationSubpath = "@specs-feup/clava/code/WeaverConfiguration.ts";
const configurationUrl = import.meta.resolve(configurationSubpath);
const { weaverConfig } = await import(configurationSubpath);
const expectedJavaBinaries = fileURLToPath(new URL("../../java-binaries/", configurationUrl));

assert.equal(path.resolve(weaverConfig.jarPath), path.resolve(expectedJavaBinaries));
assert.ok(existsSync(path.join(weaverConfig.jarPath, "lib")));
assert.ok(
  existsSync(
    fileURLToPath(
      import.meta
        .resolve("@specs-feup/clava/api/clava/graphs/cfg/CfgNodeType.ts")
        .replace(/\.js$/, ".d.ts"),
    ),
  ),
  "the package includes declarations beside emitted API JavaScript",
);

const cliPath = fileURLToPath(import.meta.resolve("@specs-feup/clava/code/index.ts"));
const cli = spawnSync(process.execPath, [cliPath, "--help"], {
  encoding: "utf8",
});

assert.equal(cli.status, 0, cli.stderr);
assert.match(cli.stdout, /Execute a Clava script/);

console.log("Clava package runtime paths passed");

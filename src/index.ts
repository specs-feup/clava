import { program, Option, Command } from "commander";
//import { program, Option, Command } from "@commander-js/extra-typings";
import { main } from "./Clava.js";
import { cosmiconfigSync } from "cosmiconfig";
import { TypeScriptLoader } from "cosmiconfig-typescript-loader";

const moduleName = "clava";
const explorer = cosmiconfigSync(moduleName, {
  searchPlaces: [
    "package.json",
    `.${moduleName}rc`,
    `.${moduleName}rc.json`,
    `.${moduleName}rc.yaml`,
    `.${moduleName}rc.yml`,
    `.${moduleName}rc.js`,
    `.${moduleName}rc.ts`,
    `.${moduleName}rc.cjs`,
    `${moduleName}.config.js`,
    `${moduleName}.config.ts`,
    `${moduleName}.config.cjs`,
  ],
  loaders: {
    ".ts": TypeScriptLoader(),
  },
});

program
  .name(moduleName)
  .addOption(
    new Option(
      "-w, --watch <directory...>",
      "Watch the following directory for changes"
    ).default([])
  )
  .addOption(new Option("-c, --config <config>", "Path to config file"))
  //.addCommand(new Command("init").description("Initialize a new clava project"))
  //.addCommand(new Command("run").description("Run clava"))
  .showHelpAfterError()
  .parse();

const config = program.opts();
let configFromFile;
if (config.config) {
  try {
    configFromFile = explorer.load(config.config);
  } catch (error) {
    console.error("Error: Configuration file not found");
    process.exit(1);
  }
} else {
  configFromFile = explorer.search();
}
config.config = configFromFile?.filepath;

if (configFromFile?.hasOwnProperty("config")) {
  for (const [key, value] of Object.entries(config)) {
    config[key] = configFromFile.config[key] || value;
  }
}

await main(config);

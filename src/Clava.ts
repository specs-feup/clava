import { OptionValues } from "commander";
import * as fs from "fs";
import { ClangJSONDumper } from "./Dumper/index.js";

export async function main(args: OptionValues) {
  console.log(args);
  const ast = await new ClangJSONDumper().dump();
  console.log(ast);
  return;
  /*
  fs.watch(args[0], (event: string, filename: string) => {
    console.log("event is: " + event);
    if (filename) {
      console.log("filename provided: " + filename);
    } else {
      console.log("filename not provided");
    }
  });
  */
}

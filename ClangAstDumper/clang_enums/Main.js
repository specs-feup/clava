#!/usr/bin/env node
import fs from "fs";
import path from "path";
import { Headers } from "./ClangEnums.js";

// Get the command line arguments
const llvmVersion = process.argv[2];
const outputFolder = process.argv[3];

const compilerCmd = `clang++-${llvmVersion}`;
const llvmFolder = `/usr/lib/llvm-${llvmVersion}/lib/cmake/llvm`;
const javaEnumsOutputFolder = path.join(outputFolder, "java_enums");

console.log("Using LLVM folder '" + llvmFolder + "'");
console.log("Generating enums to folder '" + outputFolder + "'");

fs.mkdirSync(javaEnumsOutputFolder, {recursive: true});

for (const header of Headers) {
  header.process(compilerCmd, llvmFolder);
  header.generateCode(outputFolder);
  header.generateJavaEnums(javaEnumsOutputFolder);
}

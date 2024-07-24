import { EventEmitter } from "events";
import * as fs from "fs";
import Clava from "../Clava.js";
const { gitCommand } = require('./gitCommand.js');

const eventListener = new EventEmitter();

let idx = 0;

eventListener.on("storeAST", () => {
  console.log(`Waypoint ${idx}`);
  fs.writeFileSync(`history/waypoint_${idx}.cpp`, Clava.getProgram().code);
  idx++;
  gitCommand();
});

export default eventListener;

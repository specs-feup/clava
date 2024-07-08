import { EventEmitter } from "events";
import * as fs from "fs";
import Clava from "../Clava.js";

const eventListener = new EventEmitter();

let idx = 0;

eventListener.on("storeAST", (data: string) => {
  console.log(`Waypoint ${idx}`);
  fs.writeFileSync(`history/waypoint_${idx}.txt`, Clava.getProgram().code);
  idx++;
});

export default eventListener;

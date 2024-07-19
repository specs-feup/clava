import { EventEmitter } from "events";
import * as fs from "fs";
import Clava from "../Clava.js";
import { Event } from "./Events.js";

const eventListener = new EventEmitter();

let idx = 0;

eventListener.on("storeAST", () => {
  console.log(`Waypoint ${idx}`);
  fs.writeFileSync(`history/waypoint_${idx}.cpp`, Clava.getProgram().code);
  idx++;
});
eventListener.on("ACTION", (e: Event) => {
  console.log(e.timing);
  console.log(e.description);
  console.log(e.mainJP);
  console.log(e.returnJP);
});

export default eventListener;

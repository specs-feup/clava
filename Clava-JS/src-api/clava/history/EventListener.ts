import { EventEmitter } from "events";
import * as fs from "fs";
import Clava from "../Clava.js";
import { Event, EventTime } from "./Events.js";
import ophistory from "./History.js";
import { InsertOperation } from "./Operations.js";

const eventListener = new EventEmitter();

let idx = 0;

eventListener.on("storeAST", () => {
  console.log(`Waypoint ${idx}`);
  fs.writeFileSync(`history/waypoint_${idx}.cpp`, Clava.getProgram().code);
  idx++;
});

eventListener.on("ACTION", (e: Event) => {
  
  // Event Logs for debugging

  console.log("\nReceived ACTION event");
  console.log(e.timing);
  console.log(e.description);
  console.log(e.mainJP);
  console.log(e.returnJP);
  console.log(e.inputs);
  console.log("\n");

  switch (e.timing) {
    case EventTime.BEFORE:
      break;

    case EventTime.AFTER:
      switch (e.description){
        case "insertAfter":
        case "insertBefore":
          if (e.returnJP !== undefined){
            ophistory.newOperation(new InsertOperation(e.returnJP))
          }
          break;
        default:
          break;
      }
      break;
    default:
      break;
  }

  // Manual testing the rollback
  /*
  console.log(`Waypoint ${idx}`);
  fs.writeFileSync(`history/waypoint_${idx}.cpp`, Clava.getProgram().code);
  idx++;

  ophistory.rollback();

  console.log(`Waypoint ${idx}`);
  fs.writeFileSync(`history/waypoint_${idx}.cpp`, Clava.getProgram().code);
  idx++;
  */
});

export default eventListener;

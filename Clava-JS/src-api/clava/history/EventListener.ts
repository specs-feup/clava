import { EventEmitter } from "events";
import * as fs from "fs";
import Clava from "../Clava.js";
import { Event, EventTime } from "./Events.js";
import ophistory from "./History.js";
import { InsertOperation, ReplaceOperation, TypeChangeOperation } from "./Operations.js";
import { Joinpoint } from "../../Joinpoints.js";

const eventListener = new EventEmitter();

let idx = 0;

eventListener.on("storeAST", () => {
  console.log(`Waypoint ${idx}`);
  fs.writeFileSync(`history/waypoint_${idx}.cpp`, Clava.getProgram().code);
  idx++;
});

eventListener.on("ACTION", (e: Event) => {
  
  // Event Logs for debugging
  /*
  console.log("\nReceived ACTION event");
  console.log(e.timing);
  console.log(e.description);
  console.log(e.mainJP);
  console.log(e.returnJP);
  console.log(e.inputs);
  console.log("\n");
  */

  switch (e.timing) {
    case EventTime.BEFORE:
      switch (e.description) {
        case "setType":
          changeTypeFromEvent(e);
          break;
        default:
          break;
      }
    case EventTime.AFTER:
      switch (e.description){
        case "insertAfter":
        case "insertBefore":
          insertOperationFromEvent(e);
          break;
        case "replaceWith":
          if (e.inputs.length > 0){
            if (typeof e.inputs[0] === 'string' || e.inputs[0] instanceof Joinpoint){
              replaceSingleOperationFromEvent(e);
            }
            else {
              replaceMultipleOperationFromEvent(e);
            }
          }
          break;
        case "replaceWithStrings":
          if (e.inputs.length > 0){
            replaceMultipleOperationFromEvent(e);
          }
          break;
        case "toComment":
          replaceSingleOperationFromEvent(e);
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

function insertOperationFromEvent(e: Event) {
  if (e.returnJP !== undefined){
    ophistory.newOperation(new InsertOperation(e.returnJP))
  }
}

function replaceSingleOperationFromEvent(e: Event) {
  if (e.returnJP !== undefined){
    ophistory.newOperation(new ReplaceOperation(e.mainJP, e.returnJP, 1));
  }
}

function replaceMultipleOperationFromEvent(e: Event) {
  if (e.returnJP !== undefined){
    ophistory.newOperation(new ReplaceOperation(e.mainJP, e.returnJP, (e.inputs[0] as (Joinpoint[] | string[])).length));
  }
}

function changeTypeFromEvent(e: Event) {
  ophistory.newOperation(new TypeChangeOperation(e.mainJP, e.mainJP.type))
}

export default eventListener;

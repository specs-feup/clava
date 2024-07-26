import { EventEmitter } from "events";
import { Event, EventTime } from "./Events.js";
import ophistory from  "../history/History.js"
import { DetachOperation, DetachReference, InlineCommentOperation, InsertOperation, RemoveChildrenOperation, ReplaceOperation, SetChildOperation, TypeChangeOperation, ValueOperation } from "../history/Operations.js";
import { Joinpoint } from "../../Joinpoints.js";

const eventListener = new EventEmitter();

// Used for saving previous child in setFirstChild and setLastChild
let auxJP: Joinpoint;

eventListener.on("ACTION", (e: Event) => {

  switch (e.timing) {
    case EventTime.BEFORE:
      switch (e.description) {
        case "detach":
          detachOperationFromEvent(e);
          break;
        case "removeChildren":
          removeChildrenOperationFromEvent(e);
          break;
        case "setType":
          changeTypeFromEvent(e);
          break;
        case "setFirstChild":
          auxJP = e.mainJP.firstChild;
          break;
        case "setLastChild":
          auxJP = e.mainJP.lastChild;
          break;
        case "setInlineComments":
          inlineCommentOperationFromEvent(e);
          break;
        case "setValue":
          setValueOperationFromEvent(e);
          break;
        default:
          break;
      }
      break;
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
        case "setFirstChild":
          setFirstChildFromEvent(e, auxJP);
          break;
        case "setLastChild":
          setLastChildFromEvent(e, auxJP);
          break;
        default:
          break;
      }
      break;
  }
});

function insertOperationFromEvent(e: Event) {
  if (e.returnValue !== undefined){
    ophistory.newOperation(new InsertOperation(e.returnValue))
  }
}

function replaceSingleOperationFromEvent(e: Event) {
  if (e.returnValue !== undefined){
    ophistory.newOperation(new ReplaceOperation(e.mainJP, e.returnValue, 1));
  }
}

function replaceMultipleOperationFromEvent(e: Event) {
  if (e.returnValue !== undefined){
    ophistory.newOperation(new ReplaceOperation(e.mainJP, e.returnValue, (e.inputs[0] as (Joinpoint[] | string[])).length));
  }
}

function detachOperationFromEvent(e: Event) {
  let refJP: Joinpoint, ref: DetachReference;
  if (e.mainJP.siblingsLeft.length >= 1){
    refJP = e.mainJP.leftJp;
    ref = DetachReference.LEFT;
  } else if  (e.mainJP.siblingsRight.length >= 1) {
    refJP = e.mainJP.rightJp;
    ref = DetachReference.RIGHT;
  } else {
    refJP = e.mainJP.parent;
    ref = DetachReference.TOP;
  }
  ophistory.newOperation(new DetachOperation(e.mainJP, refJP, ref));
}

function setFirstChildFromEvent(e: Event, aux: Joinpoint) {
  ophistory.newOperation(new SetChildOperation(e.mainJP.firstChild, aux));
}

function setLastChildFromEvent(e: Event, aux: Joinpoint) {
  ophistory.newOperation(new SetChildOperation(e.mainJP.lastChild, aux));
}

function removeChildrenOperationFromEvent(e: Event) {
  ophistory.newOperation(new RemoveChildrenOperation(e.mainJP, e.mainJP.children));
}

function changeTypeFromEvent(e: Event) {
  ophistory.newOperation(new TypeChangeOperation(e.mainJP, e.mainJP.type));
}

function inlineCommentOperationFromEvent(e: Event) {
  const comments = e.mainJP.inlineComments
    .map((comment) => comment.text)
    .filter((comment): comment is string => comment !== null);
  ophistory.newOperation(new InlineCommentOperation(e.mainJP, comments));
}

function setValueOperationFromEvent(e: Event) {
  if (e.inputs && e.inputs.at(0)) {
    ophistory.newOperation(new ValueOperation(e.mainJP, e.inputs.at(0) as string, e.mainJP.getValue(e.inputs.at(0) as string)));
  }
}

export default eventListener;

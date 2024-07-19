import { Joinpoint } from "../../Joinpoints.js";

export enum EventTime {
    BEFORE = "Before",
    AFTER = "After"
}

export class Event {
  public timing: EventTime;
  public description: string;
  public mainJP: Joinpoint;
  public returnJP?: Joinpoint;
  public inputs: unknown[];

  constructor(
    timing: EventTime,
    description: string,
    mainJP: Joinpoint,
    returnJP?: Joinpoint,
    ...inputs: unknown[]
  ) {
    this.timing = timing;
    this.description = description;
    this.mainJP = mainJP;
    this.returnJP = returnJP;
    this.inputs = inputs;
  }
}
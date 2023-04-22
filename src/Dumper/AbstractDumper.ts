import * as fs from "fs";

export default interface AbstractDumper {
  dump(): Object;
}

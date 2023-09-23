import { Joinpoint } from "../../Joinpoints.js";
import ResultList from "./ResultList.js";

export default abstract class Analyser {
  abstract analyse($node?: Joinpoint): ResultList | undefined;
}

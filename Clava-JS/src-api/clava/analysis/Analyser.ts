import { Joinpoint } from "../../Joinpoints.js";
import ResultList from "./ResultList.js";

export default abstract class Analyser<T extends Joinpoint> {
  abstract analyse($node: T): ResultList<T> | undefined;
}

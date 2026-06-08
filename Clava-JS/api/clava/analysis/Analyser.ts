import { Joinpoint } from "../../Joinpoints.ts";
import ResultList from "./ResultList.ts";

export default abstract class Analyser {
  abstract analyse($node?: Joinpoint): ResultList | undefined;
}

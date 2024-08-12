import WeaverLauncherBase from "lara-js/api/weaver/WeaverLauncherBase.js";
import Clava from "../clava/Clava.js";

export default class WeaverLauncher extends WeaverLauncherBase {
  execute(args: string | any[]) {
    return Clava.runClava(args);
  }
}

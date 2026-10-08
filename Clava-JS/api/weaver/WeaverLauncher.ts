import WeaverLauncherBase from "@specs-feup/lara/api/weaver/WeaverLauncherBase.ts";
import Clava from "../clava/Clava.ts";

export default class WeaverLauncher extends WeaverLauncherBase {
  execute(args: string | any[]) {
    return Clava.runClava(args);
  }
}

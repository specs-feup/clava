import { FunctionJp, Vardecl } from "../../Joinpoints.js";

export default function RemoveShadowing($function: FunctionJp): void {
  const usedNames = new Set();
  let aliasIndex = 0;

  for (const $param of $function.params) {
    usedNames.add($param.name);
  }

  for (const $jp of $function.body.getDescendants("vardecl")) {
    const $vardecl = $jp as Vardecl;

    if (usedNames.has($vardecl.name)) {
      // TODO: ensure the new name is not part of the usedNames
      const newName = `${$vardecl.name}_${aliasIndex++}`;
      $vardecl.name = newName;
      usedNames.add(newName);
    }
  }
}

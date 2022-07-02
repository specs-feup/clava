function RemoveShadowing($function) {
  const usedNames = new Set();
  let aliasIndex = 0;

  for (const $param of $function.params) {
    usedNames.add($param.name);
  }

  for (const $vardecl of $function.body.descendants("vardecl")) {
    if (usedNames.has($vardecl.name)) {
      const newName = `${$vardecl.name}_${aliasIndex++}`;
      $vardecl.name = newName;
      usedNames.add(newName);
    }
  }
}

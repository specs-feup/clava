const prefix = "clava-js/api/";
const coreImports = [];
const sideEffectsOnlyImports = ["Joinpoints.js"];
for (const sideEffectsOnlyImport of sideEffectsOnlyImports) {
    await import(prefix + sideEffectsOnlyImport);
}
for (const coreImport of coreImports) {
    const foo = Object.entries(await import(prefix + coreImport));
    foo.forEach(([key, value]) => {
        // @ts-ignore
        globalThis[key] = value;
    });
}
export {};
//# sourceMappingURL=core.js.map
export enum FileExtensions {
  JS = ".js",
  MJS = ".mjs",
  CJS = ".cjs",
}

export const isValidFileExtension = (extension: string) => {
  return Object.values(FileExtensions).includes(extension as FileExtensions);
};

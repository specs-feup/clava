export enum FileExtensions {
  JS = ".js",
  MJS = ".mjs",
  CJS = ".cjs",
}

/**
 * Checks if a file extension is valid.
 *
 * @param extension - The file extension to check.
 * @returns `true` if the file extension is valid, `false` otherwise.
 */
export const isValidFileExtension = (extension: string): boolean => {
  return Object.values(FileExtensions).includes(extension as FileExtensions);
};

import Io from "lara-js/api/lara/Io.js";
import { debug } from "lara-js/api/lara/core/LaraCore.js";
import { JavaClasses } from "lara-js/api/lara/util/JavaTypes.js";
import Clava from "../Clava.js";
import CMakerUtils from "./CMakerUtils.js";

/**
 * Contains CMaker sources
 */
export default class CMakerSources {
  untaggedSources = [];
  taggedSources: Record<string, string[]> = {};
  tags: Set<string> = new Set();
  disableWeaving: boolean;

  constructor(disableWeaving: boolean = false) {
    this.disableWeaving = disableWeaving;
  }

  private static VARIABLE_UNTAGGED_SOURCES = "CMAKER_SOURCES";
  private static VARIABLE_EXTERNAL_SOURCES = "EXTERNAL_SOURCES";

  /**
   *
   */
  copy() {
    const newCMakerSources = new CMakerSources();

    newCMakerSources.untaggedSources = this.untaggedSources.slice();
    newCMakerSources.taggedSources = structuredClone(this.taggedSources);
    newCMakerSources.tags = structuredClone(this.tags);
    newCMakerSources.disableWeaving = this.disableWeaving;

    return newCMakerSources;
  }

  /**
   * Adds the given sources.
   *
   * @param paths - Array with paths to sources
   */
  addSources(paths: string[]) {
    for (const path of paths) {
      this.addSourcePrivate(this.untaggedSources, path);
    }
  }

  /**
   * Add the given sources.
   */
  addSource(path: string) {
    this.addSourcePrivate(this.untaggedSources, path);
  }

  /**
   * Adds the given sources associated to a tag.
   */
  addTaggedSources(tag: string, paths: string[]) {
    // Get current tagged sources
    let sources = this.taggedSources[tag];

    // If not defined, initialize it
    if (sources === undefined) {
      sources = [];
      this.taggedSources[tag] = sources;
      this.tags.add(tag);
    }

    for (const path of paths) {
      this.addSourcePrivate(sources, path);
    }
  }

  private addSourcePrivate(sources: string[], path: string | JavaClasses.File) {
    const parsedPath = CMakerUtils.parsePath(path);
    sources.push(`"${parsedPath}"`);
  }

  /**
   * @returns An array with the CMake variables that have source files
   */
  getSourceVariables() {
    const sources = [];

    if (this.untaggedSources.length > 0) {
      sources.push(CMakerSources.VARIABLE_UNTAGGED_SOURCES);
    }

    if (!this.disableWeaving) {
      if (Clava.getProgram().extraSources.length > 0) {
        sources.push(CMakerSources.VARIABLE_EXTERNAL_SOURCES);
      }
    }

    for (const tag of this.tags.values()) {
      sources.push(tag);
    }

    return sources;
  }

  private parseSourcePath(path: string | JavaClasses.File) {
    return `"${CMakerUtils.parsePath(path)}"`;
  }

  /**
   * @returns String with the CMake code that declares the current sources
   */
  getCode() {
    let code = "";

    // Add untagged sources
    if (this.untaggedSources.length > 0) {
      code += this.getCodeSource(
        CMakerSources.VARIABLE_UNTAGGED_SOURCES,
        this.untaggedSources
      );
    }

    // Add external sources if weaving is not disabled
    if (!this.disableWeaving) {
      code = this.addExternalSources(code);
    }

    for (const tag of this.tags.values()) {
      const tagCode = this.getCodeSource(tag, this.taggedSources[tag]);

      if (code.length !== 0) {
        code += "\n";
      }

      code += tagCode;
    }

    return code;
  }

  private addExternalSources(code: string) {
    const extraSources = Clava.getProgram().extraSources;
    const extraSourcesArray = [];
    for (const extraSource of extraSources) {
      debug(`Adding external source '${extraSource}'`);
      if (Io.isFile(extraSource)) {
        extraSourcesArray.push(this.parseSourcePath(extraSource));
      } else if (Io.isFolder(extraSource)) {
        for (const sourcePath of Io.getPaths(extraSource)) {
          extraSourcesArray.push(this.parseSourcePath(sourcePath));
        }
      } else {
        console.log(`[CMAKER] Extra source ' ${extraSource} ' does not exist`);
      }
    }

    if (extraSourcesArray.length > 0) {
      if (code.length !== 0) {
        code += "\n";
      }

      code += this.getCodeSource(
        CMakerSources.VARIABLE_EXTERNAL_SOURCES,
        extraSourcesArray
      );
    }

    return code;
  }

  /**
   * @returns String with the CMake code for a given source name and values
   */
  private getCodeSource(sourceName: string, values: string[]) {
    const prefix = `set (${sourceName} `;

    // Build space
    let space = "";
    for (let i = 0; i < prefix.length; i++) {
      space += " ";
    }

    return prefix + values.join("\n" + space) + "\n)";

  };
}

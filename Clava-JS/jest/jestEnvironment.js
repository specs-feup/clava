import { TestEnvironment } from "jest-environment-node";
import java from "java";

export default class CustomEnvironment extends TestEnvironment {
  constructor(config) {
    super(config);
  }

  async setup() {
    await super.setup();
    this.global.__SHARED_MODULE__ = java;
  }

  async teardown() {
    await super.teardown();
  }

  runScript(script) {
    return super.runScript(script);
  }
}

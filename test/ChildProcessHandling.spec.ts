import "mocha";
import sinon from "sinon";
import * as chai from "chai";
import chaiAsPromised from "chai-as-promised";
const expect = chai.expect;
chai.use(chaiAsPromised);

import {
  addActiveChildProcess,
  handleExit,
  activeChildProcesses,
} from "../src/ChildProcessHandling";

describe("ChildProcessHandling", () => {
  describe("addActiveChildProcess", () => {
    it("should add the child process to the activeChildProcesses object", () => {
      const childProcess = { pid: 123, on: () => {} } as any;
      addActiveChildProcess(childProcess);
      expect(activeChildProcesses[childProcess.pid]).to.equal(childProcess);
    });
  });

  describe("handleExit", () => {
    it("should close all child processes", async () => {
      const childProcess1 = {
        pid: 123,
        kill: () => {},
        once: (code: string, callback: () => {}) => {
          callback();
        },
      } as any;
      const childProcess2 = {
        pid: 456,
        kill: () => {},
        once: (code: string, callback: () => {}) => {
          callback();
        },
      } as any;
      activeChildProcesses[childProcess1.pid] = childProcess1;
      activeChildProcesses[childProcess2.pid] = childProcess2;

      const childProcessKillSpy1 = sinon.spy(childProcess1, "kill");
      const childProcessOnceSpy1 = sinon.spy(childProcess1, "once");
      const childProcessKillSpy2 = sinon.spy(childProcess2, "kill");
      const childProcessOnceSpy2 = sinon.spy(childProcess2, "once");

      await handleExit(false);

      expect(childProcessKillSpy1.calledOnce).to.be.true;
      expect(childProcessOnceSpy1.calledOnce).to.be.true;
      expect(childProcessKillSpy2.calledOnce).to.be.true;
      expect(childProcessOnceSpy2.calledOnce).to.be.true;

      sinon.restore();
    });
  });
});

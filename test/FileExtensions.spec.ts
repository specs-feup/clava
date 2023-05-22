import "mocha";
import * as chai from "chai";
import chaiAsPromised from "chai-as-promised";
const expect = chai.expect;
chai.use(chaiAsPromised);

import { isValidFileExtension, FileExtensions } from "../src/FileExtensions";

describe("isValidFileExtension", () => {
  it("should return true for valid file extensions", () => {
    expect(isValidFileExtension(FileExtensions.JS)).to.be.true;
    expect(isValidFileExtension(FileExtensions.MJS)).to.be.true;
    expect(isValidFileExtension(FileExtensions.CJS)).to.be.true;
  });

  it("should return false for invalid file extensions", () => {
    expect(isValidFileExtension(".txt")).to.be.false;
    expect(isValidFileExtension(".html")).to.be.false;
    expect(isValidFileExtension(".css")).to.be.false;
  });
});

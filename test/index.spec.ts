"use strict";

import "mocha";
import * as chai from "chai";
import * as index from "../dist/index.js";

const expect = chai.expect;

describe("simple validate test", function () {
    it("should return true for null string", function () {
        var result = index.IsNullOrEmpty(null);
        expect(result).to.equal(true);
    });

    it("should return true for undefined string", function () {
        var result = index.IsNullOrEmpty(undefined);
        expect(result).to.equal(true);
    });

    it("should return true for empty string", function () {
        var result = index.IsNullOrEmpty("");
        expect(result).to.equal(true);
    });

    it("should return true for whitespace string", function () {
        var result = index.IsNullOrEmpty(" ");
        expect(result).to.equal(true);
    });

    it("should return false for non-empty string", function () {
        var result = index.IsNullOrEmpty("test");
        expect(result).to.equal(false);
    });
}).afterEach(() => console.log("test"));

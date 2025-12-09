import { describe, it } from "bun:test";
import { ClavaLegacyTester } from "../bun/ClavaLegacyTester.js";
import ClavaJavaTypes from "@specs-feup/clava/api/clava/ClavaJavaTypes.js";
import path from "path";

describe("IssuesTest", () => {
    function newTester() {
        return new ClavaLegacyTester(
            path.resolve("../ClavaWeaver/resources/clava/test/issues"),
            ClavaJavaTypes.Standard.CXX11
        )
            .setResultPackage("c/results")
            .setSrcPackage("c/src");
    }

    it("Issue168", async () => {
        await newTester().test("Issue168.js", "issue_168.c");
    });

    it("Issue-AIQ-1", async () => {
        await newTester().test("Issue_aiq_1.js", "issue_aiq_1.c");
    });
});

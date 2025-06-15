import { ClavaWeaverTester } from "./LegacyIntegrationTestsHelpers.test.js";
import ClavaJavaTypes from "@specs-feup/clava/api/clava/ClavaJavaTypes.js";
import path from "path";
import "@specs-feup/clava/api/Joinpoints.js";

/* eslint-disable jest/expect-expect */
describe("CxxTest", () => {
    function newTester() {
        return new ClavaWeaverTester(
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

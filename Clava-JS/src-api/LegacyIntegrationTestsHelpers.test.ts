import JavaTypes from "@specs-feup/lara/api/lara/util/JavaTypes.js";
import { ClavaJavaClasses } from "@specs-feup/clava/api/clava/ClavaJavaTypes.js";
import Clava from "@specs-feup/clava/api/clava/Clava.js";
import fs from "fs";
import util from "util";
import { jest } from "@jest/globals";
import path from "path";

export class ClavaWeaverTester {
    private static readonly WORK_FOLDER: string = "cxx_weaver_output";

    private readonly basePackage: string;
    private readonly standard: ClavaJavaClasses.Standard;
    private readonly compilerFlags: string;

    private checkWovenCodeSyntax: boolean;
    private _checkExpectedOutput: boolean;
    private srcPackage: string | null;
    private resultPackage: string | null;
    private resultsFile: string | null;
    private run: boolean;

    public constructor(
        basePackage: string,
        standard: ClavaJavaClasses.Standard,
        compilerFlags: string = ""
    ) {
        this.basePackage = basePackage;
        // TODO: This is ignored
        this.standard = standard;
        // TODO: This is ignored
        this.compilerFlags = compilerFlags;
        // TODO: This is ignored
        this.checkWovenCodeSyntax = true;

        this.srcPackage = null;
        this.resultPackage = null;
        this.resultsFile = null;
        this.run = true;
        this._checkExpectedOutput = true;
    }

    public checkExpectedOutput(checkExpectedOutput: boolean) {
        this._checkExpectedOutput = checkExpectedOutput;

        return this;
    }

    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    public set(key: string, value: string | boolean = true) {
        // TODO: Implement this
        //this.additionalSettings.set(key, true);
        return this;
    }

    /**
     *
     * @returns the previous value
     */
    public setCheckWovenCodeSyntax(checkWovenCodeSyntax: boolean) {
        this.checkWovenCodeSyntax = checkWovenCodeSyntax;

        return this;
    }

    public setResultPackage(resultPackage: string) {
        this.resultPackage = this.sanitizePackage(resultPackage);

        return this;
    }

    public setSrcPackage(srcPackage: string) {
        this.srcPackage = this.sanitizePackage(srcPackage);

        return this;
    }

    public doNotRun() {
        this.run = false;
        return this;
    }

    public setResultsFile(resultsFile: string): void {
        this.resultsFile = resultsFile;
    }

    private sanitizePackage(packageName: string): string {
        let sanitizedPackage: string = packageName;
        if (!sanitizedPackage.endsWith("/")) {
            sanitizedPackage += "/";
        }

        return sanitizedPackage;
    }

    private buildCodeResource(codeResourceName: string) {
        let filepath: string = this.basePackage;

        if (this.srcPackage != null) {
            filepath = path.join(filepath, this.srcPackage);
        }

        return path.join(filepath, codeResourceName);
    }

    public async test(
        laraResource: string,
        ...codeResources: string[]
    ): Promise<void> {
        if (!this.run) {
            console.info("Ignoring test, 'run' flag is not set");
            return;
        }

        let out = "";
        const log = jest.spyOn(global.console, "log");
        log.mockImplementation((data, ...args: unknown[]) => {
            out += util.format(data, ...args) + "\n";
        });

        try {
            Clava.getProgram().push();
            const program = Clava.getProgram();
            for (const codeResource of codeResources) {
                program.addFileFromPath(
                    new JavaTypes.File(this.buildCodeResource(codeResource))
                );
            }
            program.rebuild();

            await import(path.join(this.basePackage, laraResource));
        } finally {
            log.mockRestore();
            Clava.getProgram().pop();
        }

        // Do not check expected output
        if (!this._checkExpectedOutput) {
            return;
        }

        let expectedResource: string = this.basePackage;
        if (this.resultPackage != null) {
            expectedResource = path.join(expectedResource, this.resultPackage);
        }

        const actualResultsFile: string =
            this.resultsFile ?? laraResource + ".txt";

        expectedResource = path.join(expectedResource, actualResultsFile);

        if (!fs.existsSync(expectedResource)) {
            console.info(
                "Could not find resource '" +
                    expectedResource +
                    "', skipping verification. Actual output:\n" +
                    out
            );

            throw new Error("Expected outputs not found");
        }

        // eslint-disable-next-line jest/no-standalone-expect
        expect(ClavaWeaverTester.normalize(out)).toEqual(
            ClavaWeaverTester.normalize(
                fs
                    .readFileSync(expectedResource, "utf8")
                    .replaceAll(
                        `/**** File '${ClavaWeaverTester.WORK_FOLDER}/`,
                        "/**** File '"
                    )
            )
        );
    }

    /**
     * Normalizes endlines
     */
    private static normalize(string: string): string {
        return JavaTypes.SpecsStrings.normalizeFileContents(string, true);
    }
}

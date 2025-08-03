import JavaTypes, { JavaClasses } from "@specs-feup/lara/api/lara/util/JavaTypes.js";
import ClavaJavaTypes, {
    ClavaJavaClasses,
} from "@specs-feup/clava/api/clava/ClavaJavaTypes.js";
import Weaver from "@specs-feup/lara/api/weaver/Weaver.js";
import fs from "fs";
import util from "util";
import { jest } from "@jest/globals";
import path from "path";

describe("Dummy", () => {
    it("Dummy", () => {
        expect(true).toBe(true);
    });
});

afterAll(() => {
    const javaWeaver = Weaver.getWeaverEngine();
    const javaDatastore = javaWeaver.getData().get();

    javaDatastore.set(
      JavaTypes.LaraiKeys.WORKSPACE_FOLDER,
      JavaTypes.FileList.newInstance()
    );

    javaWeaver.run(javaDatastore);
});

// eslint-disable-next-line jest/no-export
export class ClavaWeaverTester {
    private static readonly WORK_FOLDER: string = "cxx_weaver_output";

    private readonly basePackage: string;
    private readonly standard: ClavaJavaClasses.Standard;
    private readonly compilerFlags: string;

    /**
     * Stores the original values of the datastore settings that were modified by the test
     */
    private readonly modifiedDatastoreSettings: Map<string, unknown> =
        new Map();
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
        this.standard = standard;
        this.compilerFlags = compilerFlags;
        this.checkWovenCodeSyntax = true;

        this.srcPackage = null;
        this.resultPackage = null;
        this.resultsFile = null;
        this.run = true;
        this._checkExpectedOutput = true;

        this.set(ClavaJavaTypes.ClavaOptions.FLAGS, this.compilerFlags);
    }

    public checkExpectedOutput(checkExpectedOutput: boolean) {
        this._checkExpectedOutput = checkExpectedOutput;

        return this;
    }

    public set(key: string, value: unknown = true) {
        const datastore = Weaver.getWeaverEngine().getData().get();
        const currentValue = datastore.get(key);

        if (value !== currentValue) {
            this.modifiedDatastoreSettings.set(key, currentValue);
            datastore.set(key, value);
        }

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
            if (data) {
                out += util.format(data, ...args);
            }
            out += "\n";
        });

        if (this.standard != null) {
            this.set(ClavaJavaTypes.ClavaOptions.STANDARD, this.standard);
        }

        this.set(
            ClavaJavaTypes.CxxWeaverOption.CHECK_SYNTAX,
            this.checkWovenCodeSyntax
        );
        this.set(ClavaJavaTypes.CxxWeaverOption.DISABLE_CLAVA_INFO, true);
        this.set(ClavaJavaTypes.CxxWeaverOption.DISABLE_CODE_GENERATION);

        // Enable parallel parsing
        //this.set(ClavaJavaTypes.ParallelCodeParser.PARALLEL_PARSING);

        try {
            const javaFiles: JavaClasses.List<JavaClasses.File> = new JavaTypes.ArrayList();

            for (const codeResource of codeResources) {
                const javaFile = new JavaTypes.File(
                    this.buildCodeResource(codeResource)
                );
                if (!fs.existsSync(javaFile.getAbsolutePath())) {
                    throw new Error(
                        `Code resource '${codeResource}' does not exist at '${javaFile.getAbsolutePath()}'.`
                    );
                }
                javaFiles.add(javaFile);
            }

            const javaWeaver = Weaver.getWeaverEngine();
            const javaDatastore = javaWeaver.getData().get();

            javaDatastore.set(
                JavaTypes.LaraiKeys.WORKSPACE_FOLDER,
                JavaTypes.FileList.newInstance(javaFiles)
            );

            javaWeaver.run(javaDatastore);
            await import(path.join(this.basePackage, laraResource));
            javaWeaver.close();
        } finally {
            log.mockRestore();

            const datastore = Weaver.getWeaverEngine().getData().get();
            this.modifiedDatastoreSettings.forEach((value, key) => {
                datastore.set(key, value);
            });
            this.modifiedDatastoreSettings.clear();
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

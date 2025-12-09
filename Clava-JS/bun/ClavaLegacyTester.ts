import ClavaJavaTypes, {
    ClavaJavaClasses,
} from "@specs-feup/clava/api/clava/ClavaJavaTypes.js";

import { WeaverLegacyTester } from "@specs-feup/lara/bun/WeaverLegacyTester.js";

export class ClavaLegacyTester extends WeaverLegacyTester {
    protected readonly WORK_FOLDER: string = "cxx_weaver_output";
    private readonly standard: ClavaJavaClasses.Standard;
    private readonly compilerFlags: string;

    public constructor(
        basePackage: string,
        standard: ClavaJavaClasses.Standard,
        compilerFlags: string = ""
    ) {
        super(basePackage);
        this.standard = standard;
        this.compilerFlags = compilerFlags;

        this.set(ClavaJavaTypes.ClavaOptions.FLAGS, this.compilerFlags);
    }

    public async test(
        laraResource: string,
        ...codeResources: string[]
    ): Promise<void> {
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

        await super.test(laraResource, ...codeResources);
    }
}

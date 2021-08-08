/**
 * Copyright 2016 SPeCS.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package eu.antarex.clang.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import pt.up.fe.specs.clang.codeparser.ClangAstDumper;
import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.providers.ResourceProvider;

public abstract class AClangAstTester {

    private static final boolean CLEAN_CLANG_FILES = true;
    private static final String OUTPUT_FOLDERNAME = "temp-clang-ast";

    private final Collection<ResourceProvider> resources;
    private List<String> compilerOptions;

    private boolean showClangAst = false;
    private boolean showClavaAst = false;
    private boolean showClangDump = false;
    private boolean showCode = false;
    private boolean onePass = false;
    private boolean run = true;
    private boolean builtinCuda = false;

    public <T extends Enum<T> & ResourceProvider> AClangAstTester(Class<T> resource) {
        this(resource, Collections.emptyList());
    }

    public <T extends Enum<T> & ResourceProvider> AClangAstTester(Class<T> resources, List<String> compilerOptions) {
        this(Arrays.asList(resources.getEnumConstants()), compilerOptions);
    }

    // public AClangAstTester(TestResources resources) {
    // this(resources, Collections.emptyList());
    // }

    public AClangAstTester(String base, String file) {
        this(base, Arrays.asList(file));
    }

    public AClangAstTester(String base, List<String> files) {
        this(new TestResources(base, files), Collections.emptyList());
    }

    public AClangAstTester(String base, String file, List<String> compilerOptions) {
        this(base, Arrays.asList(file), compilerOptions);
    }

    public AClangAstTester(String base, List<String> files, List<String> compilerOptions) {
        this(new TestResources(base, files), compilerOptions);
    }

    private AClangAstTester(TestResources resources, List<String> compilerOptions) {
        this(resources.getResources(), compilerOptions);
    }

    public AClangAstTester(Collection<ResourceProvider> resources, List<String> compilerOptions) {
        this.resources = resources;
        this.compilerOptions = new ArrayList<>(compilerOptions);

        // Set strict mode
        // ClangAstParser.strictMode(true);
    }

    public AClangAstTester showClangAst() {
        showClangAst = true;
        return this;
    }

    public AClangAstTester showClavaAst() {
        showClavaAst = true;
        return this;
    }

    public AClangAstTester showClangDump() {
        showClangDump = true;
        return this;
    }

    public AClangAstTester showCode() {
        showCode = true;
        return this;
    }

    public AClangAstTester onePass() {
        onePass = true;
        return this;
    }

    public AClangAstTester doNotRun() {
        this.run = false;
        return this;
    }

    public AClangAstTester enableBuiltinCuda() {
        this.builtinCuda = true;
        return this;
    }
    /*
    public AClangAstTester keepFiles() {
        this.keepFiles = true;
        return this;
    }
    */

    public AClangAstTester addFlags(String... flags) {
        return addFlags(Arrays.asList(flags));
    }

    public AClangAstTester addFlags(List<String> flags) {
        compilerOptions.addAll(flags);
        return this;
    }

    public void test() {
        if (!run) {
            ClavaLog.info("Ignoring test, 'run' flag is not set");
            return;
        }

        try {
            setUp();
            testProper();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    @Before
    public void setUp() throws Exception {
        SpecsSystem.programStandardInit();

        // Copy resources under test
        File outputFolder = SpecsIo.mkdir(AClangAstTester.OUTPUT_FOLDERNAME);
        for (ResourceProvider resource : resources) {
            File copiedFile = SpecsIo.resourceCopy(resource.getResource(), outputFolder, false, true);
            Assert.assertTrue("Could not copy resource '" + resource + "'", copiedFile.isFile());
        }

    }

    // @After
    public static void clear() throws Exception {

        // Delete ClangAst files
        if (CLEAN_CLANG_FILES) {

            // Delete resources under test
            File outputFolder = SpecsIo.mkdir(AClangAstTester.OUTPUT_FOLDERNAME);
            SpecsIo.deleteFolderContents(outputFolder);
            outputFolder.delete();

            ClangAstDumper.getTempFiles().stream()
                    .forEach(filename -> new File(filename).delete());
        }

    }

    @Test
    public void testProper() {
        // Parse files

        CodeParser codeParser = CodeParser.newInstance()
                .set(CodeParser.SHOW_CLANG_AST, showClangAst)
                .set(CodeParser.SHOW_CLANG_DUMP, showClangDump)
                .set(CodeParser.SHOW_CLAVA_AST, showClavaAst)
                .set(CodeParser.SHOW_CODE, showCode);

        if (builtinCuda) {
            codeParser.set(CodeParser.CUDA_PATH, CodeParser.getBuiltinOption());
        }
        // .setShowClangAst(showClangAst)
        // .setShowClangDump(showClangDump)
        // .setShowClavaAst(showClavaAst)
        // .setShowCode(showCode);

        File workFolder = new File(AClangAstTester.OUTPUT_FOLDERNAME);

        // Enable parallel parsing
        codeParser.set(ParallelCodeParser.PARALLEL_PARSING);

        App clavaAst = codeParser.parse(Arrays.asList(workFolder), compilerOptions);
        // System.out.println("STOREDEF CACHE:\n" + StoreDefinitions.getStoreDefinitionsCache().getAnalytics());
        // App clavaAst = codeParser.parseParallel(Arrays.asList(workFolder), compilerOptions);

        clavaAst.write(SpecsIo.mkdir(AClangAstTester.OUTPUT_FOLDERNAME + "/outputFirst"));
        if (onePass) {
            return;
        }

        CodeParser testCodeParser = CodeParser.newInstance();
        testCodeParser.set(ParallelCodeParser.PARALLEL_PARSING);
        if (builtinCuda) {
            testCodeParser.set(CodeParser.CUDA_PATH, CodeParser.getBuiltinOption());
        }

        // Parse output again, check if files are the same

        File firstOutputFolder = new File(AClangAstTester.OUTPUT_FOLDERNAME + "/outputFirst");

        App testClavaAst = testCodeParser.parse(Arrays.asList(firstOutputFolder), compilerOptions);
        // App testClavaAst = testCodeParser.parseParallel(Arrays.asList(firstOutputFolder), compilerOptions);
        testClavaAst.write(SpecsIo.mkdir(AClangAstTester.OUTPUT_FOLDERNAME + "/outputSecond"));
        // System.out.println("STOREDEF CACHE:\n" + StoreDefinitions.getStoreDefinitionsCache().getAnalytics());

        // Test if files from first and second are the same

        Map<String, File> outputFiles1 = SpecsIo.getFiles(new File(AClangAstTester.OUTPUT_FOLDERNAME + "/outputFirst"))
                .stream()
                .collect(Collectors.toMap(file -> file.getName(), file -> file));

        Map<String, File> outputFiles2 = SpecsIo.getFiles(new File(AClangAstTester.OUTPUT_FOLDERNAME + "/outputSecond"))
                .stream()
                .collect(Collectors.toMap(file -> file.getName(), file -> file));

        for (String name : outputFiles1.keySet()) {
            // Get corresponding file in output 2
            File outputFile2 = outputFiles2.get(name);

            // if (outputFile2 == null) {
            // ClavaLog.info("Could not find second version of file '" + name + "', ignoring");
            // }
            Assert.assertNotNull("Could not find second version of file '" + name + "'", outputFile2);

        }

        // Compare with .txt, if available
        for (ResourceProvider resource : resources) {

            // Get .txt resource
            String txtResource = resource.getResource() + ".txt";

            if (!SpecsIo.hasResource(txtResource)) {
                SpecsLogs.msgInfo("ClangTest: no .txt check file for resource '" + resource.getResource() + "'");
                System.out.println("Contents of output:\n" + SpecsIo.read(outputFiles2.get(resource.getFilename())));
                continue;
            }

            String txtContents = SpecsStrings.normalizeFileContents(SpecsIo.getResource(txtResource), true);
            File generatedFile = outputFiles2.get(resource.getFilename());
            String generatedFileContents = SpecsStrings.normalizeFileContents(SpecsIo.read(generatedFile), true);

            Assert.assertEquals(txtContents, generatedFileContents);
        }
    }

}

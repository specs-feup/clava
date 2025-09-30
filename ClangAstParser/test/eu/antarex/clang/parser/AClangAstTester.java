/**
 * Copyright 2016 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License. under the License.
 */

package eu.antarex.clang.parser;

import org.junit.Assert;
import org.junit.Before;
import org.suikasoft.jOptions.Datakey.DataKey;
import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.clang.dumper.ClangAstDumper;
import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.providers.ResourceProvider;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public abstract class AClangAstTester {

    private static final boolean CLEAN_CLANG_FILES = !SpecsSystem.isDebug();
    private static final String OUTPUT_FOLDERNAME = "temp-clang-ast";

    private final Collection<ResourceProvider> resources;
    private List<String> compilerOptions;

    private boolean onePass = false;
    private boolean run = true;
    private boolean idempotenceTest = false;

    private CodeParser codeParser;

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

        codeParser = CodeParser.newInstance();
        // Set strict mode
        // ClangAstParser.strictMode(true);
    }

    public <K, E extends K> AClangAstTester set(DataKey<K> key, E value) {
        codeParser.set(key, value);
        return this;
    }

    public AClangAstTester showClavaAst() {
        codeParser.set(CodeParser.SHOW_CLAVA_AST, true);
        return this;
    }

    public AClangAstTester showClangDump() {
        codeParser.set(CodeParser.SHOW_CLANG_DUMP, true);
        return this;
    }

    public AClangAstTester showCode() {
        codeParser.set(CodeParser.SHOW_CODE, true);
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
        codeParser.set(CodeParser.CUDA_PATH, CodeParser.getBuiltinOption());
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

    public void testProper() {

        // Enable parallel parsing
        codeParser.set(ParallelCodeParser.PARALLEL_PARSING);

        File workFolder = new File(AClangAstTester.OUTPUT_FOLDERNAME);


        // Parse files
        App clavaAst = codeParser.parse(Arrays.asList(workFolder), compilerOptions);

        clavaAst.write(SpecsIo.mkdir(AClangAstTester.OUTPUT_FOLDERNAME + "/outputFirst"));
        if (onePass) {
            return;
        }

        CodeParser testCodeParser = CodeParser.newInstance();

        // Set same options as original code parser
        testCodeParser.set(codeParser);


        // Parse output again, check if files are the same
        File firstOutputFolder = new File(AClangAstTester.OUTPUT_FOLDERNAME + "/outputFirst");

        App testClavaAst = testCodeParser.parse(Arrays.asList(firstOutputFolder), compilerOptions);

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

        // Idempotence test
        if (idempotenceTest) {
            testIdempotence(outputFiles1, outputFiles2);
        }
    }

    private void testIdempotence(Map<String, File> outputFiles1, Map<String, File> outputFiles2) {
        for (String name : outputFiles1.keySet()) {
            // Get corresponding file in output 1
            var outputFile1 = outputFiles1.get(name);

            // Get corresponding file in output 2
            var outputFile2 = outputFiles2.get(name);

            var normalizedFile1 = SpecsStrings.normalizeFileContents(SpecsIo.read(outputFile1), true);
            var normalizedFile2 = SpecsStrings.normalizeFileContents(SpecsIo.read(outputFile2), true);

            Assert.assertEquals(normalizedFile1, normalizedFile2);
        }
    }

}

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

import pt.up.fe.specs.clang.ClangAstParser;
import pt.up.fe.specs.clang.ast.genericnode.ClangRootNode;
import pt.up.fe.specs.clang.clavaparser.ClavaParser;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.util.SpecsCollections;
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
        // if (!AClangAstTester.CLEAN) {
        // if (keepFiles) {
        // return;
        // }

        // Delete resources under test
        File outputFolder = SpecsIo.mkdir(AClangAstTester.OUTPUT_FOLDERNAME);
        SpecsIo.deleteFolderContents(outputFolder);
        outputFolder.delete();

        // Delete ClangAst files
        if (CLEAN_CLANG_FILES) {
            ClangAstParser.getTempFiles().stream()
                    .forEach(filename -> new File(filename).delete());
        }

    }

    @Test
    public void testProper() {
        List<String> files = new ArrayList<>();

        // Add C++ sources
        files.addAll(SpecsCollections.map(SpecsIo.getFiles(new File(AClangAstTester.OUTPUT_FOLDERNAME), "cpp"),
                file -> SpecsIo.getCanonicalPath(file)));

        // Add C sources
        files.addAll(SpecsCollections.map(SpecsIo.getFiles(new File(AClangAstTester.OUTPUT_FOLDERNAME), "c"),
                file -> SpecsIo.getCanonicalPath(file)));

        // Parse files
        ClangRootNode ast = new ClangAstParser(showClangDump).parse(files, compilerOptions);

        if (showClangDump) {
            SpecsLogs.msgInfo("Clang Dump:\n" + SpecsIo.read(new File(ClangAstParser.getClangDumpFilename())));
        }

        if (showClangAst) {
            SpecsLogs.msgInfo("Clang AST:\n" + ast);
        }

        // Parse dump information
        try (ClavaParser clavaParser = new ClavaParser(ast)) {
            App clavaAst = clavaParser.parse();

            if (showClavaAst) {
                SpecsLogs.msgInfo("CLAVA AST:\n" + clavaAst);
            }

            if (showCode) {
                SpecsLogs.msgInfo("Code:\n" + clavaAst.getCode());
            }
            // System.out.println("AST:\n" + clavaAst);

            // Write
            // clavaAst.writeFromTopFile(new File(OUTPUT_FOLDERNAME + "/" + mainFile),
            // IoUtils.safeFolder(OUTPUT_FOLDERNAME + "/outputFirst"));
            clavaAst.write(new File(AClangAstTester.OUTPUT_FOLDERNAME),
                    SpecsIo.mkdir(AClangAstTester.OUTPUT_FOLDERNAME + "/outputFirst"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if (onePass) {
            return;
        }

        // Parse output again, check if files are the same
        List<String> files2 = new ArrayList<>();

        // C++ sources
        files2.addAll(SpecsCollections.map(
                SpecsIo.getFiles(new File(AClangAstTester.OUTPUT_FOLDERNAME + "/outputFirst"), "cpp"),
                file -> SpecsIo.getCanonicalPath(file)));

        // C sources
        files2.addAll(
                SpecsCollections.map(
                        SpecsIo.getFiles(new File(AClangAstTester.OUTPUT_FOLDERNAME + "/outputFirst"), "c"),
                        file -> SpecsIo.getCanonicalPath(file)));

        // Parse files
        ClangRootNode ast2 = new ClangAstParser().parse(files2, compilerOptions);

        // Parse dump information
        try (ClavaParser clavaParser = new ClavaParser(ast2)) {
            App clavaAst2 = clavaParser.parse();

            // Write
            // clavaAst2.writeFromTopFile(new File(OUTPUT_FOLDERNAME + "/outputFirst/" + mainFile),
            // IoUtils.safeFolder(OUTPUT_FOLDERNAME + "/outputSecond"));
            clavaAst2.write(new File(AClangAstTester.OUTPUT_FOLDERNAME + "/outputFirst/"),
                    SpecsIo.mkdir(AClangAstTester.OUTPUT_FOLDERNAME + "/outputSecond"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

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

            // List<String> lines1 = StringLines.getLines(outputFiles1.get(name));
            // List<String> lines2 = StringLines.getLines(outputFile2);

            // Print lines if they are not the same size
            //
            // if (lines1.size() != lines2.size()) {
            // LoggingUtils.msgInfo("Expected code:\n" + lines1.stream().collect(Collectors.joining("\n")));
            // LoggingUtils.msgInfo("Actual code:\n" + lines2.stream().collect(Collectors.joining("\n")));
            // LoggingUtils
            // .msgInfo("Expected number of lines to be the same. Expected (" + lines1.size() + ") vs Actual ("
            // + lines2.size() + ")");
            // }

            // System.out.println("LINES1:\n" + lines1);
            // System.out.println("LINES2:\n" + lines2);

            // Assert.assertEquals("Expected number of lines to be the same", lines1.size(), lines2.size());

            // Compare each line
            // for (int i = 0; i < lines1.size(); i++) {
            // Assert.assertEquals("Expected lines to be the same", lines1.get(i), lines2.get(i));
            // }
        }

        // System.out.println("AST:\n" + ast2);

        // Compare with .txt, if available
        for (ResourceProvider resource : resources) {
            // Get .txt resource
            String txtResource = resource.getResource() + ".txt";

            if (!SpecsIo.hasResource(txtResource)) {
                SpecsLogs.msgInfo("ClangTest: no .txt check file for resource '" + resource.getResource() + "'");
                System.out.println("Contents of output:\n" + SpecsIo.read(outputFiles2.get(resource.getFilename())));
                continue;
            }

            String txtContents = SpecsStrings.normalizeFileContents(SpecsIo.getResource(txtResource));
            File generatedFile = outputFiles2.get(resource.getFilename());
            String generatedFileContents = SpecsStrings.normalizeFileContents(SpecsIo.read(generatedFile));
            // System.out.println(
            // "CODE:" + IntStream.range(0, txtContents.length()).mapToObj(i -> (int) txtContents.charAt(i))
            // .collect(Collectors.toList()));
            // String file = IoUtils.read(generatedFile);
            // System.out.println("FILE:" + IntStream.range(0, file.length()).mapToObj(i -> (int) file.charAt(i))
            // .collect(Collectors.toList()));
            Assert.assertEquals(txtContents, generatedFileContents);
        }
    }

}

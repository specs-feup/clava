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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clang.parser;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import org.suikasoft.jOptions.Datakey.DataKey;

import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.clang.dumper.AstWireBenchmarkIdentity;
import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.providers.ResourceProvider;

public abstract class AClangAstTester {

    private static final boolean CLEAN_CLANG_FILES = !SpecsSystem.isDebug();
    private static final Set<String> JUNIT_TEST_ANNOTATIONS = Set.of(
            "org.junit.jupiter.api.Test",
            "org.junit.jupiter.api.RepeatedTest",
            "org.junit.jupiter.api.TestFactory",
            "org.junit.jupiter.api.TestTemplate",
            "org.junit.jupiter.params.ParameterizedTest");
    private static final Map<String, AtomicInteger> TESTER_INVOCATIONS = new ConcurrentHashMap<>();

    private File outputFolder;
    private final Map<File, String> copiedResourceKeys = new LinkedHashMap<>();
    private String benchmarkTestId;
    private int benchmarkTesterInvocation;

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

        if (AstWireBenchmarkIdentity.isEnabled()) {
            benchmarkTestId = currentJUnitTestId();
            benchmarkTesterInvocation = TESTER_INVOCATIONS
                    .computeIfAbsent(benchmarkTestId, ignored -> new AtomicInteger())
                    .incrementAndGet();
        } else {
            benchmarkTestId = null;
            benchmarkTesterInvocation = 0;
        }

        try {
            setUp();
            testProper();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                cleanupInstance();
            } catch (Exception e) {
                // Log but don't fail the test if cleanup fails
                SpecsLogs.info("Failed to cleanup test folder: " + e.getMessage());
            }
            benchmarkTestId = null;
            benchmarkTesterInvocation = 0;
        }

    }

    public void setUp() throws Exception {
        SpecsSystem.programStandardInit();

        outputFolder = Files.createTempDirectory("temp-clang-ast-").toFile();
        copiedResourceKeys.clear();
        for (ResourceProvider resource : resources) {
            File copiedFile = SpecsIo.resourceCopy(resource.getResource(), outputFolder, false, true);
            assertTrue(copiedFile.isFile(), "Could not copy resource '" + resource + "'");
            copiedResourceKeys.put(copiedFile, resource.getResource());
        }

    }

    public void cleanupInstance() throws Exception {
        if (CLEAN_CLANG_FILES) {
            SpecsIo.deleteFolder(outputFolder);
        }
    }

    public void testProper() {

        // Enable parallel parsing
        codeParser.set(ParallelCodeParser.PARALLEL_PARSING);

        File workFolder = outputFolder;

        // Parse files
        codeParser.set(CodeParser.GENERATED_PARSE_ROOT, workFolder);
        App clavaAst;
        try (AstWireBenchmarkIdentity.Registration ignored = registerBenchmarkIdentity(workFolder, "original")) {
            clavaAst = codeParser.parse(Arrays.asList(workFolder), compilerOptions);
        }

        File firstOutputFolder = SpecsIo.mkdir(new File(outputFolder, "outputFirst"));
        clavaAst.write(firstOutputFolder);
        if (onePass) {
            return;
        }

        CodeParser testCodeParser = CodeParser.newInstance();

        // Set same options as original code parser
        testCodeParser.set(codeParser);


        // Parse output again, check if files are the same
        testCodeParser.set(CodeParser.GENERATED_PARSE_ROOT, firstOutputFolder);
        App testClavaAst;
        try (AstWireBenchmarkIdentity.Registration ignored = registerBenchmarkIdentity(firstOutputFolder, "roundtrip")) {
            testClavaAst = testCodeParser.parse(Arrays.asList(firstOutputFolder), compilerOptions);
        }

        File secondOutputFolder = SpecsIo.mkdir(new File(outputFolder, "outputSecond"));
        testClavaAst.write(secondOutputFolder);
        // System.out.println("STOREDEF CACHE:\n" + StoreDefinitions.getStoreDefinitionsCache().getAnalytics());

        // Test if files from first and second are the same
        Map<String, File> outputFiles1 = SpecsIo.getFiles(firstOutputFolder)
                .stream()
                .collect(Collectors.toMap(file -> file.getName(), file -> file));

        Map<String, File> outputFiles2 = SpecsIo.getFiles(secondOutputFolder)
                .stream()
                .collect(Collectors.toMap(file -> file.getName(), file -> file));

        for (String name : outputFiles1.keySet()) {

            // Get corresponding file in output 2
            File outputFile2 = outputFiles2.get(name);

            assertNotNull(outputFile2, "Could not find second version of file '" + name + "'");
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

            assertEquals(txtContents, generatedFileContents);
        }

        // Idempotence test
        if (idempotenceTest) {
            testIdempotence(outputFiles1, outputFiles2);
        }
    }

    private AstWireBenchmarkIdentity.Registration registerBenchmarkIdentity(File parseRoot, String parsePass) {
        if (!AstWireBenchmarkIdentity.isEnabled() || benchmarkTestId == null) {
            return AstWireBenchmarkIdentity.Registration.NO_OP;
        }

        Map<File, String> identities = new HashMap<>();
        if (parseRoot.equals(outputFolder)) {
            identities.putAll(copiedResourceKeys);
        } else {
            Map<String, List<String>> resourcesByFilename = copiedResourceKeys.entrySet().stream()
                    .collect(Collectors.groupingBy(entry -> entry.getKey().getName(),
                            Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

            try (var files = Files.walk(parseRoot.toPath())) {
                files.filter(Files::isRegularFile).forEach(path -> {
                    File file = path.toFile();
                    List<String> resourceKeys = resourcesByFilename.getOrDefault(file.getName(), List.of());
                    String resourceKey = resourceKeys.size() == 1
                            ? resourceKeys.get(0)
                            : "generated/" + parseRoot.toPath().relativize(path).toString()
                                    .replace(File.separatorChar, '/');
                    identities.put(file, resourceKey);
                });
            } catch (java.io.IOException e) {
                throw new RuntimeException("Could not enumerate roundtrip sources for benchmark identity", e);
            }
        }

        return AstWireBenchmarkIdentity.register(
                identities, benchmarkTestId, benchmarkTesterInvocation, parsePass);
    }

    private static String currentJUnitTestId() {
        return StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).walk(frames -> frames
                .filter(AClangAstTester::isJUnitTestFrame)
                .findFirst()
                .map(frame -> frame.getClassName() + "#" + frame.getMethodName())
                .orElse("unknown"));
    }

    private static boolean isJUnitTestFrame(StackWalker.StackFrame frame) {
        try {
            MethodType methodType = MethodType.fromMethodDescriptorString(
                    frame.getDescriptor(), frame.getDeclaringClass().getClassLoader());
            Method method = frame.getDeclaringClass().getDeclaredMethod(
                    frame.getMethodName(), methodType.parameterArray());
            for (Annotation annotation : method.getDeclaredAnnotations()) {
                if (JUNIT_TEST_ANNOTATIONS.contains(annotation.annotationType().getName())) {
                    return true;
                }
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // The stack can contain synthetic frames or methods unavailable to reflection.
        }

        return false;
    }

    private void testIdempotence(Map<String, File> outputFiles1, Map<String, File> outputFiles2) {
        for (String name : outputFiles1.keySet()) {
            // Get corresponding file in output 1
            var outputFile1 = outputFiles1.get(name);

            // Get corresponding file in output 2
            var outputFile2 = outputFiles2.get(name);

            var normalizedFile1 = SpecsStrings.normalizeFileContents(SpecsIo.read(outputFile1), true);
            var normalizedFile2 = SpecsStrings.normalizeFileContents(SpecsIo.read(outputFile2), true);

            assertEquals(normalizedFile1, normalizedFile2);
        }
    }

}

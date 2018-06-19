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

package pt.up.fe.specs.cxxweaver;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.lara.interpreter.joptions.config.interpreter.LaraiKeys;
import org.lara.interpreter.joptions.config.interpreter.VerboseLevel;
import org.lara.interpreter.joptions.keys.FileList;
import org.lara.interpreter.joptions.keys.OptionalFile;
import org.suikasoft.jOptions.Interfaces.DataStore;

import larai.LaraI;
import pt.up.fe.specs.clang.ClangAstParser;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.language.Standard;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.options.CxxWeaverOption;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.providers.ResourceProvider;

public class ClavaWeaverTester {

    private static final boolean DEBUG = false;

    private static final String WORK_FOLDER = "cxx_weaver_output";

    private final String basePackage;
    private final Standard standard;
    private final String compilerFlags;

    private boolean checkWovenCodeSyntax;
    private String srcPackage;
    private String resultPackage;
    private String resultsFile;
    // private boolean debug;

    public ClavaWeaverTester(String basePackage, Standard standard, String compilerFlags) {
        this.basePackage = basePackage;
        this.standard = standard;
        this.compilerFlags = compilerFlags;

        this.checkWovenCodeSyntax = true;
        srcPackage = null;
        resultPackage = null;
        resultsFile = null;
        // debug = false;
    }

    /**
     * 
     * @param checkWovenCodeSyntax
     * @return the previous value
     */
    public ClavaWeaverTester setCheckWovenCodeSyntax(boolean checkWovenCodeSyntax) {
        this.checkWovenCodeSyntax = checkWovenCodeSyntax;

        return this;
    }

    public ClavaWeaverTester(String basePackage, Standard standard) {
        this(basePackage, standard, "");
    }

    public ClavaWeaverTester setResultPackage(String resultPackage) {
        this.resultPackage = sanitizePackage(resultPackage);

        return this;
    }

    public ClavaWeaverTester setSrcPackage(String srcPackage) {
        this.srcPackage = sanitizePackage(srcPackage);

        return this;
    }

    // public ClavaWeaverTester debug() {
    // this.debug = true;
    //
    // return this;
    // }

    public void setResultsFile(String resultsFile) {
        this.resultsFile = resultsFile;
    }

    private String sanitizePackage(String packageName) {
        String sanitizedPackage = packageName;
        if (!sanitizedPackage.endsWith("/")) {
            sanitizedPackage += "/";
        }

        return sanitizedPackage;
    }

    private ResourceProvider buildCodeResource(String codeResourceName) {
        StringBuilder builder = new StringBuilder();

        builder.append(basePackage);
        if (srcPackage != null) {
            builder.append(srcPackage);
        }

        builder.append(codeResourceName);

        return () -> builder.toString();
    }

    public void test(String laraResource, String... codeResource) {
        test(laraResource, Arrays.asList(codeResource));
    }

    public void test(String laraResource, List<String> codeResources) {
        SpecsLogs.msgInfo("\n---- Testing '" + laraResource + "' ----\n");
        List<ResourceProvider> codes = SpecsCollections.map(codeResources, this::buildCodeResource);

        File log = runCxxWeaver(() -> basePackage + laraResource, codes);
        String logContents = SpecsIo.read(log);

        StringBuilder expectedResourceBuilder = new StringBuilder();
        expectedResourceBuilder.append(basePackage);
        if (resultPackage != null) {
            expectedResourceBuilder.append(resultPackage);
        }

        String actualResultsFile = resultsFile != null ? resultsFile : laraResource + ".txt";

        // expectedResourceBuilder.append(laraResource).append(".txt");
        expectedResourceBuilder.append(actualResultsFile);

        String expectedResource = expectedResourceBuilder.toString();
        // String expectedResource = basePackage + laraResource + ".txt";
        if (!SpecsIo.hasResource(expectedResource)) {
            SpecsLogs.msgInfo("Could not find resource '" + expectedResource
                    + "', skipping verification. Actual output:\n" + logContents);
            return;
        }

        assertEquals(normalize(SpecsIo.getResource(expectedResource)), normalize(logContents));
    }

    /**
     * Normalizes endlines
     *
     * @param resource
     * @return
     */
    private static String normalize(String string) {
        return SpecsStrings.normalizeFileContents(string, true);
        // return string.replaceAll("\r\n", "\n");
    }

    private File runCxxWeaver(ResourceProvider lara, List<ResourceProvider> code) {
        // Prepare folder
        File workFolder = SpecsIo.mkdir(WORK_FOLDER);
        SpecsIo.deleteFolderContents(workFolder);

        // Prepare files

        code.forEach(resource -> resource.write(workFolder));
        // code.forEach(resource -> SpecsIo.resourceCopy(resource.getResource(), workFolder, true, true));
        File laraFile = lara.write(workFolder);

        DataStore data = DataStore.newInstance("CxxWeaverTest");

        // Set LaraI configurations
        data.add(LaraiKeys.LARA_FILE, laraFile);
        data.add(LaraiKeys.OUTPUT_FOLDER, workFolder);
        data.add(LaraiKeys.WORKSPACE_FOLDER, FileList.newInstance(workFolder));
        data.add(LaraiKeys.VERBOSE, VerboseLevel.errors);
        // data.add(LaraiKeys.DEBUG_MODE, true);
        data.add(LaraiKeys.LOG_JS_OUTPUT, Boolean.TRUE);
        data.add(LaraiKeys.LOG_FILE, OptionalFile.newInstance(getWeaverLog().getAbsolutePath()));

        // Set CxxWeaver configurations~
        if (standard != null) {
            data.set(ClavaOptions.STANDARD, standard);
        }

        data.set(ClavaOptions.FLAGS, compilerFlags);
        data.set(CxxWeaverOption.CHECK_SYNTAX, checkWovenCodeSyntax);
        data.set(CxxWeaverOption.DISABLE_CLAVA_INFO, false);
        // TEMP
        // data.set(ClavaOptions.DISABLE_NEW_PARSING_METHOD, true);

        if (DEBUG) {
            data.set(CxxWeaverOption.CLEAN_INTERMEDIATE_FILES, false);
        }

        CxxWeaver weaver = new CxxWeaver();
        try {
            boolean result = LaraI.exec(data, weaver);
            // Check weaver executed correctly
            assertTrue(result);
        } catch (Exception e) {
            // After LaraI execution, static weaver is unset, and it is no longer safe to use the weaver instance,
            // unless we set the weaver again
            if (weaver.getAppTry().isPresent()) {
                weaver.setWeaver();
                SpecsLogs.msgInfo("Current code:\n" + weaver.getApp().getCode());
                weaver.removeWeaver();
            } else {
                SpecsLogs.msgInfo("App not created");
            }

            throw new RuntimeException("Problems during weaving", e);
        }

        // Return true if result is 0
        // return result == 0 ? true : false;

        return getWeaverLog();
    }

    public static File getWorkFolder() {
        return new File(WORK_FOLDER);
    }

    public static File getWeaverLog() {
        return new File(WORK_FOLDER, "test.log");
    }

    // public static void clean() {
    // clean(DEBUG);
    // }
    //
    // public static void clean(boolean isDebug) {
    public static void clean() {
        if (DEBUG) {
            return;
        }
        // Delete CWeaver folder
        File workFolder = ClavaWeaverTester.getWorkFolder();
        SpecsIo.deleteFolder(workFolder);

        // Delete weaver files
        ClangAstParser.getTempFiles().stream()
                .forEach(filename -> new File(filename).delete());
    }

}

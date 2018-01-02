/**
 * Copyright 2017 SPeCS.
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

package pt.up.fe.specs.clava.tester;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import pt.up.fe.specs.clang.SupportedPlatform;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.csv.CsvWriter;
import pt.up.fe.specs.util.properties.SpecsProperties;
import pt.up.fe.specs.util.providers.FileResourceProvider.ResourceWriteData;
import pt.up.fe.specs.util.providers.WebResourceProvider;
import pt.up.fe.specs.util.providers.impl.GenericWebResourceProvider;
import pt.up.fe.specs.util.utilities.ProgressCounter;
import pt.up.fe.specs.util.utilities.StringLines;

public class ClavaTesterLauncher {

    private static final String WEBRESOURCES_FILENAME = "clavatester.webresources";
    private static final String CLEAN_BUILD_ARG = "-clean-build";
    private static final String CLEAN_DATA_ARG = "-clean-data";
    // private static final List<String> ARGS = Arrays.asList("-clean-build", "-clean-data");

    public static void main(String[] args) {
        SpecsSystem.programStandardInit();

        ClavaTesterArgs clavaTesterArgs = parseArgs(args);

        if (clavaTesterArgs.isCleanBuild()) {
            cleanBuild(clavaTesterArgs.getBaseFolder());
        }

        if (clavaTesterArgs.isCleanData()) {
            cleanData(clavaTesterArgs.getBaseFolder());
        }

        File baseFolder = clavaTesterArgs.getBaseFolder();
        SpecsProperties clavaTesterProps = getProps(baseFolder, args);

        // Detect platform
        SupportedPlatform platform = SupportedPlatform.getCurrentPlatform();

        // Prepare web-resources
        prepareWebResources(baseFolder);

        // Find Clava configuration files for the current platform
        String suffix = "-" + platform.getName() + ".clava";

        List<File> clavaConfigs = SpecsIo.getFilesRecursive(baseFolder).stream()
                .filter(file -> file.getName().toLowerCase().endsWith(suffix))
                .collect(Collectors.toList());

        // Sort, in Linux file listing usually are not sorted
        Collections.sort(clavaConfigs);

        // Create tester
        ClavaTester tester = new ClavaTester(platform, clavaTesterProps);

        // Incrementally write results
        CsvWriter csvWriter = new CsvWriter("Clava config", "Status", "Stage", "Error message");
        File csvFile = new File(baseFolder, "clava-tester-results-" + platform + ".csv");

        ProgressCounter progress = new ProgressCounter(clavaConfigs.size());
        SpecsLogs.msgInfo("Found " + clavaConfigs.size() + " Clava configuration files");
        Map<File, ClavaTestResult> results = new LinkedHashMap<>();
        for (File clavaConfig : clavaConfigs) {
            SpecsLogs.msgInfo("Testing '" + clavaConfig.getAbsolutePath() + "' " + progress.next());
            ClavaTestResult result = tester.test(clavaConfig);
            results.put(clavaConfig, result);

            if (result.isSuccess()) {
                SpecsLogs.msgInfo("Execution ok\n");
            } else {
                SpecsLogs.msgInfo("Execution failed:\n" + result.getErrorMessage().get() + "\n");
                if (result.isException()) {
                    result.getException().printStackTrace();
                }
            }

            SpecsLogs.msgInfo("-------------------------\n");

            // Save CSV
            csvWriter.addLine(toCsvLine(clavaConfig, result));
            SpecsIo.write(csvFile, csvWriter.buildCsv());

        }

        // Save results
        // saveResults(results, baseFolder);

    }

    private static void prepareWebResources(File baseFolder) {
        List<String> webResourcesLines = getWebResources(baseFolder);
        if (webResourcesLines.isEmpty()) {
            return;
        }

        SpecsLogs.msgInfo("Found " + webResourcesLines.size() + " resources to download");
        ProgressCounter progress = new ProgressCounter(webResourcesLines.size());
        for (String webResource : webResourcesLines) {
            SpecsLogs.msgInfo("Processing " + webResource + "... " + progress.next());

            WebResourceProvider webResourceProvider = parseWebResource(webResource).orElse(null);
            if (webResourceProvider == null) {
                continue;
            }

            // // Split into URL and version
            // String[] resourceParts = webResource.split(",");
            // if (resourceParts.length == 1) {
            // LoggingUtils.msgInfo(
            // "Expected to find one ',' character, separating the URL from the version: " + webResource);
            // continue;
            // }
            //
            // if (resourceParts.length > 2) {
            // LoggingUtils.msgInfo(
            // "Expected to find only one ',' character, separating the URL from the version: " + webResource);
            // }
            //
            // String url = resourceParts[0].trim();
            // String version = resourceParts[1].trim();
            //
            // int rootEndIndex = url.lastIndexOf('/');
            // if (rootEndIndex == -1) {
            // LoggingUtils.msgInfo("Expected to find at least one '/' in the URL: " + url);
            // continue;
            // }
            //
            // if (rootEndIndex == url.length() - 1) {
            // LoggingUtils.msgInfo("URL should represent a file and not end with a '/': " + url);
            // continue;
            // }
            //
            // String rootUrl = url.substring(0, rootEndIndex + 1);
            // String resourceUrl = url.substring(rootEndIndex + 1);
            //
            // if (!resourceUrl.endsWith(".zip")) {
            // LoggingUtils.msgInfo("Expected URL to end with '.zip': " + url);
            // continue;
            // }
            //
            // WebResourceProvider webResourceProvider = new GenericWebResourceProvider(rootUrl, resourceUrl, version);
            ResourceWriteData zipData = webResourceProvider.writeVersioned(baseFolder, ClavaTesterLauncher.class,
                    false);

            if (!zipData.isNewFile()) {
                SpecsLogs.msgInfo("File '" + webResourceProvider.getResourceUrl() + "' is up-to-date");
                continue;
            }

            SpecsIo.extractZip(zipData.getFile(), baseFolder);

            // Overwrite zip file, to reduce size, and keep it there for book-keeping of next versions
            SpecsIo.write(zipData.getFile(),
                    "File was successfully extracted (current version: " + webResourceProvider.getVersion() + ")");
        }

    }

    private static List<String> getWebResources(File baseFolder) {
        File webResources = new File(baseFolder, WEBRESOURCES_FILENAME);

        if (!webResources.isFile()) {
            SpecsLogs
                    .msgInfo("Did not find a '" + WEBRESOURCES_FILENAME + "' file, skipping download of web resources");
            return Collections.emptyList();
        }

        return StringLines.getLines(webResources);
    }

    private static Optional<WebResourceProvider> parseWebResource(String webResource) {
        // Split into URL and version
        String[] resourceParts = webResource.split(",");
        if (resourceParts.length == 1) {
            SpecsLogs.msgInfo(
                    "Expected to find one ',' character, separating the URL from the version: " + webResource);
            return Optional.empty();
        }

        if (resourceParts.length > 2) {
            SpecsLogs.msgInfo(
                    "Expected to find only one ',' character, separating the URL from the version: " + webResource);
        }

        String url = resourceParts[0].trim();
        String version = resourceParts[1].trim();

        int rootEndIndex = url.lastIndexOf('/');
        if (rootEndIndex == -1) {
            SpecsLogs.msgInfo("Expected to find at least one '/' in the URL: " + url);
            return Optional.empty();
        }

        if (rootEndIndex == url.length() - 1) {
            SpecsLogs.msgInfo("URL should represent a file and not end with a '/': " + url);
            return Optional.empty();
        }

        String rootUrl = url.substring(0, rootEndIndex + 1);
        String resourceUrl = url.substring(rootEndIndex + 1);

        if (!resourceUrl.endsWith(".zip")) {
            SpecsLogs.msgInfo("Expected URL to end with '.zip': " + url);
            return Optional.empty();
        }

        WebResourceProvider webResourceProvider = new GenericWebResourceProvider(rootUrl, resourceUrl, version);

        return Optional.of(webResourceProvider);
    }

    private static List<String> toCsvLine(File clavaConfig, ClavaTestResult result) {
        List<String> line = new ArrayList<>();

        line.add(clavaConfig.getAbsolutePath());
        line.add(result.isSuccess() ? "Pass" : "Fail");
        line.add(result.getLastExecutedStage().toString());
        line.add(SpecsStrings.escapeJson(result.getErrorMessage().orElse("")));

        return line;
    }

    /*
    private static void saveResults(Map<File, ClavaTestResult> results, File folder) {
        CsvWriter csvWriter = new CsvWriter();
        csvWriter.setHeader(Arrays.asList("Clava config", "Status", "Stage", "Error message"));
    
        for (Entry<File, ClavaTestResult> entry : results.entrySet()) {
            List<String> line = new ArrayList<>();
            line.add(entry.getKey().getAbsolutePath());
            line.add(entry.getValue().isSuccess() ? "Pass" : "Fail");
            line.add(entry.getValue().getLastExecutedStage().toString());
            line.add(ParseUtils.escapeJson(entry.getValue().getErrorMessage().orElse("")));
    
            csvWriter.addLine(line);
        }
    
        IoUtils.write(new File(folder, "clava-tester-results.csv"), csvWriter.buildCsv());
    }
    */
    private static SpecsProperties getProps(File baseFolder, String[] args) {
        if (args.length < 2) {
            SpecsLogs.msgInfo("No properties file defined, returning default properties");
            return SpecsProperties.newEmpty();
        }

        File propsFile = new File(baseFolder, args[1]);
        if (!propsFile.isFile()) {
            SpecsLogs.msgInfo("Properties file '" + args[1] + "' not fould, returning default properties");
        }

        return SpecsProperties.newInstance(propsFile);
    }

    private static ClavaTesterArgs parseArgs(String[] args) {
        List<String> argsList = new ArrayList<>(Arrays.asList(args));

        // Check if there are flags
        boolean cleanBuild = processArg(argsList, CLEAN_BUILD_ARG);
        boolean cleanData = processArg(argsList, CLEAN_DATA_ARG);

        // If no arguments, assum current folder
        if (args.length == 0) {
            File baseFolder = SpecsIo.getWorkingDir();
            SpecsLogs.msgInfo("No arguments given, assuming current folder (" + baseFolder.getAbsolutePath() + ")");
            return new ClavaTesterArgs(baseFolder, cleanBuild, cleanData);
        }

        File baseFolder = SpecsIo.mkdir(args[0]);
        SpecsLogs.msgInfo("Using '" + baseFolder.getAbsolutePath() + "' as base folder.");
        return new ClavaTesterArgs(baseFolder, cleanBuild, cleanData);
    }

    private static boolean processArg(List<String> argsList, String booleanArg) {
        Integer cleanIndex = null;

        // Find arg
        for (int i = 0; i < argsList.size(); i++) {
            String arg = argsList.get(i);
            if (arg.toLowerCase().equals(booleanArg)) {
                cleanIndex = i;
                break;
            }
        }

        // Clean arg found
        if (cleanIndex != null) {
            argsList.remove(cleanIndex);
            return true;
        }

        return false;
    }

    /**
     * Deletes build folders and woven code folders
     * 
     * @param baseFolder
     */
    private static void cleanBuild(File baseFolder) {
        List<File> foldersToClean = SpecsIo.getFoldersRecursive(baseFolder).stream()
                .filter(ClavaTesterLauncher::isBuildFolder)
                .collect(Collectors.toList());

        SpecsLogs.msgInfo("Build cleaning: found " + foldersToClean.size() + " folders to clean");
        ProgressCounter progress = new ProgressCounter(foldersToClean.size());

        Collections.sort(foldersToClean);

        for (File folderToClean : foldersToClean) {
            SpecsLogs
                    .msgInfo("Deleting folder '" + folderToClean.getAbsolutePath() + "'... " + progress.next());
            SpecsIo.deleteFolder(folderToClean);
        }

    }

    private static void cleanData(File baseFolder) {
        List<File> foldersToClean = SpecsIo.getFoldersRecursive(baseFolder).stream()
                .filter(ClavaTesterLauncher::isDataFolder)
                .collect(Collectors.toList());

        SpecsLogs.msgInfo("Data cleaning: found " + foldersToClean.size() + " folders to clean");
        ProgressCounter progress = new ProgressCounter(foldersToClean.size());

        Collections.sort(foldersToClean);

        for (File folderToClean : foldersToClean) {
            SpecsLogs
                    .msgInfo("Deleting folder '" + folderToClean.getAbsolutePath() + "'... " + progress.next());
            SpecsIo.deleteFolder(folderToClean);
        }

        // Delete webresources
        for (String webResource : getWebResources(baseFolder)) {
            WebResourceProvider parsedResource = parseWebResource(webResource).orElse(null);
            if (parsedResource == null) {
                continue;
            }

            File webFile = new File(baseFolder, parsedResource.getFilename());
            if (webFile.isFile()) {
                SpecsLogs.msgInfo("Deleting web-resource '" + webFile.getAbsolutePath() + "'");
                webFile.delete();
            }

        }

    }

    private static boolean isBuildFolder(File folder) {
        String folderName = folder.getName();

        // This is based on the .gitignore file

        if (folderName.equals("build")) {
            return true;
        }

        if (folderName.startsWith("build-")) {
            return true;
        }

        if (folderName.equals("weaved_code")) {
            return true;
        }

        if (folderName.equals("woven_code")) {
            return true;
        }

        return false;
    }

    private static boolean isDataFolder(File folder) {
        String folderName = folder.getName();

        // This is based on the .gitignore file

        if (folderName.equals("data")) {
            return true;
        }

        return false;
    }

}

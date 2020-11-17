/**
 * Copyright 2020 SPeCS.
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

package pt.up.fe.specs.tupatcher;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.suikasoft.jOptions.JOptionsUtils;
import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.app.App;
import org.suikasoft.jOptions.persistence.PropertiesPersistence;
import org.suikasoft.jOptions.streamparser.LineStreamParser;

import pt.up.fe.specs.tupatcher.parser.TUErrorParser;
import pt.up.fe.specs.tupatcher.parser.TUErrorsData;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.system.ProcessOutput;
import pt.up.fe.specs.util.utilities.LineStream;

public class TUPatcherLauncher {

    public static final DataKey<String> SOURCE_PATHS = KeyFactory.string("sourcePaths");
    public static final DataKey<String> MAX_FILES = KeyFactory.string("maxFiles", "600");
    public static final DataKey<String> MAX_ITERATIONS = KeyFactory.string("maxIterations", "100");

    // private static final String DUMPER_EXE =
    // "../TranslationUnitErrorDumper/cmake-build-debug/TranslationUnitErrorDumper";
    private static final String DUMPER_EXE = "../../TranslationUnitErrorDumper/build/TranslationUnitErrorDumper.exe";

    // private final SpecsProperties properties;
    private final TUPatcherConfig config;

    // public TUPatcherLauncher(SpecsProperties properties) {
    public TUPatcherLauncher(TUPatcherConfig config) {
        this.config = config;

        // this.properties = properties;
    }

    public static void main(String[] args) {

        SpecsSystem.programStandardInit();

        var storeDefinition = TUPatcherConfig.getDefinition();
        var app = App.newInstance("Translation Unit Patcher", storeDefinition,
                new PropertiesPersistence(storeDefinition),
                dataStore -> new TUPatcherLauncher(new TUPatcherConfig(dataStore)).execute());

        JOptionsUtils.executeApp(app, Arrays.asList(args));

        // File propertiesFile = new File("patcher.properties");
        // SpecsProperties properties = propertiesFile.isFile() ? SpecsProperties.newInstance(propertiesFile)
        // : parseArguments(args);
        //
        // var tuPatcher = new TUPatcherLauncher(properties);
        // tuPatcher.execute();

    }

    public int execute() {
        var sourcePaths = config.get(TUPatcherConfig.SOURCE_PATHS).getStringList();
        // System.out.println("SOURCE PATHS: " + sourcePaths);
        ArrayList<PatchData> data = new ArrayList<>();
        // Get list of all the files in form of String Array
        for (String arg : sourcePaths) {
            File file = new File(arg);
            if (file.isDirectory()) {
                data = patchDirectory(file);
            } else {
                data.add(patchOneFile(file, null));
            }
        }
        /*
        for (PatchData d : data) {
            if (d.getErrors().size() > 0)
            System.out.println(d.getErrors().get(d.getErrors().size()-1));            
        }*/

        return 0;
    }

    // private static SpecsProperties parseArguments(String[] args) {
    // // Doing nothing, for now
    // return SpecsProperties.newEmpty();
    // }

    /**
     * Create patches for all .c and .cpp files in a directory
     * 
     * @param dir
     * @return ArrayList with a PatchData for each file
     */
    public ArrayList<PatchData> patchDirectory(File dir) {
        var sourceFiles = SpecsIo.getFilesRecursive(dir, Arrays.asList("c", "cpp"));

        // String path = SpecsIo.getCanonicalPath(dir);
        // String[] fileNames = dir.list();
        int numErrors = 0, numSuccess = 0;
        int maxNumFiles = config.get(TUPatcherConfig.MAX_FILES), n = 0;
        // List<String> fileNamesList = Arrays.asList(fileNames);
        // Collections.shuffle(fileNamesList);
        ArrayList<String> errorMessages = new ArrayList<>();
        ArrayList<PatchData> patchesData = new ArrayList<>();

        for (var sourceFile : sourceFiles) {
            n++;
            if (n > maxNumFiles)
                break;

            // var sourceExtension = SpecsIo.getExtension(sourceFile);
            // Does some pre-processing on the files... this should be moved to patchOneFile
            System.out.println("filename: " + sourceFile);
            String fileContent = SpecsIo.read(sourceFile);
            if (!(fileContent.substring(0, 4).equals("void")
                    || fileContent.substring(0, 13).equals("TYPE_PATCH_00"))) {
                // assure the function declaration has a return type
                // File cppFile = new File(path + "/" + arg);
                if (fileContent.contains("return;") || !fileContent.contains("return")) {
                    SpecsIo.write(sourceFile, "void " + fileContent);
                } else if (fileContent.contains("return")) {
                    SpecsIo.write(sourceFile, "TYPE_PATCH_00 " + fileContent);
                }
            }
            // String a = path + "/" + arg;
            try {
                patchesData.add(patchOneFile(sourceFile, dir));
            } catch (Exception e) {
                numErrors++;
                errorMessages.add(e.toString() + "\n\n" + e.getLocalizedMessage() + "\n" + e.getMessage());
                continue;
            }
            numSuccess++;

            // }
        }

        System.out.println("Number of cpp files: " + (numSuccess + numErrors));
        System.out.println("Number of errors: " + numErrors);
        System.out.println("Number of successful patches: " + numSuccess);
        /*for (String message : errorMessages) {
            System.out.println(message);
        }*/

        return patchesData;

    }

    /**
     * Create patch for a single .cpp file
     * 
     * @param filepath
     * @return PatchData
     */
    public PatchData patchOneFile(File filepath, File baseFolder) {

        var outputFolder = SpecsIo.mkdir(config.get(TUPatcherConfig.OUTPUT_FOLDER));

        // Get base output folder for the file
        var fileOutputFolder = baseFolder != null
                ? new File(outputFolder, SpecsIo.getRelativePath(filepath.getParentFile(), baseFolder))
                : outputFolder;

        var patchedFile = new File(fileOutputFolder, TUPatcherUtils.getPatchedFilename(filepath.getName()));

        // Copy file to output folder of file
        SpecsIo.copy(filepath, patchedFile);

        // System.out.println("PATCHING " + filepath);
        var patchData = new PatchData();

        List<String> command = new ArrayList<>();
        command.add(DUMPER_EXE);
        command.add(patchedFile.getAbsolutePath());
        command.add("--");
        command.add("-ferror-limit=1");

        // Always compile as C++
        command.add("-x");
        command.add("c++");

        // // System.out.println("RUNNING... " + command);
        // var output = SpecsSystem.runProcess(command, TUPatcherLauncher::outputProcessor,
        // inputStream -> TUPatcherLauncher.lineStreamProcessor(inputStream, patchData));
        // // System.out.println("FINISHED");
        // patchData.write(patchedFile);
        //
        // List<String> command2 = new ArrayList<>();
        //
        // command2.add(DUMPER_EXE);
        // command2.add("output/file.cpp");
        // command2.add("--");
        // command2.add("-ferror-limit=1");
        //
        // // Always compile as C++
        // command2.add("-x");
        // command2.add("c++");

        int n = 0;
        int maxIterations = 100;
        ProcessOutput<Boolean, TUErrorsData> output = null;
        while (n < maxIterations) {
            output = SpecsSystem.runProcess(command,
                    TUPatcherLauncher::outputProcessor,
                    inputStream -> TUPatcherLauncher.lineStreamProcessor(inputStream, patchData));
            patchData.write(filepath, patchedFile);
            n++;
            if (n >= maxIterations) {
                System.out.println();
                /*for (ErrorKind error : patchData.getErrors()) {
                    System.out.println(error);
                }*/
                System.out.println("Program status: " + output.getReturnValue());
                System.out.println("Std out result: " + output.getStdOut());
                System.out.println("Std err result: " + output.getStdErr());
                throw new RuntimeException("Maximum number of iterations exceeded. Could not solve errors");
            }
            // System.out.print('.');

            // No more errors, break
            if (output.getStdErr().get(TUErrorsData.ERRORS).isEmpty()) {
                break;
            }
        }

        if (n >= maxIterations) {
            System.out.println("!Maximum number of iterations exceeded. Could not solve all errors");
        }

        if (output == null) {
            System.out.println("Did not run patcher even once!");
        } else {
            System.out.println();
            System.out.println("Program status: " + output.getReturnValue());
            System.out.println("Std out result: " + output.getStdOut());
            System.out.println("Std err result: " + output.getStdErr());
        }

        /* System.out.println("Errors found: ");
        for (ErrorKind error : patchData.getErrors()) {
            System.out.println(error);
        }*/
        return patchData;

    }

    /**
     * Create patch for a single .cpp file
     * 
     * @param filepath
     * @return PatchData
     */
    public PatchData patchOneFileV1(File filepath, File baseFolder) {

        var outputFolder = SpecsIo.mkdir(config.get(TUPatcherConfig.OUTPUT_FOLDER));

        // Get base output folder for the file
        var fileOutputFolder = baseFolder != null
                ? new File(outputFolder, SpecsIo.getRelativePath(filepath.getParentFile(), baseFolder))
                : outputFolder;

        var patchedFile = new File(fileOutputFolder, filepath.getName());

        // Copy file to output folder of file
        SpecsIo.copy(filepath, patchedFile);

        // System.out.println("PATCHING " + filepath);
        var patchData = new PatchData();

        List<String> command = new ArrayList<>();
        command.add(DUMPER_EXE);
        command.add(filepath.getAbsolutePath());
        command.add("--");
        command.add("-ferror-limit=1");

        // Always compile as C++
        command.add("-x");
        command.add("c++");
        // System.out.println("RUNNING... " + command);
        var output = SpecsSystem.runProcess(command, TUPatcherLauncher::outputProcessor,
                inputStream -> TUPatcherLauncher.lineStreamProcessor(inputStream, patchData));
        // System.out.println("FINISHED");
        patchData.write(filepath, filepath);

        List<String> command2 = new ArrayList<>();

        command2.add(DUMPER_EXE);
        command2.add("output/file.cpp");
        command2.add("--");
        command2.add("-ferror-limit=1");

        // Always compile as C++
        command2.add("-x");
        command2.add("c++");

        int n = 0;
        int maxIterations = 100;
        while (!output.getStdErr().get(TUErrorsData.ERRORS).isEmpty() && n < maxIterations) {
            output = SpecsSystem.runProcess(command2, TUPatcherLauncher::outputProcessor,
                    inputStream -> TUPatcherLauncher.lineStreamProcessor(inputStream, patchData));
            patchData.write(filepath, filepath);
            n++;
            if (n >= maxIterations) {
                System.out.println();
                /*for (ErrorKind error : patchData.getErrors()) {
                    System.out.println(error);
                }*/
                System.out.println("Program status: " + output.getReturnValue());
                System.out.println("Std out result: " + output.getStdOut());
                System.out.println("Std err result: " + output.getStdErr());
                throw new RuntimeException("Maximum number of iterations exceeded. Could not solve errors");
            }
            // System.out.print('.');
        }
        System.out.println();
        System.out.println("Program status: " + output.getReturnValue());
        System.out.println("Std out result: " + output.getStdOut());
        System.out.println("Std err result: " + output.getStdErr());

        /* System.out.println("Errors found: ");
        for (ErrorKind error : patchData.getErrors()) {
            System.out.println(error);
        }*/
        return patchData;

    }

    public static Boolean outputProcessor(InputStream stream) {
        try (var lines = LineStream.newInstance(stream, "Input Stream");) {
            while (lines.hasNextLine()) {
                lines.nextLine();
                // var line = lines.nextLine();
                // System.out.println("StdOut: " + line);
            }
        }

        return true;
    }

    public static String errorProcessor(InputStream stream) {
        try (var lines = LineStream.newInstance(stream, "Input Stream");) {
            while (lines.hasNextLine()) {
                var line = lines.nextLine();
                System.out.println("StdErr: " + line);
            }
        }

        return "Hello";
    }

    public static TUErrorsData lineStreamProcessor(InputStream stream, PatchData patchData) {
        // Create LineStreamParser
        try (LineStreamParser<TUErrorsData> lineStreamParser = TUErrorParser.newInstance()) {

            File dumpFile = null;

            // Parse input stream
            // String linesNotParsed =
            lineStreamParser.parse(stream, dumpFile);

            var data = lineStreamParser.getData();

            // System.out.println("[TEST] lines not parsed:\n" + linesNotParsed);

            // System.out.println("[TEST] Collected data:\n" + data);

            new ErrorPatcher(patchData).patch(data);

            return data;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error while parsing output of Clang error dumper", e);
        }
    }

}

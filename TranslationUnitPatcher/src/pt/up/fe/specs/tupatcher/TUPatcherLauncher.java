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
import java.util.Collections;
import java.util.List;

import org.suikasoft.jOptions.streamparser.LineStreamParser;

import pt.up.fe.specs.tupatcher.parser.TUErrorParser;
import pt.up.fe.specs.tupatcher.parser.TUErrorsData;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.utilities.LineStream;

public class TUPatcherLauncher {

    
    public static void main (String[] args) {

        SpecsSystem.programStandardInit();
        File file = new File(args[0]);
        
        ArrayList<PatchData> data = new ArrayList<>();
        // Get list of all the files in form of String Array
        if (file.isDirectory()) {
            data = patchDirectory(file);
        }
        else {
            data.add(patchOneFile(args[0]));
        }
        /*
        for (PatchData d : data) {
            if (d.getErrors().size() > 0)
            System.out.println(d.getErrors().get(d.getErrors().size()-1));            
        }*/
    }
    
    public static ArrayList<PatchData> patchDirectory(File dir) {
        String path = dir.getAbsolutePath();
        String[] fileNames = dir.list(); 
        int numErrors = 0, numSuccess = 0;
        int maxNumFiles = 200, n=0;
        List<String> fileNamesList = Arrays.asList(fileNames);
        Collections.shuffle(fileNamesList);
        ArrayList<String> errorMessages = new ArrayList<>();
        ArrayList<PatchData> patchesData = new ArrayList<>();
        
        for (String arg : fileNamesList) {
            n++;
            if (n > maxNumFiles) break;
            String[] splitted = arg.split("\\.");
                
            if (splitted.length > 1) {
                if (splitted[1].equals("c")) {
                    File cFile = new File(path+"/"+arg);
                    File cppFile = new File(path+"/"+arg+"pp");
                    cFile.renameTo(cppFile);
                }
                if (splitted[1].equals("cpp")) {
                    System.out.println();
                    System.out.println("filename: "+arg);
                    String fileContent = SpecsIo.read(SpecsIo.existingFile(path+"/"+arg));
                    if (!(fileContent.substring(0,4).equals("void") || fileContent.substring(0,13).equals("TYPE_PATCH_00") )) {
                        //assure the function declaration has a return type
                        File cppFile = new File(path+"/"+arg);
                        if (fileContent.contains("return;") || !fileContent.contains("return")) {
                            SpecsIo.write(cppFile, "void "+fileContent);
                        }
                        else if (fileContent.contains("return")) {
                            SpecsIo.write(cppFile, "TYPE_PATCH_00 "+fileContent);
                        }
                    }
                    String a = path+"/"+arg;
                    try {
                        patchesData.add(patchOneFile(a));
                    }
                    catch (Exception e) {
                        numErrors++;
                        errorMessages.add(e.toString()+"\n\n"+e.getLocalizedMessage() + "\n" +e.getMessage());
                        continue;
                    }
                    numSuccess++;
                }
            }
        }
        
        System.out.println("Number of cpp files: " + (numSuccess + numErrors));
        System.out.println("Number of errors: " + numErrors);
        System.out.println("Number of successful patches: " + numSuccess);
        /*for (String message : errorMessages) {
            System.out.println(message);
        }*/
        
        return patchesData;

    }

    public static PatchData patchOneFile(String filepath) {

        var patchData = new PatchData();

        List<String> command = new ArrayList<>();
        command.add("../TranslationUnitErrorDumper/cmake-build-debug/TranslationUnitErrorDumper");
        command.add(filepath);
        command.add("--");

        var output = SpecsSystem.runProcess(command, TUPatcherLauncher::outputProcessor,
                inputStream -> TUPatcherLauncher.lineStreamProcessor(inputStream, patchData));

        patchData.write(filepath);
        
        List<String> command2 = new ArrayList<>();
        
        command2.add("../TranslationUnitErrorDumper/cmake-build-debug/TranslationUnitErrorDumper");
        command2.add("output/file.cpp");
        command2.add("--");
        int n = 0;
        int maxIterations = 100;
        while (!output.getStdErr().get(TUErrorsData.ERRORS).isEmpty() && n < maxIterations) {
            output = SpecsSystem.runProcess(command2, TUPatcherLauncher::outputProcessor,
                    inputStream -> TUPatcherLauncher.lineStreamProcessor(inputStream, patchData));
            patchData.write(filepath);
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
                var line = lines.nextLine();
                //System.out.println("StdOut: " + line);
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
            //String linesNotParsed = 
            lineStreamParser.parse(stream, dumpFile);

            var data = lineStreamParser.getData();

            //System.out.println("[TEST] lines not parsed:\n" + linesNotParsed);

            System.out.println("[TEST] Collected data:\n" + data);

            new ErrorPatcher(patchData).patch(data);

            return data;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error while parsing output of Clang error dumper", e);
        }
    }

}

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
import java.util.Arrays;
import java.util.List;

import pt.up.fe.specs.clang.ast.ClangNode;
import pt.up.fe.specs.clang.astlineparser.AstParser;
import pt.up.fe.specs.clang.codeparser.CodeParser;
import pt.up.fe.specs.clang.includes.ClangIncludes;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsSystem;
import pt.up.fe.specs.util.io.PathFilter;

public class ClavaSandbox {

    private static final String AST_FILE = "miniapp.ast";

    public static void main(String[] args) {

        SpecsSystem.programStandardInit();

        extractAst();
        // testAst();
    }

    private static void extractAst() {

        // File baseFolder = new File("test_c/");
        // List<String> files = Arrays.asList("test_c/typedef.c");

        // File baseFolder = new File("test/");
        // List<String> files = Arrays.asList("test/prepos.cpp");

        // File baseFolder = new File("cl/");
        // List<String> files = Arrays.asList("cl/montecarlo.cl");
        //
        // List<String> options = Arrays.asList("-x", "cl");

        // File baseFolder = new File("pdist/");
        // List<String> files = Arrays.asList("pdist/*.cpp");

        // File baseFolder = new File("betweenness/");
        // List<String> files = Arrays.asList("betweenness/*.cpp");

        List<File> files = SpecsIo.getPathsWithPattern(new File("./betweenness"), "*.cpp", true, PathFilter.FILES);
        // List<File> files = SpecsIo.getFilesWithPattern(new File("./betweenness"), "*.cpp");
        // System.out.println("SIMPLE GTE PATTERM:" + files);
        // System.out.println("NEW GTE PATTERM:"
        // + SpecsIo.getPathsWithPattern(new File("./betweenness"), "*", true, PathFilter.FOLDERS));
        // File baseFolder = new File("montecarlo/");
        // List<String> files = Arrays.asList("montecarlo/*.cpp");

        List<String> options = Arrays.asList(
                // "-std=c99"
                "-std=c++11"
        // "-fopenmp"
        // "-DTYPE=double",

        // Argo
        // "-Iinclude-antarex"
        );

        // long tic;

        // tic = System.nanoTime();
        App clavaAst = CodeParser.newInstance().parse(files, options);

        SpecsIo.write(new File("clavaDump.txt"), clavaAst.toString());
        // System.out.println("CLAVA TREE:\n" + clavaAst);

        clavaAst.write(SpecsIo.mkdir("./output"));

        String filename = "test.cpp";
        clavaAst.getFile(filename).ifPresent(tu -> System.out.println(filename + ":\n" + tu));

        /*        
        ClangRootNode ast = new ClangAstParser().parse(files, options);
        // SpecsLogs.msgInfo(SpecsStrings.takeTime("Clang Parsing and Dump", tic));
        // SpecsLogs.msgInfo("Current memory used (Java):" + SpecsStrings.parseSize(SpecsSystem.getUsedMemory(true)));
        // LoggingUtils.msgInfo("Heap size (Java):" + ParseUtils.parseSize(Runtime.getRuntime().maxMemory()));
        
        // System.out.println("CLANG AST:" + ast.getClangOutput());
        
        try (ClavaParser clavaParser = new ClavaParser(ast)) {
            tic = System.nanoTime();
            App clavaAst = clavaParser.parse();
            SpecsLogs.msgInfo(SpecsStrings.takeTime("Clang AST to Clava", tic));
            SpecsLogs
                    .msgInfo("Current memory used (Java):" + SpecsStrings.parseSize(SpecsSystem.getUsedMemory(true)));
            // LoggingUtils.msgInfo("Heap size (Java):" + ParseUtils.parseSize(Runtime.getRuntime().maxMemory()));
        
            // System.out.println("EXTRACTED INTS:" + IntegerLiteralParser.getCOUNTER());
        
            // long integers = clavaAst.getDescendantsAndSelfStream()
            // .filter(node -> node instanceof IntegerLiteral)
            // .count();
            // System.out.println("FOUND " + integers + " integers");
            // clavaAst.writeFromTopFile(mainFile, IoUtils.safeFolder("./output"));
        
            SpecsIo.write(new File("clavaDump.txt"), clavaAst.toString());
            // System.out.println("CLAVA TREE:\n" + clavaAst);
        
            clavaAst.write(SpecsIo.mkdir("./output"));
        
            String filename = "test.cpp";
            clavaAst.getFile(filename).ifPresent(tu -> System.out.println(filename + ":\n" + tu));
        } catch (Exception e) {
            SpecsLogs.msgWarn("Error message:\n", e);
        }
        */
    }

    public static void testAst() {
        // Read includes
        System.out.println("INCLUDES:\n" + ClangIncludes.newInstance(new File("includes.txt")));

        // Read Clang ast
        File astFile = SpecsIo.existingFile(ClavaSandbox.AST_FILE);

        List<ClangNode> nodes = new AstParser().parse(SpecsIo.read(astFile));

        for (ClangNode node : nodes) {
            System.out.println("Node:" + node.toContentString());
        }
        // System.out.println("Node:\n" + node);
        /*
        	// Parse Clang ast
        	try (LineStream astStream = LineStream.newInstance(astFile)) {
        	    String startLine = astStream.nextLine();
        
        	    while (startLine != null) {
        		// We are at the start of a Translation Unit
        
        		astStream.nextLine();
        	    }
        
        	}
        */
    }
}

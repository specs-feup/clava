/**
 * Copyright 2018 SPeCS.
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

package pt.up.fe.specs.clang.codeparser;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import pt.up.fe.specs.clang.ClangAstParser;
import pt.up.fe.specs.clang.ast.genericnode.ClangRootNode;
import pt.up.fe.specs.clang.clavaparser.ClavaParser;
import pt.up.fe.specs.clang.streamparserv2.ClangDumperParser;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.utils.SourceType;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;

public class CodeParser {

    private static final boolean ONLY_NEW_PARSE_METHOD = false;

    private boolean showClangDump;
    private boolean showClangAst;
    private boolean showClavaAst;
    private boolean showCode;
    private boolean useCustomResources;
    // private boolean disableNewParsingMethod;

    public CodeParser() {
        showClangDump = false;
        showClangAst = false;
        showClavaAst = false;
        showCode = false;
        useCustomResources = false;
        // disableNewParsingMethod = false;
    }

    public CodeParser setShowClangDump(boolean showClangDump) {
        this.showClangDump = showClangDump;
        return this;
    }

    public CodeParser setShowClangAst(boolean showClangAst) {
        this.showClangAst = showClangAst;
        return this;
    }

    public CodeParser setShowClavaAst(boolean showClavaAst) {
        this.showClavaAst = showClavaAst;
        return this;
    }

    public CodeParser setShowCode(boolean showCode) {
        this.showCode = showCode;
        return this;
    }

    // public CodeParser setDisableNewParsingMethod(boolean disableNewParsingMethod) {
    // this.disableNewParsingMethod = disableNewParsingMethod;
    // return this;
    // }

    public CodeParser setUseCustomResources(boolean useCustomResources) {
        this.useCustomResources = useCustomResources;
        return this;
    }

    public App parse(List<File> sources, List<String> compilerOptions) {

        if (ONLY_NEW_PARSE_METHOD) {
            return parseNewMethod(sources, compilerOptions);
        }

        // Collect implementation files
        Map<String, File> allFiles = SpecsIo.getFileMap(sources, SourceType.getPermittedExtensions());

        List<String> implementationFiles = allFiles.keySet().stream()
                // TEST: HEADER + IMPL
                .filter(SourceType.IMPLEMENTATION::hasExtension)
                .collect(Collectors.toList());

        // Parse files
        // ClangRootNode ast = new ClangAstParser(showClangDump, useCustomResources, disableNewParsingMethod).parse(
        ClangRootNode ast = new ClangAstParser(showClangDump, useCustomResources).parse(
                implementationFiles,
                compilerOptions);

        if (showClangDump) {
            SpecsLogs.msgInfo("Clang Dump:\n" + SpecsIo.read(new File(ClangAstParser.getClangDumpFilename())));
        }

        if (showClangAst) {
            SpecsLogs.msgInfo("Clang AST:\n" + ast);
        }

        // Parse dump information
        try (ClavaParser clavaParser = new ClavaParser(ast)) {
            App clavaAst = clavaParser.parse();
            clavaAst.setSources(allFiles);
            clavaAst.addConfig(ast.getConfig());

            if (showClavaAst) {
                SpecsLogs.msgInfo("CLAVA AST:\n" + clavaAst.toTree());
            }

            if (showCode) {
                SpecsLogs.msgInfo("Code:\n" + clavaAst.getCode());
            }

            return clavaAst;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    private App parseNewMethod(List<File> sources, List<String> compilerOptions) {

        // Collect implementation files
        Map<String, File> allFiles = SpecsIo.getFileMap(sources, SourceType.getPermittedExtensions());

        List<String> implementationFiles = allFiles.keySet().stream()
                .filter(SourceType.IMPLEMENTATION::hasExtension)
                .collect(Collectors.toList());

        // Parse files
        App app = new ClangDumperParser(showClangDump, useCustomResources).parse(implementationFiles,
                compilerOptions);

        if (showClangDump) {
            SpecsLogs.msgInfo("Clang Dump not supported in new parse method");
        }

        if (showClangAst) {
            SpecsLogs.msgInfo("Clang AST not supported in new parse method");
        }

        if (showClavaAst) {
            SpecsLogs.msgInfo("CLAVA AST:\n" + app.toTree());
        }

        if (showCode) {
            SpecsLogs.msgInfo("Code:\n" + app.getCode());
        }

        return app;

    }
}
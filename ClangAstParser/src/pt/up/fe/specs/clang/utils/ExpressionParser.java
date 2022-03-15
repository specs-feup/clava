/**
 * Copyright 2022 SPeCS.
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

package pt.up.fe.specs.clang.utils;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import pt.up.fe.specs.clang.codeparser.ParallelCodeParser;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;

public class ExpressionParser {

    private static final String TEMPLATE_CODE = "void foo() {auto a = <EXPR_CODE>; }";

    private final List<String> includes;
    private final List<String> flags;

    public ExpressionParser(List<String> includes, List<String> flags) {
        this.includes = includes;
        this.flags = flags;
    }

    /**
     * By default includes <cmath>
     */
    public ExpressionParser() {
        this(Arrays.asList("<cmath>"), Collections.emptyList());
    }

    private String generateCode(String expressionCode) {
        var code = new StringBuilder();

        // Add includes
        includes.forEach(include -> code.append("#include ").append(include).append("\n"));

        // Add code with expression
        code.append(TEMPLATE_CODE.replace("<EXPR_CODE>", expressionCode));

        return code.toString();
    }

    public Optional<Expr> parseTry(String expressionCode) {
        var code = generateCode(expressionCode);

        // Create temporary file
        var tempFolder = SpecsIo.getTempFolder("clava_expression_parser");
        var tempFile = new File(tempFolder, "expression.cpp");
        SpecsIo.write(tempFile, code);

        var cppParser = ParallelCodeParser.newInstance();
        var allFlags = SpecsCollections.concat("-std=c++11", flags);

        App app = null;
        try {
            app = cppParser.parse(Arrays.asList(tempFile), allFlags);
        } catch (Exception e) {
            SpecsLogs.info("Could not parse expression '" + expressionCode + "': " + e.getMessage());
            return Optional.empty();
        }
        // Extract expression
        // Get VarDecl
        var varDecls = app.getDescendants(VarDecl.class);

        if (varDecls.isEmpty()) {
            SpecsLogs.debug(() -> "Could not find a VarDecl with expression " + expressionCode);
            return Optional.empty();
        }

        var varDecl = varDecls.get(0);

        return varDecl.getInit();

        // System.out.println("App: " + app.toTree());
        // var expression = app.getDescendants(DeclStmt.class).get(0).getVarDecls().get(0).getInit();

        // return expression;
    }

    public Expr parse(String expressionCode) {
        return parseTry(expressionCode).orElseThrow(
                () -> new RuntimeException("Could not parse code as C++ expression: '" + expressionCode + "'"));
    }
}

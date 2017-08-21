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

package pt.up.fe.specs.clang.clavaparser.utils;

import java.util.List;

import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.decl.data.FunctionDeclData;
import pt.up.fe.specs.clava.ast.stmt.Stmt;

public class FunctionDeclParserResult {

    private final FunctionDeclData functionDeclData;
    private final List<ParmVarDecl> parameters;
    private final Stmt definition;

    public FunctionDeclParserResult(FunctionDeclData functionDeclData, List<ParmVarDecl> parameters,
            Stmt definition) {

        this.functionDeclData = functionDeclData;
        this.parameters = parameters;
        this.definition = definition;
    }

    public FunctionDeclData getFunctionDeclData() {
        return functionDeclData;
    }

    public List<ParmVarDecl> getParameters() {
        return parameters;
    }

    public Stmt getDefinition() {
        return definition;
    }

}

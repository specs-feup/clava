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

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.data.CXXMethodDeclData;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.decl.data.FunctionDeclData;
import pt.up.fe.specs.clava.ast.extra.CXXCtorInitializer;
import pt.up.fe.specs.clava.ast.stmt.CXXTryStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.SpecsCollections;

/**
 * Represents a C++ constructor declaration or definition.
 * 
 * <p>
 * Structure of children:<br>
 * - Parameters (ParmVarDecl)<br>
 * - Initializers (CXXCtorInitializer)<br>
 * - Definition (Stmt)
 * 
 * @author JoaoBispo
 *
 */
public class CXXConstructorDecl extends CXXMethodDecl {

    // public CXXConstructorDecl(CXXMethodDeclData methodData, List<CXXCtorInitializer> defaultInits, String declName,
    // List<ParmVarDecl> inputs, Type functionType, FunctionDeclData functionDeclData, DeclData declData,
    // ClavaNodeInfo info) {
    //
    // this(methodData, declName, functionType, functionDeclData, declData, info, inputs, defaultInits,
    // Collections.emptyList());
    // }

    public CXXConstructorDecl(CXXMethodDeclData methodData, List<CXXCtorInitializer> defaultInits, String declName,
            List<ParmVarDecl> inputs, Type functionType, FunctionDeclData functionDeclData, DeclData declData,
            ClavaNodeInfo info, Stmt definition) {

        this(methodData, declName, functionType, functionDeclData, declData, info, inputs, defaultInits, definition);
    }

    public CXXConstructorDecl(CXXMethodDeclData methodData, String declName,
            Type functionType, FunctionDeclData functionDeclData, DeclData declData, ClavaNodeInfo info,
            List<ParmVarDecl> inputs, List<CXXCtorInitializer> defaultInits, Stmt definition) {

        this(methodData, declName, functionType, functionDeclData, declData, info,
                SpecsCollections.concat(SpecsCollections.concat(inputs, defaultInits), definition));

        checkDefinition(definition);

    }

    private CXXConstructorDecl(CXXMethodDeclData methodData, String declName,
            Type functionType, FunctionDeclData functionDeclData, DeclData declData, ClavaNodeInfo info,
            List<? extends ClavaNode> children) {

        super(methodData, declName, functionType, functionDeclData, declData, info, children);
    }

    @Override
    protected ClavaNode copyPrivate() {

        return new CXXConstructorDecl(getMethodData(), getDeclName(), getFunctionType(), getFunctionDeclData(),
                getDeclData(), getInfo(), Collections.emptyList());
    }

    public List<CXXCtorInitializer> getInitializers() {
        // Default inits appear after parameters and definition
        int startIndex = getNumParameters();

        List<ClavaNode> initAtHead = SpecsCollections.subList(getChildren(), startIndex);

        List<CXXCtorInitializer> inits = SpecsCollections.peek(initAtHead, CXXCtorInitializer.class);

        return inits;
    }

    @Override
    public String getCode() {
        if (getDeclData().isImplicit()) {
            return "";
        }

        // Special case: try
        Optional<Stmt> body = getFunctionDefinition();
        if (body.isPresent() && body.get() instanceof CXXTryStmt) {
            CXXTryStmt tryBody = (CXXTryStmt) body.get();
            return getDeclarationId(false) + " " + tryBody.getCode(getCodeInitList());
        }

        return getDeclarationId(false) + getCodeInitList() + getCodeBody();
    }

    private String getCodeInitList() {

        List<String> initList = getInitializers().stream()
                .map(init -> init.getCode())
                .filter(initCode -> !initCode.isEmpty())
                .collect(Collectors.toList());

        if (initList.isEmpty()) {
            return "";
        }

        return initList.stream()
                .collect(Collectors.joining(", ", " : ", ""));
    }

}

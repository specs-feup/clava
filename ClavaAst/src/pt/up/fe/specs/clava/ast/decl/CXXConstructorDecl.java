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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.data.ctorinit.CXXCtorInitializer;
import pt.up.fe.specs.clava.ast.stmt.CXXTryStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;

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

    /// DATAKEYS BEGIN

    public final static DataKey<List<CXXCtorInitializer>> CONSTRUCTOR_INITS = KeyFactory.generic("constructorInits",
            (List<CXXCtorInitializer>) new ArrayList<CXXCtorInitializer>());

    /**
     * True if this constructor is a default constructor.
     */
    public final static DataKey<Boolean> IS_DEFAULT_CONSTRUCTOR = KeyFactory.bool("isDefaultConstructor");

    /**
     * True if this constructor was marked "explicit".
     */
    public final static DataKey<Boolean> IS_EXPLICIT = KeyFactory.bool("isExplicit");

    /**
     * True if this constructor declaration has 'explicit' keyword.
     */
    public final static DataKey<Boolean> IS_EXPLICIT_SPECIFIED = KeyFactory.bool("isExplicitSpecified");

    /// DATAKEYS END

    public CXXConstructorDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    // public CXXConstructorDecl(CXXMethodDeclData methodData, List<CXXCtorInitializer> defaultInits, String declName,
    // List<ParmVarDecl> inputs, Type functionType, FunctionDeclData functionDeclData, DeclData declData,
    // ClavaNodeInfo info) {
    //
    // this(methodData, declName, functionType, functionDeclData, declData, info, inputs, defaultInits,
    // Collections.emptyList());
    // }

    /*
    public CXXConstructorDecl(List<CXXCtorInitializer> defaultInits, String declName,
            List<ParmVarDecl> inputs, Type functionType, FunctionDeclData functionDeclData, DeclData declData,
            ClavaNodeInfo info, Stmt definition) {
    
        this(declName, functionType, functionDeclData, declData, info, inputs, defaultInits, definition);
    }
    
    public CXXConstructorDecl(String declName,
            Type functionType, FunctionDeclData functionDeclData, DeclData declData, ClavaNodeInfo info,
            List<ParmVarDecl> inputs, List<CXXCtorInitializer> defaultInits, Stmt definition) {
    
        this(declName, functionType, functionDeclData, declData, info,
                SpecsCollections.concat(SpecsCollections.concat(inputs, defaultInits), definition));
    
        checkDefinition(definition);
    
    }
    
    // private CXXConstructorDecl(CXXMethodDeclData methodData, String declName,
    private CXXConstructorDecl(String declName,
            Type functionType, FunctionDeclData functionDeclData, DeclData declData, ClavaNodeInfo info,
            List<? extends ClavaNode> children) {
    
        super(declName, functionType, functionDeclData, declData, info, children);
    }
    
    @Override
    protected ClavaNode copyPrivate() {
    
        return new CXXConstructorDecl(getDeclName(), getFunctionType(), getFunctionDeclData(),
                getDeclData(), getInfo(), Collections.emptyList());
    }
    */

    public List<CXXCtorInitializer> getInitializers() {
        return get(CONSTRUCTOR_INITS);
        // // Default inits appear after parameters and definition
        // int startIndex = getNumParameters();
        //
        // List<ClavaNode> initAtHead = SpecsCollections.subList(getChildren(), startIndex);
        //
        // List<CXXCtorInitializer> inits = SpecsCollections.peek(initAtHead, CXXCtorInitializer.class);
        //
        // return inits;
    }

    @Override
    public String getCode() {
        if (get(IS_IMPLICIT)) {
            return "";
        }
        // if (getDeclData().isImplicit()) {
        // return "";
        // }

        // Special case: try
        Optional<Stmt> body = getFunctionDefinition();
        if (body.isPresent() && body.get() instanceof CXXTryStmt) {
            CXXTryStmt tryBody = (CXXTryStmt) body.get();
            return getDeclarationId(false) + " " + tryBody.getCode(getCodeInitList());
        }

        return getDeclarationId(false) + getCodeInitList() + getCodeBody();
    }

    @Override
    public String getCodeInitList() {

        // Check if super is not empty
        String superCode = super.getCodeInitList();
        if (!superCode.isEmpty()) {
            return superCode;
        }

        List<String> initList = getInitializers().stream()
                // Do not take into account default initializers
                // .filter(init -> !(init.get(CXXCtorInitializer.INIT_EXPR) instanceof CXXDefaultInitExpr))
                // Do not take into account default initializers that are not written in the source code
                .filter(init -> init.get(CXXCtorInitializer.IS_WRITTEN))
                .map(init -> init.getCode(this))
                .filter(initCode -> !initCode.isEmpty())
                .collect(Collectors.toList());

        if (initList.isEmpty()) {
            return "";
        }

        return initList.stream()
                .collect(Collectors.joining(", ", " : ", ""));
    }

}

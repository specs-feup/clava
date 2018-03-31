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

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ast.decl.data.CXXMethodDeclData;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.decl.data.FunctionDeclData;
import pt.up.fe.specs.clava.ast.decl.enums.StorageClass;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.type.FunctionProtoType;
import pt.up.fe.specs.clava.ast.type.Type;

/**
 * Represents a C++ class method declaration or definition.
 * 
 * <p>
 * Structure of children:<br>
 * - Parameters (ParmVarDecl)<br>
 * - Definition (Stmt)
 * 
 * @author JoaoBispo
 *
 */
public class CXXMethodDecl extends FunctionDecl {

    private static final long NULL_EXCEPT_ADDRESS = 0;

    public static long getNullExceptAddress() {
        return NULL_EXCEPT_ADDRESS;
    }

    private final CXXMethodDeclData methodData;

    /**
     * 
     * 
     * @param declName
     * @param returnType
     * @param attributes
     * @param hasDefinition
     * @param info
     * @param children
     */
    public CXXMethodDecl(CXXMethodDeclData methodData, String declName, Type functionType,
            FunctionDeclData functionDeclData, DeclData declData, ClavaNodeInfo info, List<ParmVarDecl> inputs,
            Stmt definition) {

        super(declName, functionType, functionDeclData, declData, info, inputs, definition);

        this.methodData = methodData;
    }

    /**
     * Constructor for copy() function and derived classes.
     * 
     * @param methodData
     * @param declName
     * @param functionType
     * @param functionDeclData
     * @param declData
     * @param info
     * @param children
     */
    protected CXXMethodDecl(CXXMethodDeclData methodData, String declName, Type functionType,
            FunctionDeclData functionDeclData, DeclData declData, ClavaNodeInfo info,
            List<? extends ClavaNode> children) {

        super(declName, functionType, functionDeclData, declData, info, children);

        this.methodData = methodData;
    }

    @Override
    protected ClavaNode copyPrivate() {

        return new CXXMethodDecl(methodData, getDeclName(), getFunctionType(), getFunctionDeclData(), getDeclData(),
                getInfo(), Collections.emptyList());
    }

    protected boolean isInsideRecordDecl() {
        return getAncestorTry(CXXRecordDecl.class).isPresent();
    }

    protected boolean isInsideNamespaceDecl() {
        return getAncestorTry(NamespaceDecl.class).isPresent();
    }

    public CXXMethodDeclData getMethodData() {
        return methodData;
    }

    @Override
    public String getCode() {
        // System.out.println("CXXRECORD PARAMETER TYPES:" + getFunctionType().getParamTypes());
        return getCode(true);
    }

    public String getCode(boolean useReturnType) {
        if (getDeclData().isImplicit()) {
            return "";
        }

        return getDeclarationId(useReturnType) + getCodeBody();
    }

    @Override
    public String getDeclarationId(boolean useReturnType) {
        StringBuilder code = new StringBuilder();

        // if (getFunctionDeclData().getStorageClass() == StorageClass.STATIC) {
        if (getFunctionDeclData().getStorageClass() != StorageClass.NONE) {
            code.append(getFunctionDeclData().getStorageClass().getString()).append(" ");
        }

        if (getFunctionDeclData().isInline()) {
            code.append("inline ");
        }

        if (getFunctionDeclData().isVirtual()) {
            code.append("virtual ");
            // throwNoCodeGeneration();
        }

        if (getFunctionDeclData().isModulePrivate()) {
            // code.append("__module_private__ ");
            throwNoCodeGeneration();
        }

        if (useReturnType) {
            // TODO: When mixing templates and lambdas, sometimes types are not available. Find better solution?
            getFunctionTypeTry().ifPresent(fType -> code.append(fType.getReturnType().getCode()).append(" "));
            // code.append(getFunctionType().getReturnType().getCode()).append(" ");
        }

        // Add namespace if not inside namespace decl
        if (!isInsideNamespaceDecl()) {
            String namespace = getMethodData().getNamespace();
            namespace = namespace == null ? "" : namespace + "::";

            code.append(namespace);
        }

        // Add record if not inside record decl
        if (!isInsideRecordDecl()) {
            code.append(getMethodData().getRecord()).append("::");
        }

        code.append(getTypelessCode());

        // TODO: Pass above code to typelessCode
        // Add except
        // code.append(getCodeExcept());

        return code.toString();
    }

    public CXXRecordDecl getRecordDecl() {
        // Check if this node is inside the record
        Optional<CXXRecordDecl> ancestor = getAncestorTry(CXXRecordDecl.class);

        return ancestor.orElse(
                getApp().getCXXRecordDecl(getMethodData().getNamespace(), getMethodData().getRecord()));

    }

    @Override
    public FunctionProtoType getFunctionType() {
        return (FunctionProtoType) super.getFunctionType();
    }

    public Optional<FunctionProtoType> getFunctionTypeTry() {
        return Optional.ofNullable((FunctionProtoType) super.getFunctionType());
    }

    @Override
    public String toContentString() {
        return super.toContentString() + "implicit:" + getDeclData().isImplicit();
    }

}

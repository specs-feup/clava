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

import java.util.Collection;
import java.util.Optional;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.enums.StorageClass;
import pt.up.fe.specs.clava.ast.type.FunctionProtoType;

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

    /// DATAKEYS BEGIN

    // TODO: Change to CXXRecordDecl
    public final static DataKey<Decl> RECORD = KeyFactory.object("record", Decl.class);

    public final static DataKey<String> RECORD_ID = KeyFactory.string("recordId");

    /// DATAKEYS END

    // private static final long NULL_EXCEPT_ADDRESS = 0;
    //
    // public static long getNullExceptAddress() {
    // return NULL_EXCEPT_ADDRESS;
    // }

    // private final CXXMethodDeclData methodData;

    public CXXMethodDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

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
    /*
    public CXXMethodDecl(String declName, Type functionType,
            FunctionDeclData functionDeclData, DeclData declData, ClavaNodeInfo info, List<ParmVarDecl> inputs,
            Stmt definition) {
    
        this(declName, functionType, functionDeclData, declData, info, SpecsCollections.concat(inputs, definition));
    
        // this.methodData = methodData;
    }
    */

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
    /*
    protected CXXMethodDecl(String declName, Type functionType,
            FunctionDeclData functionDeclData, DeclData declData, ClavaNodeInfo info,
            List<? extends ClavaNode> children) {
    
        super(declName, functionType, functionDeclData, declData, info, children);
    
        // this.methodData = methodData;
    }
    */
    /*
    @Override
    protected ClavaNode copyPrivate() {
    
        // return new CXXMethodDecl(methodData, getDeclName(), getFunctionType(), getFunctionDeclData(), getDeclData(),
        return new CXXMethodDecl(getDeclName(), getFunctionType(), getFunctionDeclData(), getDeclData(),
                getInfo(), Collections.emptyList());
    }
    */
    protected boolean isInsideRecordDecl() {
        return getAncestorTry(CXXRecordDecl.class).isPresent();
    }

    protected boolean isInsideNamespaceDecl() {
        return getAncestorTry(NamespaceDecl.class).isPresent();
    }

    protected boolean addNamespace() {

        // Do not add if inside namespace
        if (isInsideNamespaceDecl()) {
            return false;
        }

        // Do not add if parent is a FunctionTemplateDecl
        if (getAncestorTry(FunctionTemplateDecl.class).isPresent()) {
            return false;
        }

        // Do not add if inside record decl
        if (isInsideRecordDecl()) {
            return false;
        }

        return true;

    }

    // public CXXMethodDeclData getMethodData() {
    // return methodData;
    // }

    @Override
    public String getCode() {
        // System.out.println("CXXRECORD PARAMETER TYPES:" + getFunctionType().getParamTypes());
        return getCode(true);
    }

    public String getCode(boolean useReturnType) {
        if (get(IS_IMPLICIT)) {
            // if (getDeclData().isImplicit()) {
            return "";
        }

        // System.out.println("CXXMETHOD DECL CODE:" + getDeclarationId(useReturnType) + getCodeBody());
        // System.out.println("TREE:" + toTree());
        return getDeclarationId(useReturnType) + getCodeInitList() + getCodeBody();
    }

    public String getCodeInitList() {

        if (get(IS_EXPLICITLY_DEFAULTED)) {
            return " = default";
        }

        return "";
    }

    @Override
    public String getDeclarationId(boolean useReturnType) {
        StringBuilder code = new StringBuilder();

        // if (getFunctionDeclData().getStorageClass() == StorageClass.STATIC) {
        if (get(STORAGE_CLASS) != StorageClass.NONE) {
            code.append(get(STORAGE_CLASS).getString()).append(" ");
        }

        if (get(IS_INLINE)) {
            code.append("inline ");
        }

        if (get(IS_VIRTUAL)) {
            code.append("virtual ");
            // throwNoCodeGeneration();
        }

        if (get(IS_MODULE_PRIVATE)) {
            // code.append("__module_private__ ");
            throwNoCodeGeneration();
        }

        if (useReturnType) {
            // TODO: When mixing templates and lambdas, sometimes types are not available. Find better solution?
            getFunctionTypeTry().ifPresent(fType -> code.append(fType.getReturnType().getCode(this)).append(" "));
            // code.append(getFunctionType().getReturnType().getCode()).append(" ");
        }

        // Add namespace if not inside namespace decl and not inside RecordDecl
        // if (!isInsideNamespaceDecl()) {

        if (addNamespace()) {
            String namespace = getNamespace(getRecordName()).map(str -> str + "::").orElse("");
            // String namespace = getMethodData().getNamespace();
            // namespace = namespace == null ? "" : namespace + "::";

            code.append(namespace);
        }

        // Add record if not inside record decl

        if (!isInsideRecordDecl()) {
            // code.append(getMethodData().getRecord()).append("::");
            code.append(getRecordName()).append("::");
        }

        code.append(getTypelessCode());

        // TODO: Pass above code to typelessCode
        // Add except
        // code.append(getCodeExcept());

        return code.toString();
    }

    /**
     * 
     * @return the class of this method, or null if no class is associated
     */
    public Optional<CXXRecordDecl> getRecordDecl() {

        var record = get(RECORD);
        if (record instanceof NullDecl) {
            return Optional.empty();
        }

        return Optional.of((CXXRecordDecl) record);
        // return (CXXRecordDecl) getApp().getNode(get(RECORD_ID));
        // // Check if this node is inside the record
        // Optional<CXXRecordDecl> ancestor = getAncestorTry(CXXRecordDecl.class);
        //
        // return ancestor.orElse(
        // // getApp().getCXXRecordDecl(getMethodData().getNamespace(), getMethodData().getRecord()));
        // getApp().getCXXRecordDecl(getNamespace().orElse(null), getRecordName()));

    }

    @Override
    public FunctionProtoType getFunctionType() {
        // System.out.println("CXX METHOD DECL:" + getId());
        // System.out.println("CXX METHOD DECL FUNCTION TYPE:" + super.getFunctionType().getId());
        // System.out.println("CXX METHOD DECL FUNCTION TYPE IS CONST:" + super.getFunctionType().isConst());
        return (FunctionProtoType) super.getFunctionType();
    }

    public Optional<FunctionProtoType> getFunctionTypeTry() {
        return Optional.ofNullable((FunctionProtoType) super.getFunctionType());
    }

    // @Override
    // public String toContentString() {
    // return super.toContentString() + "implicit:" + getDeclData().isImplicit();
    // }

    @Override
    public String getSignature() {
        String baseSignature = super.getSignature();

        String namespace = getNamespace(getRecordName()).map(str -> str + "::").orElse("");
        // String namespace = getMethodData().getNamespace();
        // namespace = namespace == null ? "" : namespace + "::";

        var recordName = getRecordDecl().map(record -> record.getDeclName() + "::").orElse("");
        namespace = namespace + recordName;
        // namespace = namespace + getRecordDecl().getDeclName() + "::";

        String signature = namespace + baseSignature;
        // System.out.println("CXX METHOD DECL:" + this);
        // System.out.println("RECORD TYPE:" + getRecordDecl().getType());
        // System.out.println("METHOD TYPE:" + getType().getId());
        // System.out.println("FUNCTION TYPE:" + getFunctionType().getId());
        // System.out.println("IS CONST:" + getFunctionType().isConst());
        // Check qualifiers
        if (getFunctionType().isConst()) {
            signature = signature + " const";
        }

        return signature;
    }

    // public CXXRecordDecl getRecord() {
    // return (CXXRecordDecl) get(RECORD);
    // }
    /*
    public Optional<String> getNamespace() {
        // Qualified name has full name
        String qualifiedName = get(QUALIFIED_NAME);
    
        if (qualifiedName.isEmpty()) {
            return Optional.empty();
        }
    
        // Remove decl name
        String declName = "::" + get(DECL_NAME);
        SpecsCheck.checkArgument(qualifiedName.endsWith(declName),
                () -> "Expected qualified name '" + qualifiedName + "' to end with '" + declName + "'");
    
        String currentString = qualifiedName.substring(0, qualifiedName.length() - declName.length());
    
        // TODO: Replace with RECORD, after CXXRecordDecl is implemented
        // CXXRecordDecl record = getRecordDecl();
        // String recordName = record.getDeclName();
        String recordName = getRecordName();
        SpecsCheck.checkArgument(currentString.endsWith(recordName),
                () -> "Expected current string '" + currentString + "' to end with '" + recordName + "'");
    
        // Remove record name
        String namespace = currentString.substring(0, currentString.length() - recordName.length());
    
        // Remove ::, if present
        if (namespace.endsWith("::")) {
            namespace = namespace.substring(0, namespace.length() - "::".length());
        }
    
        return !namespace.isEmpty() ? Optional.of(namespace) : Optional.empty();
    
    }
    */
    public String getRecordName() {
        return getRecordDecl().map(record -> record.getDeclName()).orElse(null);
    }

    /*
    @Override
    public String getFunctionId() {
        // return getDeclarationId(false);
        StringBuilder id = new StringBuilder();
    
        var ftype = getFunctionType();
        // Check if function is const
        if (ftype.isConst()) {
            id.append("const ");
        }
    
        if (addNamespace()) {
            String namespace = getNamespace(getRecordName()).map(str -> str + "::").orElse("");
            id.append(namespace);
        }
    
        // Add record if not inside record decl
    
        if (!isInsideRecordDecl()) {
            // code.append(getMethodData().getRecord()).append("::");
            id.append(getRecordName()).append("::");
        }
    
        id.append(getFunctionType().getCode(getDeclName()));
    
        return id.toString();
    }
    */

    @Override
    protected FunctionDecl copyFunction(String newName) {
        var copy = super.copyFunction(newName);

        // If new name is the same as the current name, remove record from copy
        if (getDeclName().equals(newName)) {
            removeRecord();
        }

        return copy;
    }

    public void removeRecord() {
        set(CXXMethodDecl.RECORD, getFactory().nullDecl());
        set(CXXMethodDecl.RECORD_ID, "null");
    }
}

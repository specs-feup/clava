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
import java.util.List;
import java.util.Optional;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.enums.StorageClass;
import pt.up.fe.specs.clava.ast.type.FunctionProtoType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.util.SpecsCheck;

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

    // TODO: Change to Optional<CXXRecordDecl>, since it can be null
    public final static DataKey<Decl> RECORD = KeyFactory.object("record", Decl.class);

    public final static DataKey<String> RECORD_ID = KeyFactory.string("recordId");

    public final static DataKey<List<CXXMethodDecl>> OVERRIDDEN_METHODS = KeyFactory.list("overriddenMethods",
            CXXMethodDecl.class);

    public final static DataKey<Boolean> IS_STATIC = KeyFactory.bool("isStatic");

    public final static DataKey<Boolean> IS_INSTANCE = KeyFactory.bool("isInstance");

    public final static DataKey<Boolean> IS_CONST = KeyFactory.bool("isConst");

    public final static DataKey<Boolean> IS_VOLATILE = KeyFactory.bool("isVolatile");

    public final static DataKey<Boolean> IS_VIRTUAL = KeyFactory.bool("isVirtual");

    /**
     * True if this is a copy-assignment operator, either declared implicitly or explicitly.
     */
    public final static DataKey<Boolean> IS_COPY_ASSIGNMENT_OPERATOR = KeyFactory.bool("isCopyAssignmentOperator");

    /**
     * 
     */
    public final static DataKey<Boolean> IS_MOVE_ASSIGNMENT_OPERATOR = KeyFactory.bool("isMoveAssignmentOperator");

    /**
     * Return the type of the 'this' pointer. Note that for the call operator of a lambda closure type, this returns the
     * desugared 'this' type (a pointer to the closure type), not the captured 'this' type.
     */
    public final static DataKey<Optional<Type>> THIS_TYPE = KeyFactory.optional("thisType");

    /**
     * The type of the object pointed by 'this'.
     */
    public final static DataKey<Optional<Type>> THIS_OJBECT_TYPE = KeyFactory.optional("thisObjectType");

    public final static DataKey<Boolean> HAS_INLINE_BODY = KeyFactory.bool("hasInlineBody");

    /**
     * True if this is a lambda closure type's static member function that is used for the result of the lambda's
     * conversion to function pointer (for a lambda with no captures).
     */
    public final static DataKey<Boolean> IS_LAMBDA_STATIC_INVOKER = KeyFactory.bool("isLambdaStaticInvoker");

    /// DATAKEYS END

    public CXXMethodDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    protected boolean isInsideRecordDecl() {
        return getAncestorTry(CXXRecordDecl.class).isPresent();
    }

    protected boolean isInsideNamespaceDecl() {
        return getAncestorTry(NamespaceDecl.class).isPresent();
    }

    protected boolean addNamespace() {

        // Do not add if inside record decl
        if (isInsideRecordDecl()) {
            return false;
        }

        return true;

    }

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

        // System.out
        // .println("CXXMETHOD DECL CODE:" + getDeclarationId(useReturnType));
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
        if (get(STORAGE_CLASS) != StorageClass.None) {
            code.append(get(STORAGE_CLASS).getString()).append(" ");
        }

        if (get(IS_INLINE_SPECIFIED)) {
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
        if (addNamespace()) {
            // String namespace = getCurrentNamespace(getRecordName()).map(str -> str + "::").orElse("");
            String namespace = getCurrentNamespace().map(str -> str + "::").orElse("");

            code.append(namespace);
        }

        code.append(getTypelessCode());

        // TODO: Pass above code to typelessCode
        // Add except
        // code.append(getCodeExcept());

        // Make method declaration pure
        if (get(IS_PURE) && get(IS_VIRTUAL_AS_WRITTEN)) {
            code.append(" = 0");
        }

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

    @Override
    public String getSignature() {
        String baseSignature = super.getSignature();

        String namespace = getNamespace().map(str -> str + "::").orElse("");
        // String namespace = getNamespace(getRecordName()).map(str -> str + "::").orElse("");

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

    public String getRecordName() {
        return getRecordDecl().map(record -> record.getDeclName()).orElse(null);
    }

    @Override
    protected FunctionDecl copyFunction(String newName) {
        var copy = (CXXMethodDecl) super.copyFunction(newName);

        // If new name is the same as the current name, remove record from copy
        if (getDeclName().equals(newName)) {
            copy.removeRecord();
        }

        return copy;
    }

    public void removeRecord() {
        var currentRecordDecl = get(RECORD);

        // Already removed
        if (currentRecordDecl instanceof NullDecl) {
            return;
        }

        // var currentRecordName = ((NamedDecl) currentRecordDecl).getDeclName();
        // var currentQualifiedPrefix = get(QUALIFIED_PREFIX);
        //
        // SpecsCheck.checkArgument(currentQualifiedPrefix.endsWith(currentRecordName),
        // () -> "Expected current qualified prefix (" + currentQualifiedPrefix
        // + ") to end with the name of the current record (" + currentRecordName + ")");
        //
        // var endIndex = currentQualifiedPrefix.length() - currentRecordName.length();
        // var newQualifiedPrefix = currentQualifiedPrefix.substring(0, endIndex);

        var newQualifiedPrefix = getQualifiedPrefixWithoutRecord();

        set(RECORD, getFactory().nullDecl());
        set(RECORD_ID, "null");
        set(QUALIFIED_PREFIX, newQualifiedPrefix);
        // Removed record from qualified prefix
    }

    public String getQualifiedPrefixWithoutRecord() {
        var currentQualifiedPrefix = get(QUALIFIED_PREFIX);
        var currentRecordDecl = get(RECORD);

        var currentRecordName = currentRecordDecl instanceof NullDecl ? ""
                : ((NamedDecl) currentRecordDecl).getDeclName();

        SpecsCheck.checkArgument(currentQualifiedPrefix.endsWith(currentRecordName),
                () -> "Expected current qualified prefix (" + currentQualifiedPrefix
                        + ") to end with the name of the current record (" + currentRecordName + ")");

        var endIndex = currentQualifiedPrefix.length() - currentRecordName.length();
        return currentQualifiedPrefix.substring(0, endIndex);
    }

    public void setRecord(CXXRecordDecl cxxRecordDecl) {

        // Update qualified prefix
        var newQualifiedPrefix = getQualifiedPrefixWithoutRecord() + cxxRecordDecl.getDeclName();

        // System.out.println("CURRENT RECORD DECL: " + currentRecordDecl.getDeclName());
        // System.out.println("NEW RECORD DECL: " + cxxRecordDecl.getDeclName());
        // System.out.println("CURRENT QUAL NAME: " + currentQualifiedPrefix);
        // System.out.println("NEW QUAL NAME: " + newQualifiedPrefix);

        set(QUALIFIED_PREFIX, newQualifiedPrefix);
        set(RECORD, cxxRecordDecl);
        set(RECORD_ID, cxxRecordDecl.getId());

    }
}

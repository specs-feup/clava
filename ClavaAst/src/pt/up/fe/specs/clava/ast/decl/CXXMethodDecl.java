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
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.ast.decl;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
public class CXXMethodDecl extends CXXMethodDeclBase {

    // Data declarations are generated from the wire schema in CXXMethodDeclBase.

    public CXXMethodDecl(DataStore data, Collection<? extends ClavaNode> children, CXXMethodDeclBase.Values values) {
        this(data, children);
        setWireValues(values);
    }

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

        if (getWireIsExplicitlyDefaulted()) {
            return " = default";
        }

        return "";
    }

    @Override
    public String getDeclarationId(boolean useReturnType) {
        StringBuilder code = new StringBuilder();
        code.append(getTemplateHeadersCode());

        // if (getFunctionDeclData().getStorageClass() == StorageClass.STATIC) {
        if (getWireStorageClass() != StorageClass.None) {
            code.append(getWireStorageClass().getString()).append(" ");
        }

        if (getWireIsInlineSpecified()) {
            code.append("inline ");
        }

        if (getWireIsVirtual()) {
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
            String namespace = getCurrentQualifiedPrefix().map(str -> str + "::").orElse("");
            if (getRecordDecl().orElse(null) instanceof ClassTemplateSpecializationDecl specialization) {
                String arguments = specialization.get(ClassTemplateSpecializationDecl.TEMPLATE_ARGUMENTS).stream()
                        .map(argument -> argument.getCode(this)).collect(java.util.stream.Collectors.joining(", "));
                String prefix = specialization.get(QUALIFIED_PREFIX);
                namespace = (prefix.isEmpty() ? "" : prefix + "::")
                        + specialization.getDeclName() + "<" + arguments + ">::";
            }

            var record = getRecordDecl().orElse(null);
            if (record != null && record.getParent() instanceof ClassTemplateDecl template
                    && template.getTemplateDecl() == record && !getWireTemplateParameterListSizes().isEmpty()) {
                namespace = getPrimaryTemplateQualifier(record);
            }
            code.append(namespace);
        }

        code.append(getTypelessCode());

        // TODO: Pass above code to typelessCode
        // Add except
        // code.append(getCodeExcept());

        // Make method declaration pure
        if (getWireIsPure() && getWireIsVirtualAsWritten()) {
            code.append(" = 0");
        }

        return code.toString();
    }

    private String getPrimaryTemplateQualifier(CXXRecordDecl record) {
        List<CXXRecordDecl> scopes = new java.util.ArrayList<>();
        for (CXXRecordDecl scope = record; scope != null;
                scope = scope.getAncestorTry(CXXRecordDecl.class).orElse(null)) {
            scopes.add(scope);
        }
        java.util.Collections.reverse(scopes);
        String prefix = scopes.get(0).get(QUALIFIED_PREFIX);
        StringBuilder qualifier = new StringBuilder(prefix.isEmpty() ? "" : prefix + "::");
        int group = 0;
        int offset = 0;
        for (CXXRecordDecl scope : scopes) {
            qualifier.append(scope.getDeclName());
            if (scope.getParent() instanceof ClassTemplateDecl template && template.getTemplateDecl() == scope) {
                if (group >= getWireTemplateParameterListSizes().size()) {
                    throw new IllegalArgumentException("Missing enclosing class template parameter list");
                }
                int size = getWireTemplateParameterListSizes().get(group++);
                if (size != template.getNumTemplateParameters() || size > getWireTemplateParameters().size() - offset) {
                    throw new IllegalArgumentException("Class template parameters do not match method context");
                }
                String arguments = getWireTemplateParameters().subList(offset, offset + size).stream()
                        .map(CXXMethodDecl::getTemplateArgumentCode).collect(java.util.stream.Collectors.joining(", "));
                qualifier.append("<").append(arguments).append(">");
                offset += size;
            } else if (scope instanceof ClassTemplateSpecializationDecl specialization) {
                String arguments = specialization.get(ClassTemplateSpecializationDecl.TEMPLATE_ARGUMENTS).stream()
                        .map(argument -> argument.getCode(this)).collect(java.util.stream.Collectors.joining(", "));
                qualifier.append("<").append(arguments).append(">");
                if (scope instanceof ClassTemplatePartialSpecializationDecl partial) {
                    if (group >= getWireTemplateParameterListSizes().size()) {
                        throw new IllegalArgumentException("Missing enclosing partial specialization parameter list");
                    }
                    int size = getWireTemplateParameterListSizes().get(group++);
                    if (size != partial.get(ClassTemplatePartialSpecializationDecl.TEMPLATE_PARAMETERS).size()
                            || size > getWireTemplateParameters().size() - offset) {
                        throw new IllegalArgumentException("Partial specialization parameters do not match method context");
                    }
                    offset += size;
                }
            }
            qualifier.append("::");
        }
        return qualifier.toString();
    }

    private static String getTemplateArgumentCode(NamedDecl parameter) {
        boolean packed = parameter instanceof TemplateTypeParmDecl type && type.get(TemplateTypeParmDecl.IS_PARAMETER_PACK)
                || parameter instanceof NonTypeTemplateParmDecl value && value.get(NonTypeTemplateParmDecl.IS_PARAMETER_PACK)
                || parameter instanceof TemplateTemplateParmDecl template && template.get(TemplateTemplateParmDecl.IS_PARAMETER_PACK);
        return parameter.getDeclName() + (packed ? "..." : "");
    }

    /**
     * 
     * @return the class of this method, or null if no class is associated
     */
    public Optional<CXXRecordDecl> getRecordDecl() {

        var record = getWireRecord();
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
        var currentRecordDecl = getWireRecord();

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
        set(QUALIFIED_PREFIX, newQualifiedPrefix);
        // Removed record from qualified prefix
    }

    public String getQualifiedPrefixWithoutRecord() {
        var currentQualifiedPrefix = get(QUALIFIED_PREFIX);
        var currentRecordDecl = getWireRecord();

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

    }
}

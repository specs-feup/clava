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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;
import org.suikasoft.jOptions.Interfaces.DataStore;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.Types;
import pt.up.fe.specs.clava.ast.decl.data.templates.TemplateArgument;
import pt.up.fe.specs.clava.ast.decl.enums.StorageClass;
import pt.up.fe.specs.clava.ast.decl.enums.TemplateKind;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.extra.data.Language;
import pt.up.fe.specs.clava.ast.stmt.CXXTryStmt;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.type.FunctionProtoType;
import pt.up.fe.specs.clava.ast.type.FunctionType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.utils.NodeWithScope;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.exceptions.CaseNotDefinedException;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;

/**
 * Represents a function declaration or definition.
 *
 * <p>
 * Structure of children:<br>
 * - Parameters (ParmVarDecl)<br>
 * - Definition (Stmt)
 *
 * @author JoaoBispo
 *
 */
public class FunctionDecl extends DeclaratorDecl implements NodeWithScope {

    /// DATAKEYS BEGIN

    /**
     * True if this is a (C++11) constexpr function or constexpr constructor.
     */
    public final static DataKey<Boolean> IS_CONSTEXPR = KeyFactory.bool("isConstexpr");

    /**
     * The kind of templated function.
     */
    public final static DataKey<TemplateKind> TEMPLATE_KIND = KeyFactory
            .enumeration("templateKind", TemplateKind.class)
            .setDefault(() -> TemplateKind.NON_TEMPLATE);

    /**
     * The storage class as written in the source.
     */
    public final static DataKey<StorageClass> STORAGE_CLASS = KeyFactory
            .enumeration("storageClass", StorageClass.class)
            .setDefault(() -> StorageClass.None);

    /**
     * True if the "inline" keyword was specified for this function.
     */
    public final static DataKey<Boolean> IS_INLINE_SPECIFIED = KeyFactory.bool("isInline");

    /**
     * True if this function is explicitly marked as virtual.
     */
    public final static DataKey<Boolean> IS_VIRTUAL_AS_WRITTEN = KeyFactory.bool("isVirtualAsWritten");

    /**
     * True if this virtual function is pure, i.e. makes the containing class abstract.
     */
    public final static DataKey<Boolean> IS_PURE = KeyFactory.bool("isPure");

    /**
     * True if this function was deleted (via the C++0x "= delete" syntax).
     */
    public final static DataKey<Boolean> IS_DELETED = KeyFactory.bool("isDeleted");

    /**
     * True if this function is explicitly defaulted (via the C++0x "= default" syntax).
     */
    public final static DataKey<Boolean> IS_EXPLICITLY_DEFAULTED = KeyFactory.bool("isExplicitlyDefaulted");

    // TODO: Change to FunctionDecl when refactoring is complete
    public final static DataKey<Optional<Decl>> PREVIOUS_DECL = KeyFactory.optional("previousDecl");

    // TODO: Change to FunctionDecl when refactoring is complete
    public final static DataKey<Decl> CANONICAL_DECL = KeyFactory.object("canonicalDecl", Decl.class);

    public final static DataKey<Optional<FunctionDecl>> PRIMARY_TEMPLATE_DECL = KeyFactory
            .optional("primaryTemplateDecl");

    /**
     * Template arguments of this function, if any.
     */
    public final static DataKey<List<TemplateArgument>> TEMPLATE_ARGUMENTS = KeyFactory
            .generic("templateArguments", (List<TemplateArgument>) new ArrayList<TemplateArgument>());

    /// DATAKEYS END

    // CHECK: Directly enconding information relative to the tree structure (e.g., how many parameter nodes)
    // prevents direct transformations in the tree (e.g., if we remove a parameter, the value needs to be updated)
    // Solutions:
    // 1) Treat the nodes as immutable -> Does not really solve the problem in this case, the information of the
    // parameters is information about the children, not the node itself
    // 2) Can only add/remove nodes using node-specific functions -> How do we distinguish nodes where we can use
    // generic insert/remove from these special nodes?
    // 3) Create special nodes -> e.g., FunctionDecl would have a Inputs node, a Definition node, etc. More cumbersome
    // tree
    // 4) Make internal logic for returning/adding nodes more complex -> Removes node information about children, but
    // reverts to 2)
    // private final int numParameters;
    // private final boolean hasDefinition;

    public FunctionDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
    }

    protected void checkDefinition(Stmt definition) {

        if (definition == null) {
            return;
        }

        // Allowed statements: CompoundStmt, CXXTryStmt
        if (definition instanceof CompoundStmt ||
                definition instanceof CXXTryStmt) {
            return;
        }

        throw new RuntimeException("Stmt now allowed as a definition of FunctionDecl: " + definition.getClass());
    }

    public FunctionType getFunctionType() {
        return Types.getFunctionType(getType());
    }

    public Type getReturnType() {
        return getFunctionType().getReturnType();
    }

    public void setReturnType(Type returnType) {

        FunctionType functionType = getFunctionType();

        // Create a copy of the function type, to avoid setting the type on all functions with the same signature
        FunctionType functionTypeCopy = (FunctionType) functionType.copy();

        // Replace the return type of the function type copy
        functionTypeCopy.set(FunctionType.RETURN_TYPE, returnType);

        // Set the function type copy as the type of the function in all declarations
        for (var function : getDecls()) {
            function.setType(functionTypeCopy);
        }

    }

    public boolean isInline() {
        return get(IS_INLINE_SPECIFIED);
    }

    /**
     * Has definition if the last child is a Stmt.
     *
     * @return
     */
    public boolean hasBody() {
        return SpecsCollections.lastTry(getChildren()).map(child -> child instanceof Stmt).orElse(false);
    }

    /**
     *
     * @param body
     * @return
     */
    public Optional<Stmt> setBody(Stmt body) {

        // If no body, just add the stmt and return
        if (!hasBody()) {
            addChild(body);
            return Optional.empty();
        }

        // Replace last statement
        int lastIndex = getNumChildren() - 1;
        Stmt previousBody = (Stmt) getChild(lastIndex);

        setChild(lastIndex, body);

        return Optional.of(previousBody);
    }

    @Override
    public String getCode() {
        StringBuilder code = new StringBuilder();

        code.append(getAttributesCode());

        code.append(getDeclarationId(true));

        code.append(getCodeBody());

        return code.toString();
    }

    /**
     *
     * @return the node representing the declaration of this function, if it exists
     */
    public List<FunctionDecl> getPrototypes() {

        // Search for the declaration
        var prototypes = getAppTry().map(app -> app.getFunctionPrototypes(this)).orElse(Collections.emptyList());

        if (!prototypes.isEmpty()) {
            return prototypes;
        }

        // If no body, return immediately
        // There are a number of situations where this should be done
        // e.g. node is still not inserted (no App), method is still not in class
        if (!hasBody()) {
            return Arrays.asList(this);
        }

        return Collections.emptyList();
    }

    /**
     * Legacy method, please use getPrototypes() instead.
     * 
     * @return
     */
    public Optional<FunctionDecl> getDeclaration() {
        return getPrototypes().stream().findFirst();
    }

    /**
     * 
     * @return the node representing the implementation of this function.
     */
    public Optional<FunctionDecl> getImplementation() {

        // If has body, return immediately
        // There are a number of situations where this should be done
        // e.g. node is still not inserted (no App), method is still not in class
        if (hasBody()) {
            return Optional.of(this);
        }
        // System.out.println("App: " + getAppTry());
        // System.out.println("CLEARING CACHE");
        // getAppTry().get().clearCache();
        // Search for the definition
        return getAppTry().flatMap(app -> app.getFunctionImplementation(this));
    }

    /**
     * Legacy method, please use getImplementation() instead.
     * 
     * @return
     */
    public Optional<FunctionDecl> getDefinition() {
        return getImplementation();
    }

    /**
     * 
     * @return all the FunctionDecl related to this function (e.g., prototypes, implementation)
     */
    public List<FunctionDecl> getDecls() {
        List<FunctionDecl> decls = new ArrayList<>();

        // First get declarations
        getPrototypes().stream()
                .forEach(decls::add);

        // Check definition
        getImplementation().ifPresent(decls::add);

        return decls;
    }

    /**
     * 
     * @param useReturnType
     * @return
     */
    public String getDeclarationId(boolean useReturnType) {
        List<String> codeElements = new ArrayList<>();

        if (get(IS_INLINE_SPECIFIED)) {
            if (getAncestor(TranslationUnit.class).get(TranslationUnit.LANGUAGE).get(Language.GNU_INLINE)) {
                codeElements.add("__inline__");
            } else {
                codeElements.add("inline");
            }

        }

        if (get(IS_CONSTEXPR)) {
            codeElements.add("constexpr");
        }

        if (get(STORAGE_CLASS) != StorageClass.None) {
            codeElements.add(get(STORAGE_CLASS).getString());
        }

        if (useReturnType) {
            String returnType = getFunctionType().getReturnType().getCode(this);
            codeElements.add(returnType);
        }

        var currentNamespace = getCurrentNamespace().map(namespace -> namespace + "::").orElse("");
        var typelessCode = currentNamespace + getTypelessCode();
        codeElements.add(typelessCode);

        return codeElements.stream().collect(Collectors.joining(" ")).trim();
    }

    @Override
    public String getTypelessCode() {
        List<String> codeElements = new ArrayList<>();

        codeElements.add(getDeclName() + "(" + getParametersCode() + ")");

        var codeAfterParams = getCodeAfterParams();
        if (!codeAfterParams.isBlank()) {
            codeElements.add(codeAfterParams);
        }

        return codeElements.stream().collect(Collectors.joining(" "));
    }

    public boolean isDefinition() {
        return hasBody();
    }

    private String getCodeAfterParams() {
        FunctionType functionType = getFunctionType();

        if (!(functionType instanceof FunctionProtoType)) {
            return "";
        }

        FunctionProtoType functionProtoType = (FunctionProtoType) functionType;

        return functionProtoType.getCodeAfterParams();

    }

    protected String getCodeBody() {
        StringBuilder code = new StringBuilder();

        if (!hasBody()) {
            code.append(";");
            return code.toString();
        }

        // Translate code of the only child
        Stmt body = getFunctionDefinition().orElseThrow(() -> new RuntimeException("Expected a body"));
        if (body instanceof CXXTryStmt) {
            code.append(" ");
        }
        code.append(body.getCode());

        return code.toString();
    }

    public List<ParmVarDecl> getParameters() {
        return SpecsCollections.peek(getChildren(), ParmVarDecl.class);
    }

    public int getNumParameters() {
        return getParameters().size();
    }

    /**
     * TODO: Make it protected
     *
     * @return
     */
    public Optional<Stmt> getFunctionDefinition() {
        if (!hasBody()) {
            return Optional.empty();
        }

        // Last child should be a Stmt
        return Optional.of(getChild(Stmt.class, getNumChildren() - 1));
    }

    public Optional<CompoundStmt> getBody() {
        return getFunctionDefinition().map(this::getCompoundStmt);
    }

    @Override
    public Optional<CompoundStmt> getNodeScope() {
        return getBody();
    }

    private CompoundStmt getCompoundStmt(Stmt body) {
        if (body instanceof CompoundStmt) {
            return (CompoundStmt) body;
        }

        if (body instanceof CXXTryStmt) {
            return ((CXXTryStmt) body).getBody();
        }

        throw new CaseNotDefinedException(body.getClass());
    }

    protected String getParametersCode() {

        String params = getParameters().stream()
                // .map(param -> parseTypes(param.getType()) + param.getDeclName())
                .map(param -> param.getCode())
                .collect(Collectors.joining(", "));

        return getFunctionType().isVariadic() ? params + ", ..." : params;

    }

    public FunctionDecl setName(String name) {

        set(DECL_NAME, name);

        return this;
    }

    private boolean isCorrespondingCall(CallExpr call) {
        // If declaration exists, declaration of call must exist too, if call refers to this function
        var callDecl = call.getFunctionDecl();

        Optional<Boolean> result = getImplementation().map(decl -> match(decl, callDecl));
        if (result.isPresent()) {
            return result.get();
        }

        result = getPrototypes().stream().map(def -> match(def, callDecl)).findFirst();
        if (result.isPresent()) {
            return result.get();
        }

        throw new RuntimeException("Should not arrive here, either function declaration or definition must be defined");
    }

    private static boolean match(FunctionDecl decl, Optional<FunctionDecl> callDecl) {
        return callDecl.map(cDecl -> decl.equals(cDecl)).orElse(false);
    }

    /**
     *
     * @return all the calls to this function declaration.
     */
    public List<CallExpr> getCalls() {
        // Get all the calls
        App app = getAppTry().orElse(null);
        if (app == null) {
            return Collections.emptyList();
        }

        return app.getDescendantsStream()
                .filter(CallExpr.class::isInstance)
                .map(CallExpr.class::cast)
                .filter(this::isCorrespondingCall)
                .collect(Collectors.toList());
    }

    public void setParameters(List<ParmVarDecl> params) {
        getDecls().forEach(fdecl -> fdecl.setParametersSingle(params));
    }

    private void setParametersSingle(List<ParmVarDecl> params) {
        // Remove current parameters
        removeChildren(ParmVarDecl.class);

        // Add parameters to the beginning of the children
        for (int i = 0; i < params.size(); i++) {
            addChild(i, params.get(i));
        }

        // Update FunctionType
        var newFunctionType = (FunctionType) getFunctionType().copy();

        var paramsTypes = params.stream()
                .map(ParmVarDecl::getType)
                .collect(Collectors.toList());

        newFunctionType.setParamTypes(paramsTypes);

        set(TYPE, newFunctionType);
    }

    public void setParamType(Integer index, Type type) {
        int numParams = getNumParameters();
        if (index >= numParams) {
            ClavaLog.info(
                    "Cannot set type for param at index '" + index + "', function has only '" + numParams + "' params");
            return;
        }

        // Set type of parameter
        var param = getChild(ParmVarDecl.class, index);
        param.setType(type);

        // Set type of the function type
        var newFunctionType = ((FunctionType) getFunctionType().copy());
        newFunctionType.setParamType(index, type);
        setFunctionType(newFunctionType);
    }

    public String getSignature() {
        StringBuilder builder = new StringBuilder();

        builder.append(getDeclName());

        builder.append("(");
        builder.append(getParameters().stream()
                // .map(param -> param.getType().getCode())
                .map(param -> param.get(ValueDecl.TYPE).desugarAll().getCode())
                .collect(Collectors.joining(", ")));
        builder.append(")");

        return builder.toString();
    }

    /**
     * Clones this function (both declaration and definition, if present), and inserts the cloned functions after the
     * corresponding original functions.
     *
     * @param newName
     *
     * @return the definition or the declaration of the cloned function, according to this node being a definition or a
     *         declaration.
     */
    public FunctionDecl cloneAndInsert(String newName, boolean insert) {
        return cloneAndInsert(newName, null, insert);
    }

    public FunctionDecl cloneAndInsertOnFile(String newName, TranslationUnit destinationUnit, boolean insert) {
        return cloneAndInsert(newName, destinationUnit, insert);
    }

    private FunctionDecl cloneAndInsert(String newName, TranslationUnit destinationUnit, boolean insert) {
        // Get both declaration and definition (if present)
        var definition = getImplementation();
        var declaration = getPrototypes();

        Optional<FunctionDecl> newDefinition = definition.map(def -> def.copyFunction(newName));

        if (insert) {
            if (destinationUnit == null) {
                definition.ifPresent(def -> NodeInsertUtils.insertAfter(def, newDefinition.get()));
            } else {
                definition.ifPresent(def -> destinationUnit.addChild(newDefinition.get()));
            }
        }

        List<FunctionDecl> newPrototypes = new ArrayList<>();
        for (var decl : declaration) {
            var newPrototype = decl.copyFunction(newName);
            newPrototypes.add(newPrototype);

            if (insert) {
                if (destinationUnit == null) {
                    NodeInsertUtils.insertAfter(decl, newPrototype);
                } else {
                    // Declarationn should still be inserted next to their original declarations
                    NodeInsertUtils.insertAfter(decl, newPrototype);
                }
            }

        }

        // Return corresponding clone
        if (hasBody()) {
            return newDefinition.get();
        } else {
            return newPrototypes.get(0);
        }
    }

    protected FunctionDecl copyFunction(String newName) {
        var copy = (FunctionDecl) copy();
        copy.setName(newName);

        return copy;
    }

    /**
     * Makes sure the type changes in both the declaration and definition.
     *
     * @param type
     */
    public void setFunctionType(FunctionType type) {
        getPrototypes().stream().forEach(decl -> decl.setType(type));
        getImplementation().ifPresent(decl -> decl.setType(type));
    }

    @Override
    public String getStableId() {
        // Get id of file
        String fileId = getAncestorTry(TranslationUnit.class)
                .map(tunit -> tunit.getStableId())
                .orElse("<no_file>");

        String functionId = "function$" + getDeclarationId(false);

        return fileId + getNodeIdSeparator() + functionId;
    }

}

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
import pt.up.fe.specs.clava.ast.attr.Attribute;
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
            .setDefault(() -> StorageClass.NONE);

    /**
     * True if the "inline" keyword was specified for this function.
     */
    public final static DataKey<Boolean> IS_INLINE = KeyFactory.bool("isInline");

    /**
     * True if this function is explicitly marked as virtual.
     */
    public final static DataKey<Boolean> IS_VIRTUAL = KeyFactory.bool("isVirtual");

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

    // private final FunctionDeclData functionDeclData;

    // private Lazy<FunctionDecl> declaration;
    // private Lazy<FunctionDecl> definition;
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

    // Just for testing
    // public FunctionDecl(FunctionDeclDataV2 data, List<ClavaNode> children) {
    // super(null, null, null, null, Collections.emptyList());
    // functionDeclData = null;
    // }

    public FunctionDecl(DataStore data, Collection<? extends ClavaNode> children) {
        super(data, children);
        // this.functionDeclData = null;
        // this.declaration = Lazy.newInstance(() -> this.findDeclOrDef(true));
        // this.definition = Lazy.newInstance(() -> this.findDeclOrDef(false));

        // SpecsLogs.debug("FUNCTION DECL CHILDREN:" + children);
        // System.out.println("CREATING FUNCTION DECL WITH ID " + get(ID) + ", hash " + hashCode());
    }

    /**
     * Constructor for a function definition.
     */
    /*
    public FunctionDecl(String declName, List<ParmVarDecl> inputs, Type functionType, FunctionDeclData functionDeclData,
            DeclData declData, ClavaNodeInfo info, Stmt definition) {
    
        this(declName, functionType, functionDeclData, declData, info, inputs, definition);
    }
    */

    /**
     *
     *
     * @param declName
     * @param inputs
     * @param returnType
     * @param attributes
     * @param info
     * @param definition
     */
    // protected FunctionDecl(String declName, Type functionType, FunctionDeclData functionDeclData, DeclData declData,
    // ClavaNodeInfo info, List<ParmVarDecl> inputs, Stmt definition) {
    //
    // this(declName, functionType, functionDeclData, declData, info, SpecsCollections.concat(inputs, definition));
    //
    // checkDefinition(definition);
    // }

    // protected FunctionDecl(String declName, Type functionType, FunctionDeclData functionDeclData, DeclData declData,
    // ClavaNodeInfo info, List<? extends ClavaNode> children) {
    //
    // super(new LegacyToDataStore().setFunctionDecl(functionDeclData).setDecl(declData).setNodeInfo(info).getData(),
    // children);
    // // super(declName, functionType, declData, info, children);
    //
    // set(NamedDecl.DECL_NAME, processDeclName(declName));
    // set(ValueDecl.TYPE, processType(functionType));
    //
    // // this.functionDeclData = functionDeclData;
    // this.declaration = Lazy.newInstance(() -> this.findDeclOrDef(true));
    // this.definition = Lazy.newInstance(() -> this.findDeclOrDef(false));
    //
    // }

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

    // @Override
    // protected ClavaNode copyPrivate() {
    // return new FunctionDecl(getDeclName(), getFunctionType(), getFunctionDeclData(), getDeclData(), getInfo(),
    // Collections.emptyList());
    // }

    public FunctionType getFunctionType() {
        return Types.getFunctionType(getType());
    }

    public Type getReturnType() {
        return getFunctionType().getReturnType();
    }

    public void setReturnType(Type returnType) {
        getFunctionType().setReturnType(returnType);
    }

    // @Deprecated
    // protected FunctionDeclData getFunctionDeclData() {
    // return DataStoreToLegacy.getFunctionDecl(getData());
    //
    // // return functionDeclData;
    // }

    public boolean isInline() {
        return get(IS_INLINE);
        // return functionDeclData.isInline();
    }

    /**
     * Has definition if the last child is a Stmt.
     *
     * @return
     */
    public boolean hasBody() {
        return SpecsCollections.lastTry(getChildren()).map(child -> child instanceof Stmt).orElse(false);
    }

    // public Optional<Stmt> setBody(CompoundStmt body) {
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

        for (Attribute attr : get(ATTRIBUTES)) {
            code.append(attr.getCode());

            if (attr.getKind().isInline()) {
                code.append(" ");
            } else {
                code.append("\n");
            }

        }

        code.append(getDeclarationId(true));
        // if (code.toString().contains("%Dummy")) {
        // System.out.println("F DECLARATION:" + code);
        // System.out.println("F TYPE:" + getType());
        // System.out.println("F TYPE TREE:" + getType().toTree());
        // }

        /*
        if (getFunctionDeclData().isInline()) {
            code.append("inline ");
        }
        
        if (getFunctionDeclData().getStorageClass() != StorageClass.NONE) {
            code.append(getFunctionDeclData().getStorageClass().getString()).append(" ");
        }
        
        String returnType = getFunctionType().getReturnType().getCode();
        
        code.append(returnType).append(" ").append(getTypelessCode()).append(getCodeBody());
        */

        code.append(getCodeBody());

        return code.toString();
    }

    /**
     *
     * @return the node representing the declaration of this function, if it exists
     */
    public Optional<FunctionDecl> getDeclaration() {

        // If no body, return immediately
        if (!hasBody()) {
            return Optional.of(this);
        }

        // Search for the declaration

        return getAppTry().flatMap(app -> app.getFunctionDeclaration(this));

        // // If no body, this node already is the declaration
        // if (!hasBody()) {
        // return Optional.of(this);
        // }
        //
        // // Get 'cached' value
        // return Optional.ofNullable(declaration.get());
    }

    /**
     * The function declaration corresponding to the definition of the function represented by this node.
     *
     * @deprecated use .getDefinition() instead
     * @return
     */
    @Deprecated
    public Optional<FunctionDecl> getDefinitionDeclaration() {
        return getDefinition();
        // // If has body, this node is already the definition
        // if (hasBody()) {
        // return Optional.of(this);
        // }
        //
        // // Get 'cached' value
        // return Optional.ofNullable(definition.get());
    }

    public Optional<FunctionDecl> getDefinition() {

        // If has body, return immediately
        if (hasBody()) {
            return Optional.of(this);
        }

        // Search for the definition
        return getAppTry().flatMap(app -> app.getFunctionDefinition(this));
    }

    // private FunctionDecl findDeclaration() {
    //
    // }

    // private FunctionDecl findDeclOrDef(boolean findDecl) {
    // App app = getAppTry().orElse(null);
    //
    // if (app == null) {
    // return null;
    // }
    //
    // // Find function declarations with the same name and the same signature
    // List<FunctionDecl> decls = getApp().getDescendantsStream()
    // .filter(FunctionDecl.class::isInstance)
    // .map(FunctionDecl.class::cast)
    // .filter(fdecl -> findDecl ? !fdecl.hasBody() : fdecl.hasBody())
    // .filter(fdecl -> fdecl.getDeclName().equals(getDeclName()))
    // .filter(fdecl -> fdecl.getDeclarationId(false).equals(getDeclarationId(false)))
    // .collect(Collectors.toList());
    //
    // if (decls.size() > 1) {
    // SpecsLogs.msgInfo("getDeclaration(): Found more than one declaration for function at " + getLocation());
    // }
    //
    // return !decls.isEmpty() ? decls.get(0) : null;
    // }

    public String getDeclarationId(boolean useReturnType) {
        StringBuilder code = new StringBuilder();

        // if (hasAttribute(AttributeKind.OpenCLKernel)) {
        // code.append("__kernel ");
        // }
        // get(ATTRIBUTES).stream()
        // .filter(attr -> attr instanceof OpenCLKernelAttr)
        // .findFirst()
        // .ifPresent(attr -> code.append("__kernel "));

        // if (getFunctionDeclData().hasOpenCLKernelAttr()) {
        // code.append("__kernel ");
        // }

        // if (getFunctionDeclData().isInline()) {
        if (get(IS_INLINE)) {
            if (getAncestor(TranslationUnit.class).get(TranslationUnit.LANGUAGE).get(Language.GNU_INLINE)) {
                code.append("__inline__ ");
            } else {
                code.append("inline ");
            }

        }

        if (get(IS_CONSTEXPR)) {
            code.append("constexpr ");
        }

        if (get(STORAGE_CLASS) != StorageClass.NONE) {
            code.append(get(STORAGE_CLASS).getString()).append(" ");
        }

        if (useReturnType) {
            String returnType = getFunctionType().getReturnType().getCode(this);
            code.append(returnType);
        }

        code.append(" ");

        getCurrentNamespace().ifPresent(namespace -> code.append(namespace).append("::"));

        code.append(getTypelessCode());

        return code.toString().trim();
    }

    @Override
    public String getTypelessCode() {
        // return getTypelessCode(true);
        // }
        //
        // protected String getTypelessCode(boolean useQualifiedName) {
        StringBuilder code = new StringBuilder();

        // boolean useQualifiedName = useQualifiedName();
        // boolean useQualifiedName = false;

        code.append(getDeclName());
        // if (useQualifiedName) {
        // code.append(getQualifiedName());
        // } else {
        // code.append(getDeclName());
        // }

        String parameters = getParametersCode();

        // if (!getFunctionTypeTry().isPresent()) {
        // if (Types.getFunctionType(getType()) == null) {
        // throw new RuntimeException("Expected type to be a function type: " + getType().toTree());
        // // return "<no function type>";
        // }

        // Optional<Type> lastParamType = SpecsCollections.lastTry(getFunctionType().getParamTypes());
        // boolean hasVariadicArguments = lastParamType.map(type -> type instanceof VariadicType).orElse(false);
        // parameters = hasVariadicArguments ? parameters + ", ..." : parameters;

        code.append("(").append(parameters).append(")");

        code.append(getCodeAfterParams());

        return code.toString();
    }

    public boolean isDefinition() {
        return hasBody();
        // return getDefinition().map(def -> def == this).orElse(false);
    }

    // private boolean useQualifiedName() {
    // // True if this is a function declaration outside a record
    // if (getDefinition().map(def -> def == this).orElse(false)) {
    // boolean isInsideRecord = getAncestorTry(RecordDecl.class).isPresent();
    // // System.out.println("FUNC DECL IS INSIDE RECORD? " + isInsideRecord + " (" + getLocation() + ")");
    // return !isInsideRecord;
    // }
    //
    // return false;
    // }

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
        // System.out.println("SETTING NAME:" + get(ID));
        // System.out.println("SETTING NAME OF FUNCTIONDECL " + get(ID));
        // System.out.println("PREVIOUS NAME:" + get(DECL_NAME));
        // String previousName = get(DECL_NAME);

        set(DECL_NAME, name);

        /*
        // Adapt QualifiedName
        String qualifiedName = get(QUALIFIED_NAME);
        SpecsCheck.checkArgument(qualifiedName.endsWith(previousName),
                () -> "Expected qualified name to end with '" + previousName + "': " + qualifiedName);
        
        boolean hasColons = qualifiedName.contains("::");
        
        String qualifiedPrefix = hasColons ? qualifiedName.substring(0, qualifiedName.lastIndexOf("::")) + "::" : "";
        
        String newQualifiedName = qualifiedPrefix + name;
        set(QUALIFIED_NAME, newQualifiedName);
        */

        // System.out.println("OLD QUALIFIED NAME: " + qualifiedName);
        // System.out.println("NEW QUALIFIED NAME: " + newQualifiedName);

        // System.out.println("NEW NAME:" + get(DECL_NAME));
        // System.out.println("THIS HASH: " + hashCode());

        return this;
    }
    //
    // public String getName() {
    // return get(DECL_NAME);
    // }

    // TODO: Replace with set(DECL_NAME) when refactoring to new format is finished
    // public void setName(String name) {
    // // String functionName = getDeclName();
    //
    // // Determine scope of change. If static, only change calls inside the file
    // boolean isStatic = get(STORAGE_CLASS) == StorageClass.STATIC;
    // ClavaNode root = isStatic ? getAncestorTry(TranslationUnit.class).orElse(null) : getAppTry().orElse(null);
    //
    // Optional<FunctionDecl> decl = getDeclaration();
    // Optional<FunctionDecl> def = getDefinitionDeclaration();
    //
    // // Find all calls of this function
    // // System.out.println("FUNCTION DECL " + get(ID) + ", setting name to " + name);
    // if (root != null) {
    // for (CallExpr callExpr : root.getDescendants(CallExpr.class)) {
    // testAndSetCallName(callExpr, name);
    // }
    //
    // }
    //
    // // Change name of itself, both definition and declaration
    // decl.ifPresent(node -> node.setDeclName(name));
    // def.ifPresent(node -> node.setDeclName(name));
    //
    // // setDeclName(name);
    // // if (name.equals("declAndDefNew")) {
    // // System.out.println("BEFORE");
    // // System.out.println("DECL:" + getDeclaration());
    // // System.out.println("DEF:" + getFunctionDefinition());
    // // }
    //
    // // if (name.equals("declAndDefNew")) {
    // // System.out.println("AFTER");
    // // System.out.println("DECL:" + getDeclaration());
    // // System.out.println("DEF:" + getFunctionDefinition());
    // // }
    // // Change name of declaration
    // // getDeclaration()
    // // .filter(functionDecl -> functionDecl != this)
    // // .ifPresent(functionDecl -> functionDecl.setDeclName(name));
    //
    // }

    /**
     * Sets the name of call if the corresponding definition, declaration or both refer to the function represented by
     * this node.
     *
     * @param name
     * @param original
     * @param tentative
     */
    // private void testAndSetCallName(CallExpr call, String newName) {
    // if (isCorrespondingCall(call)) {
    // call.setCallName(newName);
    // }
    // }

    private boolean isCorrespondingCall(CallExpr call) {
        // If declaration exists, declaration of call must exist too, if call refers to this function

        Optional<Boolean> result = getDeclaration().map(decl -> match(decl, call.getDeclaration()));
        if (result.isPresent()) {
            return result.get();
        }

        result = getDefinitionDeclaration().map(def -> match(def, call.getDefinition()));
        if (result.isPresent()) {
            return result.get();
        }

        System.out.println("DECL:" + getDeclaration());
        System.out.println("DEF:" + getDefinitionDeclaration());

        throw new RuntimeException("Should not arrive here, either function declaration or definition must be defined");
        /*
        return getDeclaration().map(decl -> match(decl, call.getDeclaration()))
                // If no declaration, try definition
                .orElse(getFunctionDefinition().map(def -> match(def, call.getDefinition()))
                        .orElseThrow(() -> new RuntimeException(
                                "Should not arrive here, either function declaration or definition must be defined")));
        */
        // Optional<FunctionDecl> functionDecl = getDeclaration();
        // Optional<FunctionDecl> callDecl = call.getDeclaration();

        /*
        Optional<Boolean> declMatch = match(getDeclaration(), call.getDeclaration());
        if (declMatch.isPresent()) {
            return declMatch.get();
        }
        
        Optional<Boolean> defMatch = match(getFunctionDefinition(), call.getDefinition());
        if (defMatch.isPresent()) {
            return defMatch.get();
        }
        
        throw new RuntimeException("Should not arrive here, either function declaration or definition must be defined");
        */
        /*
        if (functionDecl.isPresent()) {
            FunctionDecl decl = functionDecl.get();
        
            if (!callDecl.isPresent()) {
                return;
            }
        
            boolean isFunction = decl == callDecl.get();
        
            if (isFunction) {
                call.setCallName(newName);
            }
        
            return;
        }
        */
        /*
        boolean isDeclOfCall;
        // If declaration exists, check if declaration of call is the
        getDeclaration();
        call.getDeclaration()
        */
    }

    /**
     * True if both objects match, false if they don't match, or empty if the first argument (functionDecl) is not
     * present.
     *
     * @param function
     * @param call
     * @return
     */
    /*
    private static Optional<Boolean> match(Optional<FunctionDecl> functionDecl, Optional<FunctionDecl> callDecl) {
        if (!functionDecl.isPresent()) {
            return Optional.empty();
        }
    
        FunctionDecl decl = functionDecl.get();
    
        if (!callDecl.isPresent()) {
            return Optional.of(false);
        }
    
        return Optional.of(decl == callDecl.get());
    }
    */

    private static boolean match(FunctionDecl decl, Optional<FunctionDecl> callDecl) {
        // return callDecl.map(cDecl -> decl == cDecl).orElse(false);
        return callDecl.map(cDecl -> decl.equals(cDecl)).orElse(false);
        /*
        if (!callDecl.isPresent()) {
            return false;
        }
        
        return decl == callDecl.get();
        */
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

    // @Override
    // public SpecsList<String> getSignatureCustomStrings() {
    // return super.getSignatureCustomStrings().andAdd(getSignature());
    // }

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
        Optional<FunctionDecl> definition = getDefinition();
        Optional<FunctionDecl> declaration = getDeclaration();

        // Optional<FunctionDecl> newDefinition = definition.map(def -> ((FunctionDecl) def.copy()).setName(newName));
        // Optional<FunctionDecl> newDeclaration = declaration.map(decl -> ((FunctionDecl)
        // decl.copy()).setName(newName));

        Optional<FunctionDecl> newDefinition = definition.map(def -> def.copyFunction(newName));
        Optional<FunctionDecl> newDeclaration = declaration.map(decl -> decl.copyFunction(newName));

        if (insert) {
            if (destinationUnit == null) {
                definition.ifPresent(def -> NodeInsertUtils.insertAfter(def, newDefinition.get()));
                declaration.ifPresent(decl -> NodeInsertUtils.insertAfter(decl, newDeclaration.get()));
            } else {
                definition.ifPresent(def -> destinationUnit.addChild(newDefinition.get()));

                // Declarationn should still be inserted next to their original declarations
                declaration.ifPresent(decl -> NodeInsertUtils.insertAfter(decl, newDeclaration.get()));
                /*
                if (this instanceof CXXMethodDecl) {
                declaration.ifPresent(decl -> NodeInsertUtils.insertAfter(decl, newDeclaration.get()));
                } else {
                declaration.ifPresent(decl -> destinationUnit.addChild(newDeclaration.get()));
                }
                */

            }
        }

        // Return corresponding clone
        if (hasBody()) {
            return newDefinition.get();
        } else {
            return newDeclaration.get();
        }
        // definition2.ifPresent(node -> System.out.println("DEF:\n" + node.getCode()));
        // declaration.ifPresent(node -> System.out.println("DECL:\n" + node.getCode()));
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
        getDeclaration().ifPresent(decl -> decl.setType(type));
        getDefinition().ifPresent(decl -> decl.setType(type));
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

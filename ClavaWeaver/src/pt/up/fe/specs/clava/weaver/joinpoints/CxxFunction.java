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

package pt.up.fe.specs.clava.weaver.joinpoints;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ast.decl.CXXMethodDecl;
import pt.up.fe.specs.clava.ast.decl.Decl;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.IncludeDecl;
import pt.up.fe.specs.clava.ast.decl.LinkageSpecDecl;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.stmt.ReturnStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.type.FunctionType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.weaver.CxxActions;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.ACxxWeaverJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ACall;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ADecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFunction;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFunctionType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AParam;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AScope;
import pt.up.fe.specs.clava.weaver.enums.StorageClass;
import pt.up.fe.specs.clava.weaver.importable.AstFactory;
import pt.up.fe.specs.clava.weaver.joinpoints.types.CxxFunctionType;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.enums.EnumHelperWithValue;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;
import pt.up.fe.specs.util.treenode.TreeNodeUtils;

public class CxxFunction extends AFunction {

    // TODO: Move this to generated enums
    private static final Lazy<EnumHelperWithValue<StorageClass>> STORAGE_CLASS = EnumHelperWithValue
            .newLazyHelperWithValue(StorageClass.class);

    private final FunctionDecl function;
    private final ACxxWeaverJoinPoint parent;

    public CxxFunction(FunctionDecl function, ACxxWeaverJoinPoint parent) {
        super(new CxxNamedDecl(function, parent));
        this.function = function;
        this.parent = parent;
    }

    @Override
    public List<? extends AScope> selectBody() {
        CxxScope body = getBodyImpl();

        return body == null ? Collections.emptyList() : Arrays.asList(body);

        // if (!function.hasBody()) {
        // return Collections.emptyList();
        // }
        //
        // return Arrays.asList(new CxxScope(function.getBody().get(), this));
    }

    @Override
    public ACxxWeaverJoinPoint getParentImpl() {
        return parent;
    }

    @Override
    public FunctionDecl getNode() {
        return function;
    }

    @Override
    public AJoinPoint getTypeImpl() {
        return CxxJoinpoints.create(function.getReturnType(), this);
    }

    @Override
    public AFunctionType getFunctionTypeImpl() {
        return (CxxFunctionType) CxxJoinpoints.create(function.getType(), this);
    }

    @Override
    public ACall newCallImpl(AJoinPoint[] args) {
        return AstFactory.callFromFunction(this, args);
    }

    @Override
    public Boolean getHasDefinitionImpl() {
        return function.hasBody();
    }

    @Override
    public void insertImpl(String position, String code) {
        // Stmt literalStmt = ClavaNodeFactory.literalStmt(code);
        Stmt literalStmt = CxxWeaver.getSnippetParser().parseStmt(code);
        insertStmt(literalStmt, position);
    }

    @Override
    public AJoinPoint insertAfterImpl(AJoinPoint node) {
        return CxxActions.insertJpAsStatement(this, node, "after", getWeaverEngine());
    }

    @Override
    public AJoinPoint insertBeforeImpl(AJoinPoint node) {
        return CxxActions.insertJpAsStatement(this, node, "before", getWeaverEngine());
    }

    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint node) {
        if (node.getNode() instanceof LinkageSpecDecl) {
            return CxxActions.insertJp(this, node, "replace", getWeaverEngine());
        }

        return CxxActions.insertJpAsStatement(this, node, "replace", getWeaverEngine());
    }

    private void insertStmt(Stmt newNode, String position) {
        switch (position) {
        case "before":
            NodeInsertUtils.insertBefore(function, newNode);
            break;

        case "after":
            NodeInsertUtils.insertAfter(function, newNode);
            break;

        case "around":
        case "replace":
            NodeInsertUtils.replace(function, newNode);
            break;
        default:
            throw new RuntimeException("Case not defined:" + position);
        }
    }

    @Override
    public List<? extends AParam> selectParam() {
        return function.getParameters().stream()
                .map(param -> CxxJoinpoints.create(param, this, AParam.class))
                .collect(Collectors.toList());
    }

    @Override
    public String declarationImpl(Boolean withReturnType) {
        return function.getDeclarationId(withReturnType);
    }

    @Override
    public CxxScope getBodyImpl() {
        if (!function.hasBody()) {
            return null;
        }

        return (CxxScope) CxxJoinpoints.create(function.getBody().get(), this);
    }

    // TODO check if the new name clashes with other symbol?
    @Override
    public AFunction cloneImpl(String newName) {

        /* make clone and insert after the function of this join point */
        return makeCloneAndInsert(newName, function);
    }

    private AFunction makeCloneAndInsert(String newName, ClavaNode reference) {

        if (function instanceof CXXMethodDecl) {

            SpecsLogs.msgInfo(
                    "function " + function.getDeclName() + " is a class method, which is not supported yet for clone");
            return null;
        }

        FunctionDecl newFunc = makeNewFuncDecl(newName);

        if (reference instanceof FunctionDecl) {

            NodeInsertUtils.insertAfter(function, newFunc);

        } else if (reference instanceof TranslationUnit) {

            ((TranslationUnit) reference).addChild(newFunc);

        } else {
            throw new IllegalArgumentException(
                    "The node (" + reference + ") needs to be either a FuncDecl or a TranslationUnit.");
        }

        // change the ids of stuff
        // newFunc.getDescendantsStream().forEach(n -> n.setInfo(ClavaNodeInfo.undefinedInfo()));

        return CxxJoinpoints.create(newFunc, null, AFunction.class);
    }

    /**
     * Make a new {@link FunctionDecl} based on the node of this join point and the provided name.
     *
     * @param newName
     * @return
     */
    private FunctionDecl makeNewFuncDecl(String newName) {

        // make sure to see if we can just copy
        // function.getDefinition().ifPresent(def -> newFunc.addChild(def.copy()));
        Stmt definition = function.getFunctionDefinition().map(stmt -> (Stmt) stmt.copy()).orElse(null);

        // List<ClavaNode> originalCasts = function.getFunctionDefinition().get()
        // .getDescendants();
        //
        // List<ClavaNode> copiedCasts = definition.getDescendants();
        /*
        System.out.println("ORIGINAL CAST:" + originalCasts.get(0));
        System.out.println("COPIED CAST:" + copiedCasts.get(0));
        copiedCasts.get(0).setType(new BuiltinType(BuiltinKind.FLOAT));
        System.out.println("ORIGINAL CAST 2:" + originalCasts.get(0));
        System.out.println("COPIED CAST 2:" + copiedCasts.get(0));
        
        System.out.println("ORIGINAL CAST 2 code:" + originalCasts.get(0).getCode());
        System.out.println("COPIED CAST 2 code:" + copiedCasts.get(0).getCode());
        */
        // for (int i = 0; i < originalCasts.size(); i++) {
        // if (originalCasts.get(i) == copiedCasts.get(i)) {
        // System.out.println("FOUND SAME");
        // }
        // // System.out.println("ARE SAME? " + (originalCasts.get(i) == copiedCasts.get(i)));
        // }
        // System.out.println("FINISH");

        // make a new function declaration with the new name

        // FunctionDecl newFunc = ClavaNodeFactory.functionDecl(newName,
        // function.getParameters(),
        // (FunctionType) function.getFunctionType().copy(),
        // function.getFunctionDeclData(), // check
        // function.getDeclData(), // check
        // ClavaNodeInfo.undefinedInfo(), // check
        // definition);

        FunctionDecl newFunc = getFactory().functionDecl(newName, function.getFunctionType());

        newFunc.setParameters(function.getParameters());
        if (definition != null) {
            newFunc.setBody(definition);
        }

        return newFunc;
    }

    @Override
    public String cloneOnFileImpl(String newName) {

        boolean isCxx = function.getAncestor(TranslationUnit.class).isCXXUnit();
        String extension = isCxx ? ".cpp" : ".c";

        String prefix = newName;

        String fileName = prefix + extension;

        return cloneOnFileImpl(newName, fileName);
    }

    @Override
    // TODO: copy header file inclusion
    public String cloneOnFileImpl(String newName, String fileName) {

        if (function.hasBody()) {
            /* if this is a definition, add the clone to the correct file */

            App app = getRootImpl().getNode();

            Optional<TranslationUnit> file = app.getFile(fileName);

            if (!file.isPresent()) {

                // String path = getRootImpl().getBaseFolderImpl();
                TranslationUnit tu = getFactory().translationUnit(new File(fileName), Collections.emptyList());

                app.addFile(tu);

                file = Optional.of(tu);
            }

            makeCloneAndInsert(newName, file.get());

            /* copy headers from the current file to the file with the clone */
            List<IncludeDecl> allIncludes = getWrapperIncludesFromFile(file.get());
            allIncludes.stream().forEach(file.get()::addInclude);

        } else {
            /*otherwise, add the clone to the original place in order to be included where needed */

            makeCloneAndInsert(newName, function);
        }

        return fileName;
    }

    /**
     * XXX: copied from CallWrap
     */
    private List<IncludeDecl> getWrapperIncludesFromFile(TranslationUnit newTu) {

        TranslationUnit callFile = function.getAncestor(TranslationUnit.class);

        return TreeNodeUtils.copy(callFile.getIncludes().getIncludes());

    }

    @Override
    public String[] getParamNamesArrayImpl() {

        return function.getParameters()
                .stream()
                .map(ParmVarDecl::getCode)
                .collect(Collectors.toList())
                .toArray(new String[0]);
    }

    @Override
    public AParam[] getParamsArrayImpl() {
        return selectParam().toArray(new AParam[0]);
    }

    @Override
    public void insertReturnImpl(String code) {
        // insertReturnImpl(CxxJoinpoints.create(ClavaNodeFactory.literalStmt(code), null));
        insertReturnImpl(CxxJoinpoints.create(CxxWeaver.getSnippetParser().parseStmt(code), null));
    }

    @Override
    public void insertReturnImpl(AJoinPoint code) {
        // Does not take into account situations where functions returns in all paths of an if/else.
        // This means it can lead to dead-code, although for C/C++ that does not seem to be problematic.

        // Do not insert if function has no implementation
        if (!function.hasBody()) {
            return;
        }

        List<Stmt> bodyStmts = function.getBody().get().toStatements();

        // Check if it has return statement
        Stmt lastStmt = SpecsCollections.last(bodyStmts);
        ReturnStmt lastReturnStmt = lastStmt instanceof ReturnStmt ? (ReturnStmt) lastStmt : null;

        // Get list of all return statements inside children
        List<ReturnStmt> returnStatements = bodyStmts.stream()
                .flatMap(Stmt::getDescendantsStream)
                .filter(ReturnStmt.class::isInstance)
                .map(ReturnStmt.class::cast)
                .collect(Collectors.toList());

        if (lastReturnStmt != null) {
            returnStatements = SpecsCollections.concat(returnStatements, lastReturnStmt);
        }
        // If there is no return in the body, add at the end of the function
        else {
            getBodyImpl().insertEnd(code);
        }

        for (ReturnStmt returnStmt : returnStatements) {
            ACxxWeaverJoinPoint returnJp = CxxJoinpoints.create(returnStmt, null);
            returnJp.insertBefore(code);
        }
    }

    /**
     * Uses the declaration, without the return type, to identify the function.
     */
    @Override
    public String getIdImpl() {
        return declarationImpl(false);
    }

    @Override
    public AJoinPoint getDeclarationJpImpl() {
        return function.getDeclaration()
                .map(node -> CxxJoinpoints.create(node, null))
                .orElse(null);
    }

    @Override
    public AJoinPoint getDefinitionJpImpl() {
        return function.getDefinition()
                .map(node -> CxxJoinpoints.create(node, null))
                .orElse(null);
    }

    /**
     * Setting the type of a Function join point sets the return type
     */
    @Override
    public void setTypeImpl(AJoinPoint type) {
        // Get new type to set
        Type newType = (Type) type.getNode();

        FunctionType functionType = function.getFunctionType();

        // Create a copy of the function type, to avoid setting the type on all functions with the same signature
        FunctionType functionTypeCopy = (FunctionType) functionType.copy();

        // Replace the return type of the function type copy
        functionTypeCopy.set(FunctionType.RETURN_TYPE, newType);
        // CxxActions.replace(functionTypeCopy.getReturnType(), newType, getWeaverEngine());

        // Set the function type copy as the type of the function
        function.setType(functionTypeCopy);
    }

    // @Override
    // public void defNameImpl(String value) {
    // function.setName(value);
    // }

    @Override
    public void defNameImpl(String value) {
        // Set both the names of corresponding definition and declaration
        // Needs to first fetch both definition and declaration.
        // If one is renamed before fetching the other, the other will not be found
        Optional<FunctionDecl> def = function.getDefinition();
        Optional<FunctionDecl> decl = function.getDeclaration();
        // System.out.println("DEF:" + def);
        // System.out.println("DECL:" + decl);
        def.ifPresent(node -> node.setName(value));
        decl.ifPresent(node -> node.setName(value));
    }

    @Override
    public void setNameImpl(String name) {
        defNameImpl(name);

    }

    @Override
    public StorageClass getStorageClassImpl() {
        return STORAGE_CLASS.get().fromValue(function.get(FunctionDecl.STORAGE_CLASS).name().toLowerCase());
    }

    @Override
    public Boolean getIsInlineImpl() {
        return function.get(FunctionDecl.IS_INLINE);
    }

    @Override
    public Boolean getIsVirtualImpl() {
        return function.get(FunctionDecl.IS_VIRTUAL);
    }

    @Override
    public Boolean getIsModulePrivateImpl() {
        return function.get(FunctionDecl.IS_MODULE_PRIVATE);
    }

    @Override
    public Boolean getIsPureImpl() {
        return function.get(FunctionDecl.IS_PURE);
    }

    @Override
    public Boolean getIsDeleteImpl() {
        return function.get(FunctionDecl.IS_DELETED);
    }

    @Override
    public List<? extends ADecl> selectDecl() {
        return function.getDescendants(Decl.class).stream()
                .map(decl -> CxxJoinpoints.create(decl, this, ADecl.class))
                .collect(Collectors.toList());
    }

    @Override
    public ACall[] getCallsArrayImpl() {
        return function.getCalls().stream()
                .map(call -> CxxJoinpoints.create(call, this, ACall.class))
                .toArray(ACall[]::new);
    }

    @Override
    public void defParamsImpl(AParam[] value) {
        List<ParmVarDecl> newParams = Arrays.stream(value)
                .map(param -> (ParmVarDecl) param.getNode())
                .collect(Collectors.toList());

        function.setParameters(newParams);
    }

    @Override
    public void defParamsImpl(String[] value) {
        AParam[] params = new AParam[value.length];

        // Each value is a type - varName pair, separate them by last space
        for (int i = 0; i < value.length; i++) {
            String typeVarname = value[i];

            typeVarname = typeVarname.trim();
            int indexOfSpace = typeVarname.lastIndexOf(' ');
            if (indexOfSpace == -1) {
                throw new RuntimeException("Expected parameter to be a type - varName pair, separated by a space");
            }

            String type = typeVarname.substring(0, indexOfSpace).trim();
            String varName = typeVarname.substring(indexOfSpace + 1).trim();

            ParmVarDecl parmVarDecl = getFactory().parmVarDecl(varName, getFactory().literalType(type));

            params[i] = CxxJoinpoints.create(parmVarDecl, this, AParam.class);
        }

        defParamsImpl(params);
    }

    @Override
    public void setParamsImpl(AParam[] params) {
        defParamsImpl(params);
    }

    @Override
    public void setParamsFromStringsImpl(String[] params) {
        defParamsImpl(params);
    }

    @Override
    public String getSignatureImpl() {
        return function.getSignature();
    }
}

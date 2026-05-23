/**
 * Copyright 2016 SPeCS.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package pt.up.fe.specs.clava.weaver.joinpoints;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.lara.interpreter.weaver.interf.enums.InsertPosition;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.attr.CUDAGlobalAttr;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.IncludeDecl;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.extra.App;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.stmt.CompoundStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.type.FunctionType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.weaver.CxxActions;
import pt.up.fe.specs.clava.weaver.CxxJoinpoints;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ABody;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ACall;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFile;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFunction;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFunctionType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinpoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AParam;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AScope;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;
import pt.up.fe.specs.clava.weaver.enums.StorageClass;
import pt.up.fe.specs.clava.weaver.importable.AstFactory;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.lazy.Lazy;
import pt.up.fe.specs.util.lazy.ThreadSafeLazy;
import pt.up.fe.specs.util.treenode.NodeInsertUtils;
import pt.up.fe.specs.util.treenode.TreeNodeUtils;

public class CxxFunction<Self extends CxxFunction<Self>> extends AFunction<Self> {

    private static final Lazy<Map<pt.up.fe.specs.clava.ast.decl.enums.StorageClass, StorageClass>> STORAGE_TYPE = new ThreadSafeLazy<>(
            () -> buildStorageTypeMap());

    private static Map<pt.up.fe.specs.clava.ast.decl.enums.StorageClass, StorageClass> buildStorageTypeMap() {
        HashMap<pt.up.fe.specs.clava.ast.decl.enums.StorageClass, StorageClass> storageClasses = new HashMap<>();

        storageClasses.put(pt.up.fe.specs.clava.ast.decl.enums.StorageClass.None, StorageClass.NONE);
        storageClasses.put(pt.up.fe.specs.clava.ast.decl.enums.StorageClass.Extern, StorageClass.EXTERN);
        storageClasses.put(pt.up.fe.specs.clava.ast.decl.enums.StorageClass.Static, StorageClass.STATIC);
        storageClasses.put(pt.up.fe.specs.clava.ast.decl.enums.StorageClass.PrivateExtern, StorageClass.PRIVATE_EXTERN);
        storageClasses.put(pt.up.fe.specs.clava.ast.decl.enums.StorageClass.Auto, StorageClass.AUTO);
        storageClasses.put(pt.up.fe.specs.clava.ast.decl.enums.StorageClass.Register, StorageClass.REGISTER);

        return storageClasses;
    }

    public CxxFunction(FunctionDecl function, CxxWeaver weaver) {
        super(function, weaver);
    }

    @Override
    public FunctionDecl getNodeImpl() {
        return (FunctionDecl) super.getNodeImpl();
    }

    @Override
    public AType<?> getTypeImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().getReturnType(), getWeaverEngine(), AType.class);
    }

    @Override
    public AFunctionType<?> getFunctionTypeImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().getFunctionType(), getWeaverEngine(), AFunctionType.class);
    }

    @Override
    public ACall<?> newCallImpl(AJoinpoint<?>[] args) {
        return AstFactory.callFromFunction(getWeaverEngine(), this, SpecsCollections.asListT(AJoinpoint.class, (Object[]) args));
    }

    @Override
    public boolean getHasDefinitionImpl() {
        return getIsImplementationImpl();
    }

    @Override
    public boolean getIsImplementationImpl() {
        return this.getNodeImpl().hasBody();
    }

    @Override
    public boolean getIsPrototypeImpl() {
        return !this.getNodeImpl().hasBody();
    }

    private AJoinpoint<?> processNodeToInsert(AJoinpoint<?> node) {

        // If node is an expression or VarDecl, convert to Stmt first
        var clavaNode = node.getNodeImpl();

        if (clavaNode instanceof VarDecl || clavaNode instanceof Expr) {
            return CxxJoinpoints.create(ClavaNodes.toStmt(clavaNode), getWeaverEngine());
        }

        // Otherwise, do nothing
        return node;
    }

    @Override
    public AJoinpoint<?>[] insertImpl(InsertPosition position, String code) {
        // Stmt literalStmt = ClavaNodeFactory.literalStmt(code);
        Stmt literalStmt = getWeaverEngine().getSnippetParser().parseStmt(code);
        return insertStmt(literalStmt, position);
    }

    @Override
    public AJoinpoint<?> insertAfterImpl(AJoinpoint<?> node) {
        var processNode = processNodeToInsert(node);
        return CxxActions.insertJp(this, processNode, "after", getWeaverEngine());
    }

    @Override
    public AJoinpoint<?> insertAfterImpl(String code) {
        return insertAfterImpl(CxxJoinpoints.create(getWeaverEngine().getSnippetParser().parseStmt(code),
                getWeaverEngine()));
    }

    @Override
    public AJoinpoint<?> insertBeforeImpl(AJoinpoint<?> node) {
        var processNode = processNodeToInsert(node);
        return CxxActions.insertJp(this, processNode, "before", getWeaverEngine());
    }

    @Override
    public AJoinpoint<?> insertBeforeImpl(String code) {
        return insertBeforeImpl(CxxJoinpoints.create(getWeaverEngine().getSnippetParser().parseStmt(code),
                getWeaverEngine()));
    }

    @Override
    public AJoinpoint<?> replaceWithImpl(AJoinpoint<?> node) {
        var processNode = processNodeToInsert(node);
        return CxxActions.insertJp(this, processNode, "replace", getWeaverEngine());
    }

    private AJoinpoint<?>[] insertStmt(Stmt newNode, InsertPosition position) {
        switch (position) {
            case BEFORE:
                NodeInsertUtils.insertBefore(this.getNodeImpl(), newNode);
                return null;

            case AFTER:
                NodeInsertUtils.insertAfter(this.getNodeImpl(), newNode);
                return null;

            case REPLACE:
                NodeInsertUtils.replace(this.getNodeImpl(), newNode);
                return new AJoinpoint<?>[]{CxxJoinpoints.create(newNode, getWeaverEngine())};
            default:
                throw new RuntimeException("Case not defined:" + position);
        }
    }

    @Override
    public String getGetDeclarationImpl(boolean withReturnType) {
        return this.getNodeImpl().getDeclarationId(withReturnType);
    }

    @Override
    public ABody<?> getBodyImpl() {
        if (!this.getNodeImpl().hasBody()) {
            return null;
        }

        return CxxJoinpoints.create(this.getNodeImpl().getBody().get(), getWeaverEngine(), ABody.class);
    }

    @Override
    public AFunction<?> cloneImpl(String newName, boolean insert) {
        /* make clone and insert after the function of this join point */
        return makeCloneAndInsert(newName, this.getNodeImpl(), insert);
    }

    private AFunction<?> makeCloneAndInsert(String newName, ClavaNode reference, boolean insert) {

        FunctionDecl newFunc = null;
        if (reference instanceof FunctionDecl) {
            newFunc = this.getNodeImpl().cloneAndInsert(newName, insert);
        } else if (reference instanceof TranslationUnit) {
            newFunc = this.getNodeImpl().cloneAndInsertOnFile(newName, (TranslationUnit) reference, insert);
        } else {
            throw new IllegalArgumentException(
                    "The node (" + reference + ") needs to be either a FuncDecl or a TranslationUnit.");
        }

        return CxxJoinpoints.create(newFunc, getWeaverEngine(), AFunction.class);
    }

    @Override
    public AFunction<?> cloneOnFileImpl(String newName, String fileName) {
        if (fileName == null) {
            boolean isCxx = this.getNodeImpl().getAncestor(TranslationUnit.class).isCXXUnit();
            String extension = getIsPrototypeImpl() ? ".h" : isCxx ? ".cpp" : ".c";

            String prefix = newName;

            fileName = prefix + extension;
        }


        // First, check if the given filename is the same as a file in the AST
        App app = (App) getRootImpl().getNodeImpl();
        var currentFile = new File(fileName);

        var existingFile = app.getTranslationUnits().stream()
                .filter(tu -> tu.getFile().equals(currentFile))
                .findFirst();

        if (existingFile.isPresent()) {
            return cloneOnFileImpl(newName, new CxxFile<>(existingFile.get(), getWeaverEngine()));
        }

        // Extract relative path
        var relativePath = currentFile.getParentFile() != null ? currentFile.getParent() : null;

        // Create a new file
        var newFile = AstFactory.file(getWeaverEngine(), fileName, relativePath);

        // Set same source foldername
        var originalFile = this.getNodeImpl().getAncestorTry(TranslationUnit.class).orElse(null);
        if (originalFile != null) {
            newFile.getNodeImpl().copyValue(TranslationUnit.SOURCE_FOLDERNAME, originalFile);
        }

        app.addFile((TranslationUnit) newFile.getNodeImpl());

        return cloneOnFileImpl(newName, newFile);
    }

    @Override
    // TODO: copy header file inclusion
    public AFunction<?> cloneOnFileImpl(String newName, AFile<?> file) {
        var tu = (TranslationUnit) file.getNodeImpl();

        var cloneFunction = makeCloneAndInsert(newName, tu, true);

        /* copy headers from the current file to the file with the clone */
        TranslationUnit originalFile = this.getNodeImpl().getAncestorTry(TranslationUnit.class).orElse(null);
        if (originalFile != null) {
            var includesCopy = TreeNodeUtils.copy(originalFile.getIncludes().getIncludes());

            File baseIncludePath = null;

            // Add as many ../ as folders in the relative folder
            var relativeFolderDepth = tu.getRelativeFolderpath().map(folder -> SpecsIo.getDepth(new File(folder)))
                    .orElse(0);
            for (int i = 0; i < relativeFolderDepth; i++) {
                baseIncludePath = new File(baseIncludePath, "../");
            }

            // Add relative folder of original file
            var relativeDepth = baseIncludePath;
            baseIncludePath = originalFile.getRelativeFolderpath()
                    .map(relativeFolder -> new File(relativeDepth, relativeFolder))
                    .orElse(baseIncludePath);

            // Adapt includes
            for (var includeDecl : includesCopy) {
                var include = includeDecl.getInclude();

                // If angled, ignore
                if (include.isAngled()) {
                    continue;
                }

                var newInclude = include.setInclude(new File(baseIncludePath, include.getInclude()).toString());
                includeDecl.set(IncludeDecl.INCLUDE, newInclude);
            }

            // Add includes
            includesCopy.stream().forEach(tu::addInclude);

        }

        return cloneFunction;
    }

    @Override
    public String[] getParamNamesImpl() {
        return this.getNodeImpl().getParameters()
                .stream()
                .map(ParmVarDecl::getCode)
                .collect(Collectors.toList())
                .toArray(new String[0]);
    }

    @Override
    public AParam<?>[] getParamsImpl() {
        return this.getNodeImpl().getParameters()
                .stream()
                .map(param -> CxxJoinpoints.create(param,
                        getWeaverEngine(), AParam.class))
                .collect(Collectors.toList())
                .toArray(new AParam[0]);
    }

    @Override
    public AJoinpoint<?> insertReturnImpl(String code) {
        return insertReturnImpl(CxxJoinpoints.create(getWeaverEngine().getSnippetParser().parseStmt(code),
                getWeaverEngine()));
    }

    @Override
    public AJoinpoint<?> insertReturnImpl(AJoinpoint<?> code) {
        // Does not take into account situations where functions returns in all paths of an if/else.
        // This means it can lead to dead-code, although for C/C++ that does not seem to be problematic.

        // Do not insert if function has no implementation
        if (!this.getNodeImpl().hasBody()) {
            ClavaLog.info("insertReturn: could not insert in function without body");
            return null;
        }

        return CxxActions.insertReturn(getBodyImpl(), code, getWeaverEngine());
    }

    /**
     * Uses the declaration, without the return type, to identify the function.
     */
    @Override
    public String getIdImpl() {
        return getGetDeclarationImpl(false);
    }

    @Override
    public AFunction<?>[] getDeclarationJpsImpl() {
        return this.getNodeImpl().getPrototypes().stream()
                .map(node -> CxxJoinpoints.create(node, getWeaverEngine(), AFunction.class))
                .toArray(AFunction[]::new);
    }

    @Override
    public AFunction<?> getDeclarationJpImpl() {
        var prototypes = getDeclarationJpsImpl();

        if (prototypes.length == 0) {
            return null;
        }

        if (prototypes.length != 1) {
            ClavaLog.debug(
                    "$function.declarationJp: found more than one prototype, returning the first prototype that was found");
        }

        return prototypes[0];
    }

    @Override
    public AFunction<?> getDefinitionJpImpl() {
        return this.getNodeImpl().getImplementation()
                .map(node -> CxxJoinpoints.create(node, getWeaverEngine(), AFunction.class))
                .orElse(null);
    }

    /**
     * Setting the type of a Function join point sets the return type
     */
    @Override
    public void setTypeImpl(AType<?> type) {
        setReturnTypeImpl(type);
    }

    @Override
    public void setNameImpl(String name) {
        // Set both the names of corresponding definition and declaration
        // Needs to first fetch both definition and declaration.
        // If one is renamed before fetching the other, the other will not be found

        var impl = this.getNodeImpl().getImplementation();
        var proto = this.getNodeImpl().getPrototypes();

        impl.ifPresent(node -> node.setName(name));
        proto.stream().forEach(node -> node.setName(name));
    }

    @Override
    public StorageClass getStorageClassImpl() {
        var nodeStorageClass = this.getNodeImpl().get(FunctionDecl.STORAGE_CLASS);
        if (nodeStorageClass == null) {
            throw new RuntimeException("Storage class of function '" + getSignatureImpl() + "' is null");
        }

        StorageClass jpStorageClass = STORAGE_TYPE.get().get(nodeStorageClass);
        if (jpStorageClass == null) {
            throw new RuntimeException("Storage class '" + nodeStorageClass + "' of function '" + getSignatureImpl()
                    + "' is not supported in the join point model");
        }

        return jpStorageClass;
    }

    @Override
    public boolean setStorageClassImpl(StorageClass storageClass) {
        var nodeStorageClass = STORAGE_TYPE.get().entrySet().stream()
                .filter(entry -> entry.getValue() == storageClass)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new RuntimeException(
                        "Storage class '" + storageClass + "' is not supported in the join point model"));

        return this.getNodeImpl().setStorageClass(nodeStorageClass);
    }

    @Override
    public boolean getIsInlineImpl() {
        return this.getNodeImpl().get(FunctionDecl.IS_INLINE_SPECIFIED);
    }

    @Override
    public boolean getIsVirtualImpl() {
        return this.getNodeImpl().get(FunctionDecl.IS_VIRTUAL_AS_WRITTEN);
    }

    @Override
    public boolean getIsModulePrivateImpl() {
        return this.getNodeImpl().get(FunctionDecl.IS_MODULE_PRIVATE);
    }

    @Override
    public boolean getIsPureImpl() {
        return this.getNodeImpl().get(FunctionDecl.IS_PURE);
    }

    @Override
    public boolean getIsDeleteImpl() {
        return this.getNodeImpl().get(FunctionDecl.IS_DELETED);
    }

    @Override
    public ACall<?>[] getCallsImpl() {
        return this.getNodeImpl().getCalls().stream()
                .map(call -> CxxJoinpoints.create(call, getWeaverEngine(), ACall.class))
                .toArray(ACall[]::new);
    }

    @Override
    public void setParamsImpl(AParam<?>[] params) {
        List<ParmVarDecl> newParams = Arrays.stream(
                params)
                .map(param -> (ParmVarDecl) param.getNodeImpl())
                .collect(Collectors.toList());

        this.getNodeImpl().setParameters(newParams);
    }

    @Override
    public void setParamsFromStringsImpl(String[] params) {
        AParam<?>[] newParams = new AParam<?>[params.length];

        // Each value is a type - varName pair, separate them by last space
        for (int i = 0; i < params.length; i++) {
            String typeVarname = params[i];

            var parmVarDecl = ClavaNodes.toParam(typeVarname, this.getNodeImpl());

            newParams[i] = CxxJoinpoints.create(parmVarDecl, getWeaverEngine(), AParam.class);
        }

        setParamsImpl(newParams);
    }

    @Override
    public String getSignatureImpl() {
        return this.getNodeImpl().getSignature();
    }

    @Override
    public void setBodyImpl(AScope<?> body) {
        this.getNodeImpl().setBody((CompoundStmt) body.getNodeImpl());
    }

    @Override
    public void setFunctionTypeImpl(AFunctionType<?> functionType) {
        this.getNodeImpl().setFunctionType((FunctionType) functionType.getNodeImpl());
    }

    @Override
    public AType<?> getReturnTypeImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().getReturnType(), getWeaverEngine(), AType.class);
    }

    @Override
    public void setReturnTypeImpl(AType<?> returnType) {
        this.getNodeImpl().setReturnType((Type) returnType.getNodeImpl());
    }

    @Override
    public void setParamTypeImpl(int index, AType<?> newType) {
        this.getNodeImpl().setParamType(index, (Type) newType.getNodeImpl());
    }

    @Override
    public void addParamImpl(AParam<?> param) {
        var originalParams = getParamsImpl();
        var newParams = Arrays.copyOf(originalParams, originalParams.length + 1);

        newParams[newParams.length - 1] = param;

        setParamsImpl(newParams);
    }

    @Override
    public void addParamImpl(String name, AType<?> type) {
        ClavaNode paramNode;
        if (type == null) {
            paramNode = ClavaNodes.toParam(name, this.getNodeImpl());
        } else {
            paramNode = getFactory().parmVarDecl(name, (Type) type.getNodeImpl());
        }
        addParamImpl(CxxJoinpoints.create(paramNode, getWeaverEngine(), AParam.class));
    }

    @Override
    public void setParamImpl(int index, AParam<?> param) {
        var params = getParamsImpl();

        if (index >= params.length) {
            SpecsLogs.info("Tried to set parameter '" + param.getCodeImpl() + "' at index '" + index
                    + "' but function '" + this.getNodeImpl().getSignature() + "' only has " + params.length + " parameters");
            return;
        }

        params[index] = param;

        setParamsImpl(params);
    }

    @Override
    public void setParamImpl(int index, String name, AType<?> type) {
        ClavaNode paramNode;

        if (type == null) {
            paramNode = ClavaNodes.toParam(name, this.getNodeImpl());
        } else {
            paramNode = getFactory().parmVarDecl(name, (Type) type.getNodeImpl());
        }

        setParamImpl(index, CxxJoinpoints.create(paramNode, getWeaverEngine(), AParam.class));
    }

    @Override
    public boolean getIsCudaKernelImpl() {
        return this.getNodeImpl().get(FunctionDecl.ATTRIBUTES).stream()
                .filter(attr -> attr instanceof CUDAGlobalAttr)
                .findFirst()
                .isPresent();
    }

    @Override
    public AFunction<?> getCanonicalImpl() {
        return CxxJoinpoints.create(this.getNodeImpl().canonical(), getWeaverEngine(), AFunction.class);
    }

    @Override
    public boolean getIsCanonicalImpl() {
        return this.getNodeImpl().isCanonical();
    }

}

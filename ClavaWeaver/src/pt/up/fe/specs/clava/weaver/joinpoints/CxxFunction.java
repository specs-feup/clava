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
import java.util.stream.Collectors;

import pt.up.fe.specs.clava.ClavaLog;
import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaNodes;
import pt.up.fe.specs.clava.ast.attr.CUDAGlobalAttr;
import pt.up.fe.specs.clava.ast.decl.Decl;
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
import pt.up.fe.specs.clava.weaver.CxxSelects;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ABody;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ACall;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.ADecl;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFile;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFunction;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFunctionType;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AJoinPoint;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AParam;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AScope;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AType;
import pt.up.fe.specs.clava.weaver.enums.StorageClass;
import pt.up.fe.specs.clava.weaver.importable.AstFactory;
import pt.up.fe.specs.util.SpecsIo;
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

    public CxxFunction(FunctionDecl function) {
        super(new CxxDeclarator(function));
        this.function = function;
    }

    @Override
    public List<? extends ABody> selectBody() {
        var body = getBodyImpl();

        return body == null ? Collections.emptyList() : Arrays.asList(body);
    }

    @Override
    public FunctionDecl getNode() {
        return function;
    }

    @Override
    public AType getTypeImpl() {
        return CxxJoinpoints.create(function.getReturnType(), AType.class);
    }

    @Override
    public AFunctionType getFunctionTypeImpl() {
        return CxxJoinpoints.create(function.getFunctionType(), AFunctionType.class);
    }

    @Override
    public ACall newCallImpl(AJoinPoint[] args) {
        return AstFactory.callFromFunction(this, args);
    }

    @Override
    public Boolean getHasDefinitionImpl() {
        return getIsImplementationImpl();
    }

    @Override
    public Boolean getIsImplementationImpl() {
        return function.hasBody();
    }

    @Override
    public Boolean getIsPrototypeImpl() {
        return !function.hasBody();
    }

    private AJoinPoint processNodeToInsert(AJoinPoint node) {

        // If node is an expression or VarDecl, convert to Stmt first
        var clavaNode = node.getNode();

        if (clavaNode instanceof VarDecl || clavaNode instanceof Expr) {
            return CxxJoinpoints.create(ClavaNodes.toStmt(clavaNode));
        }

        // Otherwise, do nothing
        return node;
    }

    @Override
    public AJoinPoint[] insertImpl(String position, String code) {
        // Stmt literalStmt = ClavaNodeFactory.literalStmt(code);
        Stmt literalStmt = CxxWeaver.getSnippetParser().parseStmt(code);
        return insertStmt(literalStmt, position);
    }

    @Override
    public AJoinPoint insertAfterImpl(AJoinPoint node) {
        var processNode = processNodeToInsert(node);
        return CxxActions.insertJp(this, processNode, "after", getWeaverEngine());
    }

    @Override
    public AJoinPoint insertAfterImpl(String code) {
        return insertAfterImpl(CxxJoinpoints.create(CxxWeaver.getSnippetParser().parseStmt(code)));
    }

    @Override
    public AJoinPoint insertBeforeImpl(AJoinPoint node) {
        var processNode = processNodeToInsert(node);
        return CxxActions.insertJp(this, processNode, "before", getWeaverEngine());
    }

    @Override
    public AJoinPoint insertBeforeImpl(String code) {
        return insertBeforeImpl(CxxJoinpoints.create(CxxWeaver.getSnippetParser().parseStmt(code)));
    }

    @Override
    public AJoinPoint replaceWithImpl(AJoinPoint node) {
        var processNode = processNodeToInsert(node);
        return CxxActions.insertJp(this, processNode, "replace", getWeaverEngine());
    }

    private AJoinPoint[] insertStmt(Stmt newNode, String position) {
        switch (position) {
        case "before":
            NodeInsertUtils.insertBefore(function, newNode);
            return null;

        case "after":
            NodeInsertUtils.insertAfter(function, newNode);
            return null;

        case "around":
        case "replace":
            NodeInsertUtils.replace(function, newNode);
            return new AJoinPoint[] { CxxJoinpoints.create(newNode) };
        default:
            throw new RuntimeException("Case not defined:" + position);
        }
    }

    @Override
    public List<? extends AParam> selectParam() {
        return function.getParameters().stream()
                .map(param -> CxxJoinpoints.create(param, AParam.class))
                .collect(Collectors.toList());
    }

    @Override
    public String getDeclarationImpl(Boolean withReturnType) {
        return function.getDeclarationId(withReturnType);
    }

    @Override
    public ABody getBodyImpl() {
        if (!function.hasBody()) {
            return null;
        }

        return (ABody) CxxJoinpoints.create(function.getBody().get());
    }

    @Override
    public AFunction cloneImpl(String newName, Boolean insert) {
        /* make clone and insert after the function of this join point */
        return makeCloneAndInsert(newName, function, insert);
    }

    private AFunction makeCloneAndInsert(String newName, ClavaNode reference, boolean insert) {

        FunctionDecl newFunc = null;
        if (reference instanceof FunctionDecl) {
            newFunc = function.cloneAndInsert(newName, insert);
        } else if (reference instanceof TranslationUnit) {
            newFunc = function.cloneAndInsertOnFile(newName, (TranslationUnit) reference, insert);
        } else {
            throw new IllegalArgumentException(
                    "The node (" + reference + ") needs to be either a FuncDecl or a TranslationUnit.");
        }

        return CxxJoinpoints.create(newFunc, AFunction.class);
    }

    @Override
    public AFunction cloneOnFileImpl(String newName, String fileName) {
        if (fileName == null) {
            boolean isCxx = function.getAncestor(TranslationUnit.class).isCXXUnit();
            String extension = getIsPrototypeImpl() ? ".h" : isCxx ? ".cpp" : ".c";

            String prefix = newName;

            fileName = prefix + extension;
        }
        
        
        // First, check if the given filename is the same as a file in the AST
        App app = (App) getRootImpl().getNode();
        var currentFile = new File(fileName);

        var existingFile = app.getTranslationUnits().stream()
                .filter(tu -> tu.getFile().equals(currentFile))
                .findFirst();

        if (existingFile.isPresent()) {
            return cloneOnFileImpl(newName, new CxxFile(existingFile.get()));
        }

        // Extract relative path
        var relativePath = currentFile.getParentFile() != null ? currentFile.getParent() : null;

        // Create a new file
        var newFile = AstFactory.file(fileName, relativePath);

        // Set same source foldername
        var originalFile = function.getAncestorTry(TranslationUnit.class).orElse(null);
        if (originalFile != null) {
            // newFile.getNode().set(TranslationUnit.SOURCE_FOLDERNAME,
            // originalFile.get(TranslationUnit.SOURCE_FOLDERNAME));
            newFile.getNode().copyValue(TranslationUnit.SOURCE_FOLDERNAME, originalFile);
            // originalFile.get(TranslationUnit.SOURCE_FOLDERNAME).
        }
        // System.out.println("NEW FILE:" + newFile.getNode());
        // System.out.println("CURRRENT FILE:" + function.getAncestor(TranslationUnit.class));

        app.addFile((TranslationUnit) newFile.getNode());

        return cloneOnFileImpl(newName, newFile);
    }

    @Override
    // TODO: copy header file inclusion
    public AFunction cloneOnFileImpl(String newName, AFile file) {

        // if (!function.hasBody()) {
        // /*add the clone to the original place in order to be included where needed */
        // return makeCloneAndInsert(newName, function, true);
        // }

        /* if this is a definition, add the clone to the correct file */

        // App app = getRootImpl().getNode();
        //
        // Optional<TranslationUnit> file = app.getFile(fileName);
        //
        // if (!file.isPresent()) {
        //
        // TranslationUnit tu = getFactory().translationUnit(new File(fileName), Collections.emptyList());
        //
        // app.addFile(tu);
        //
        // file = Optional.of(tu);
        // }

        var tu = (TranslationUnit) file.getNode();

        var cloneFunction = makeCloneAndInsert(newName, tu, true);

        /* copy headers from the current file to the file with the clone */
        TranslationUnit originalFile = function.getAncestorTry(TranslationUnit.class).orElse(null);
        if (originalFile != null) {
            var includesCopy = TreeNodeUtils.copy(originalFile.getIncludes().getIncludes());
            // List<IncludeDecl> allIncludes = getIncludesCopyFromFile(originalFile);

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

            // System.out.println("BASE: " + baseIncludePath);
            // System.out.println("DEPTH: " + relativeFolderDepth);

            // Adapt includes
            for (var includeDecl : includesCopy) {
                var include = includeDecl.getInclude();

                // If angled, ignore
                if (include.isAngled()) {
                    continue;
                }
                // System.out.println("INCLUDE BEFORE: " + includeDecl.getCode());
                var newInclude = include.setInclude(new File(baseIncludePath, include.getInclude()).toString());
                includeDecl.set(IncludeDecl.INCLUDE, newInclude);
                // System.out.println("INCLUDE AFTER: " + includeDecl.getCode());
            }

            // Add includes
            includesCopy.stream().forEach(tu::addInclude);

        }

        return cloneFunction;
    }

    // /**
    // * XXX: copied from CallWrap
    // */
    // private List<IncludeDecl> getIncludesCopyFromFile(TranslationUnit tu) {
    //
    // return TreeNodeUtils.copy(tu.getIncludes().getIncludes());
    //
    // }

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
    public AJoinPoint insertReturnImpl(String code) {
        return insertReturnImpl(CxxJoinpoints.create(CxxWeaver.getSnippetParser().parseStmt(code)));
    }

    @Override
    public AJoinPoint insertReturnImpl(AJoinPoint code) {
        // Does not take into account situations where functions returns in all paths of an if/else.
        // This means it can lead to dead-code, although for C/C++ that does not seem to be problematic.

        // Do not insert if function has no implementation
        if (!function.hasBody()) {
            ClavaLog.info("insertReturn: could not insert in function without body");
            return null;
        }

        return CxxActions.insertReturn(getBodyImpl(), code);

        //
        // List<Stmt> bodyStmts = function.getBody().get().toStatements();
        //
        // // Check if it has return statement, ignoring wrapper statements
        // Stmt lastStmt = SpecsCollections.reverseStream(bodyStmts)
        // .filter(stmt -> !(stmt instanceof WrapperStmt))
        // .findFirst().orElse(null);
        //
        // ReturnStmt lastReturnStmt = lastStmt instanceof ReturnStmt ? (ReturnStmt) lastStmt : null;
        //
        // // Get list of all return statements inside children
        // List<ReturnStmt> returnStatements = bodyStmts.stream()
        // .flatMap(Stmt::getDescendantsStream)
        // .filter(ReturnStmt.class::isInstance)
        // .map(ReturnStmt.class::cast)
        // .collect(Collectors.toList());
        //
        // AJoinPoint lastInsertPoint = null;
        //
        // if (lastReturnStmt != null) {
        // returnStatements = SpecsCollections.concat(returnStatements, lastReturnStmt);
        // }
        //
        // for (ReturnStmt returnStmt : returnStatements) {
        // ACxxWeaverJoinPoint returnJp = CxxJoinpoints.create(returnStmt);
        // lastInsertPoint = returnJp.insertBefore(code);
        // }
        //
        // // If there is no return in the body, add at the end of the function
        // if (lastReturnStmt == null) {
        // lastInsertPoint = getBodyImpl().insertEnd(code);
        // }
        //
        // return lastInsertPoint;
    }

    /**
     * Uses the declaration, without the return type, to identify the function.
     */
    @Override
    public String getIdImpl() {
        return getDeclarationImpl(false);
    }

    @Override
    public AFunction[] getDeclarationJpsArrayImpl() {
        return function.getPrototypes().stream()
                .map(node -> CxxJoinpoints.create(node, AFunction.class))
                .toArray(size -> new AFunction[size]);
    }

    @Override
    public AFunction getDeclarationJpImpl() {
        var prototypes = getDeclarationJpsArrayImpl();

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
    public AFunction getDefinitionJpImpl() {
        return function.getImplementation()
                .map(node -> CxxJoinpoints.create(node, AFunction.class))
                .orElse(null);
    }

    /**
     * Setting the type of a Function join point sets the return type
     */
    @Override
    public void setTypeImpl(AType type) {
        setReturnTypeImpl(type);
    }

    @Override
    public void defNameImpl(String value) {
        // Set both the names of corresponding definition and declaration
        // Needs to first fetch both definition and declaration.
        // If one is renamed before fetching the other, the other will not be found

        var impl = function.getImplementation();
        var proto = function.getPrototypes();

        impl.ifPresent(node -> node.setName(value));
        proto.stream().forEach(node -> node.setName(value));
    }

    @Override
    public void setNameImpl(String name) {
        defNameImpl(name);
    }

    @Override
    public String getStorageClassImpl() {
        return function.get(FunctionDecl.STORAGE_CLASS).getString();
    }

    @Override
    public Boolean getIsInlineImpl() {
        return function.get(FunctionDecl.IS_INLINE_SPECIFIED);
    }

    @Override
    public Boolean getIsVirtualImpl() {
        return function.get(FunctionDecl.IS_VIRTUAL_AS_WRITTEN);
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
        SpecsLogs.info("[DEPRECATED] Selecting 'decl' from 'function' is deprecated");
        return CxxSelects.select(ADecl.class, function.getChildren(), true, Decl.class);
    }

    @Override
    public ACall[] getCallsArrayImpl() {
        return function.getCalls().stream()
                .map(call -> CxxJoinpoints.create(call, ACall.class))
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

            var parmVarDecl = ClavaNodes.toParam(typeVarname, function);

            params[i] = CxxJoinpoints.create(parmVarDecl, AParam.class);
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

    @Override
    public void defBodyImpl(AScope value) {
        function.setBody((CompoundStmt) value.getNode());
    }

    @Override
    public void setBodyImpl(AScope body) {
        defBodyImpl(body);
    }

    @Override
    public void defFunctionTypeImpl(AFunctionType value) {
        function.setFunctionType((FunctionType) value.getNode());
    }

    @Override
    public void setFunctionTypeImpl(AFunctionType functionType) {
        defFunctionTypeImpl(functionType);
    }

    @Override
    public AType getReturnTypeImpl() {
        return CxxJoinpoints.create(function.getReturnType(), AType.class);
    }

    @Override
    public void defReturnTypeImpl(AType value) {
        function.setReturnType((Type) value.getNode());
    };

    @Override
    public void setReturnTypeImpl(AType returnType) {
        defReturnTypeImpl(returnType);
    }

    @Override
    public void setParamTypeImpl(Integer index, AType newType) {
        function.setParamType(index, (Type) newType.getNode());
    }

    @Override
    public void addParamImpl(AParam param) {
        var originalParams = getParamsArrayImpl();
        var newParams = Arrays.copyOf(originalParams, originalParams.length + 1);

        newParams[newParams.length - 1] = param;

        defParamsImpl(newParams);
    }

    @Override
    public void addParamImpl(String name, AType type) {
        ClavaNode paramNode;
        if (type == null) {
            paramNode = ClavaNodes.toParam(name, function);
        } else {
            paramNode = getFactory().parmVarDecl(name, (Type) type.getNode());            
        }
        addParamImpl(CxxJoinpoints.create(paramNode, AParam.class));
    }

    @Override
    public void setParamImpl(Integer index, AParam param) {
        var params = getParamsArrayImpl();

        if (index >= params.length) {
            SpecsLogs.info("Tried to set parameter '" + param.getCodeImpl() + "' at index '" + index
                    + "' but function '" + function.getSignature() + "' only has " + params.length + " parameters");
            return;
        }

        params[index] = param;

        setParamsImpl(params);
    }

    @Override
    public void setParamImpl(Integer index, String name, AType type) {
        ClavaNode paramNode;
        
        if (type == null) {
            paramNode = ClavaNodes.toParam(name, function);
        } else {
            paramNode = getFactory().parmVarDecl(name, (Type) type.getNode());            
        }
        
        setParamImpl(index, CxxJoinpoints.create(paramNode, AParam.class));
    }

    @Override
    public Boolean getIsCudaKernelImpl() {
        return function.get(FunctionDecl.ATTRIBUTES).stream()
                .filter(attr -> attr instanceof CUDAGlobalAttr)
                .findFirst()
                .isPresent();
    }

    @Override
    public AFunction getCanonicalImpl() {
        return CxxJoinpoints.create(function.canonical(), AFunction.class);
    }

    @Override
    public Boolean getIsCanonicalImpl() {
        return function.isCanonical();
    }

}

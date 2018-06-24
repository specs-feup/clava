/**
 * Copyright 2017 SPeCS.
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

package pt.up.fe.specs.clava.weaver.actions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.suikasoft.jOptions.Datakey.DataKey;
import org.suikasoft.jOptions.Datakey.KeyFactory;

import com.google.common.base.Preconditions;

import pt.up.fe.specs.clava.ClavaNodeInfo;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.ast.ClavaNodeFactory;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.IncludeDecl;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.decl.data.DeclData;
import pt.up.fe.specs.clava.ast.decl.data.FunctionDeclData;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
import pt.up.fe.specs.clava.ast.extra.VariadicType;
import pt.up.fe.specs.clava.ast.stmt.ExprStmt;
import pt.up.fe.specs.clava.ast.stmt.Stmt;
import pt.up.fe.specs.clava.ast.type.BuiltinType;
import pt.up.fe.specs.clava.ast.type.FunctionType;
import pt.up.fe.specs.clava.ast.type.Type;
import pt.up.fe.specs.clava.ast.type.enums.BuiltinKind;
import pt.up.fe.specs.clava.context.ClavaFactory;
import pt.up.fe.specs.clava.weaver.CxxWeaver;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFile;
import pt.up.fe.specs.clava.weaver.abstracts.joinpoints.AFunction;
import pt.up.fe.specs.clava.weaver.importable.AstFactory;
import pt.up.fe.specs.clava.weaver.joinpoints.CxxCall;
import pt.up.fe.specs.clava.weaver.joinpoints.CxxProgram;
import pt.up.fe.specs.util.SpecsCollections;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsStrings;
import pt.up.fe.specs.util.treenode.TreeNodeUtils;

public class CallWrap {

    private static final DataKey<Set<String>> WRAPPER_CALLS = KeyFactory.generic(
            "clava_weaver_wrapper_calls", new HashSet<>());

    private static final String WRAPPER_IMPL_FILENAME_PREFIX = "clava_weaver_wrappers";
    private static final String WRAPPER_H_FILENAME = "clava_weaver_wrappers.h";

    private static final String WRAPPERS_FOLDERNAME = "clava_wrappers";

    private final CxxCall cxxCall;
    private final CxxProgram app;

    private final ClavaFactory factory;

    public CallWrap(CxxCall cxxCall) {
        this.cxxCall = cxxCall;
        app = cxxCall.getRootImpl();

        factory = app.getNode().getFactory();
    }

    public void addWrapper(String name) {
        // Ensure there is a set for the key
        if (!app.getAppData().hasValue(WRAPPER_CALLS)) {
            // app.getAppData().add(WRAPPER_CALLS, new HashMap<>());
            app.getAppData().add(WRAPPER_CALLS, new HashSet<>());
        }

        // If no wrapper with the given name, create wrapper
        // After wrapper is created, we can access its FunctionDecl
        // using the DataStore
        // if (!app.getAppData().get(WRAPPER_CALLS).containsKey(name)) {
        if (!app.getAppData().get(WRAPPER_CALLS).contains(name)) {
            CallWrapType wrapType = getWrapType();
            switch (wrapType) {
            case SYSTEM_INCLUDE:
                createSystemIncludeWrapper(name);
                break;
            case USER_INCLUDE:
                createUserIncludeWrapper(name);
                break;
            case NO_INCLUDE:
                // SpecsLogs.msgInfo(
                // "action 'wrap' is not supported yet when the call refers to a function that has no declaration in a
                // header file: "
                // + cxxCall.getNode().getLocation());
                createInPlaceWrapper(name);
                cxxCall.setName(name); // nned to call this here before returning
                return;
            case DECLARATION_IN_IMPLEMENTATION:
                SpecsLogs.msgInfo(
                        "Found a call to a function whose declaration is not in a header file, will not perform action 'wrap': "
                                + cxxCall.getNode().getLocation());
                return;
            }
            // System.out.println("INCLUDES:" + getWrapperIncludes(cxxCall));
            //
            // // If could not create a wrapper using separate files method, declare in the same file as a fallback
            // if (!createWrapperFunction(cxxCall, name)) {
            // SpecsLogs.msgInfo(
            // "addWrapper: Could not create wrapper function, wrapper functions in the same file as the call not
            // supported yet");
            // return;
            // }
        }

        // Add include
        // String includePath = CxxWeaver.getRelativeFilepath(getHeaderFile());
        String includePath = getHeaderFile().getRelativeFilepath();

        cxxCall.getNode().getAncestor(TranslationUnit.class).addInclude(includePath, false);
        // cxxCall.getNode().getAncestor(TranslationUnit.class).addInclude(getHeaderFile(),
        // CxxWeaver.getCxxWeaver().getBaseSourceFolder());

        // Set call name
        cxxCall.setName(name);

    }

    /**
     * Creates a wrapper below the found implementation of the original function.
     *
     * @param name
     */
    private void createInPlaceWrapper(String name) {

        FunctionDecl declaration = (FunctionDecl) cxxCall.getDefinitionImpl().getNode();
        addWrapperFunctionInPlace(name, declaration);
    }

    /**
     * Creates a wrapper function based on the declaration of the function call.
     *
     * @param name
     * @param cxxCall
     */
    private void createUserIncludeWrapper(String name) {
        // Get declaration of function call
        // FunctionDecl functionDecl = (FunctionDecl) cxxCall.getDeclImpl().getNode();

        // Get include file of declaration
        // FunctionDecl declaration = functionDecl.getDeclaration().get();

        // Get declaration of function call
        FunctionDecl declaration = (FunctionDecl) cxxCall.getDeclarationImpl().getNode();

        // Get include file
        TranslationUnit includeFile = declaration.getAncestor(TranslationUnit.class);

        // Get wrapper implementation file
        TranslationUnit implTu = getImplementationFile();

        // String includePath = CxxWeaver.getRelativeFilepath(includeFile);
        String includePath = includeFile.getRelativeFilepath();

        implTu.addInclude(includePath, false);
        // implTu.addInclude(includeFile, CxxWeaver.getCxxWeaver().getBaseSourceFolder());

        addWrapperFunction(name, declaration);
    }

    private void addWrapperFunction(String name, FunctionDecl declaration) {
        TranslationUnit implTu = getImplementationFile();

        FunctionDecl wrapperFunctionDeclHeader = (FunctionDecl) declaration.copy();
        FunctionDecl wrapperFunctionDeclImpl = (FunctionDecl) declaration.copy();
        wrapperFunctionDeclHeader.setDeclName(name);
        wrapperFunctionDeclImpl.setDeclName(name);

        // Create code that calls previous function
        List<String> paramNames = wrapperFunctionDeclImpl.getParameters().stream()
                .map(param -> param.getDeclName())
                .collect(Collectors.toList());

        List<Stmt> functionCallCode = createFunctionCallCode(paramNames);

        wrapperFunctionDeclImpl.setBody(factory.compoundStmt(functionCallCode));

        implTu.addChild(wrapperFunctionDeclImpl);

        getHeaderFile().addChild(wrapperFunctionDeclHeader);

        // Save function implementation declaration
        app.getAppData().get(WRAPPER_CALLS).add(name);
    }

    private void addWrapperFunctionInPlace(String name, FunctionDecl declaration) {

        FunctionDecl wrapperFunctionDeclImpl = (FunctionDecl) declaration.copy();
        wrapperFunctionDeclImpl.setDeclName(name);

        // Create code that calls previous function
        List<String> paramNames = wrapperFunctionDeclImpl.getParameters().stream()
                .map(param -> param.getDeclName())
                .collect(Collectors.toList());

        List<Stmt> functionCallCode = createFunctionCallCode(paramNames);

        wrapperFunctionDeclImpl.setBody(factory.compoundStmt(functionCallCode));

        // add to original file
        TranslationUnit originalFile = declaration.getAncestor(TranslationUnit.class);
        int indexOfOriginal = declaration.indexOfSelf();
        originalFile.addChild(indexOfOriginal + 1, wrapperFunctionDeclImpl);

        // Save function implementation declaration
        app.getAppData().get(WRAPPER_CALLS).add(name);
    }

    private void createSystemIncludeWrapper(String name) {

        // Get wrapper implementation file
        TranslationUnit implTu = getImplementationFile();

        // Add includes to implementation wrapper file
        List<IncludeDecl> allIncludes = getWrapperIncludesFromFile();

        allIncludes.stream().forEach(implTu::addInclude);

        // Create function declaration
        FunctionType functionType = getFunctionType();
        List<Type> paramTypes = functionType.getParamTypes();
        int numParameters = paramTypes.size();

        // Adjust number of parameters if last type is VariadicType,
        boolean isVariadicType = SpecsCollections.last(paramTypes) instanceof VariadicType;
        if (isVariadicType) {
            numParameters--;
        }

        List<String> paramNames = IntStream.rangeClosed(1, numParameters)
                .mapToObj(SpecsStrings::toExcelColumn)
                .collect(Collectors.toList());

        List<ParmVarDecl> inputs = IntStream.range(0, numParameters)
                .mapToObj(i -> ClavaNodeFactory.parmVarDecl(paramNames.get(i), paramTypes.get(i)))
                .collect(Collectors.toList());

        FunctionDeclData functionDeclData = new FunctionDeclData();
        DeclData declData = new DeclData();
        FunctionDecl declaration = ClavaNodeFactory.functionDecl(name, inputs, functionType, functionDeclData, declData,
                ClavaNodeInfo.undefinedInfo(), null);

        addWrapperFunction(name, declaration);

    }

    private CallWrapType getWrapType() {
        // Get declaration of function call
        AFunction functionDeclJp = cxxCall.getDeclarationImpl();
        AFunction functionDefJp = cxxCall.getDefinitionImpl();
        // AJoinPoint functionDeclJp = cxxCall.getDeclImpl();

        // If no declaration join point is found, this probably means that the call is from
        // a system header. Currently we cannot know a system include from a function call,
        // using another strategy where we copy all includes in the file of the current call to
        // the wrappers implementation file.

        if (functionDeclJp == null) {
            // If no declaration nor definition, this most likely indicates a system header function
            if (functionDefJp == null) {
                return CallWrapType.SYSTEM_INCLUDE;
            }
            // If no declaration but definition is present, this most likely indicates that the function is defined in
            // the
            // file of the function call
            else {
                FunctionDecl funcDef = (FunctionDecl) functionDefJp.getNode();
                SpecsLogs.msgLib("Could not find declaration of function '" + funcDef.getDeclName() + "' at "
                        + funcDef.getLocation());
                return CallWrapType.NO_INCLUDE;
            }
        }

        FunctionDecl functionDecl = (FunctionDecl) functionDeclJp.getNode();
        // Optional<FunctionDecl> declarationTry = functionDecl.getDeclaration();

        // if (!declarationTry.isPresent()) {

        // If no declaration but definition is present, this most likely indicates that the function is defined in the
        // file of the function call
        // if(functionDeclJp==null&&functionDefJp!=null)
        // {
        // SpecsLogs.msgLib("Could not find declaration of function '" + functionDecl.getDeclName() + "' at "
        // + functionDecl.getLocation());
        // return CallWrapType.NO_INCLUDE;
        // }

        // Get include file of declaration
        // FunctionDecl declaration = declarationTry.get();
        FunctionDecl declaration = (FunctionDecl) functionDeclJp.getNode();
        TranslationUnit includeFile = declaration.getAncestor(TranslationUnit.class);

        if (!includeFile.isHeaderFile()) {
            SpecsLogs.msgLib("Declaration of function '" + functionDecl.getDeclName() + "' is not in a header file: "
                    + functionDecl.getLocation());
            return CallWrapType.DECLARATION_IN_IMPLEMENTATION;
        }

        return CallWrapType.USER_INCLUDE;
    }

    /**
     * Ensures all required resources are available, and creates them if they are not.
     */
    private void initClavaWrappers() {
        // If wrapper files do not exist, create them
        String implementationFilename = getImplFilename();
        Optional<TranslationUnit> wrapperImpl = app.getNode().getFile(implementationFilename);

        if (!wrapperImpl.isPresent()) {
            // Ensure the header file does not exit yet
            Preconditions.checkArgument(!app.getNode().getFile(WRAPPER_H_FILENAME).isPresent(),
                    "Expected header file to not exist yet");

            // Create implementation and header file
            // AFile implFile = AstFactory.file(implementationFilename, app.getBaseFolderImpl());
            // AFile headerFile = AstFactory.file(WRAPPER_H_FILENAME, app.getBaseFolderImpl());
            AFile implFile = AstFactory.file(implementationFilename, WRAPPERS_FOLDERNAME);
            AFile headerFile = AstFactory.file(WRAPPER_H_FILENAME, WRAPPERS_FOLDERNAME);
            // System.out.println("INCLUDE PATH:" + includePath);
            System.out.println("HEADER RELATIVE:" + headerFile.getRelativeFolderpath());
            app.addFileImpl(headerFile);
            app.addFileImpl(implFile);
        }

        // Ensure the header file also exists
        Preconditions.checkArgument(app.getNode().getFile(WRAPPER_H_FILENAME).isPresent(),
                "Expected header file to exist");

        return;

    }

    private String getImplFilename() {
        boolean isCxx = CxxWeaver.getCxxWeaver().getConfig().get(ClavaOptions.STANDARD).isCxx();
        String extension = isCxx ? "cpp" : "c";
        return WRAPPER_IMPL_FILENAME_PREFIX + "." + extension;
    }

    private List<IncludeDecl> getWrapperIncludesFromFile() {
        TranslationUnit callFile = cxxCall.getNode().getAncestor(TranslationUnit.class);
        return TreeNodeUtils.copy(callFile.getIncludes().getIncludes());
    }

    private FunctionType getFunctionType() {
        return cxxCall.getNode().getCalleeDeclRef().getType().toTry(FunctionType.class).get();
    }

    private List<Stmt> createFunctionCallCode(List<String> paramNames) {
        CallExpr call = cxxCall.getNode();

        List<Stmt> wrapperStmts = new ArrayList<>();

        FunctionType ftype = getFunctionType();

        StringBuilder code = new StringBuilder();

        Type returnType = ftype.getReturnType();

        boolean isVoid = isVoid(returnType);
        String varName = "result";
        if (!isVoid) {
            VarDecl varDecl = ClavaNodeFactory.varDecl(varName, ftype.getReturnType());

            wrapperStmts.add(factory.declStmt(varDecl));
            // wrapperStmts.add(ClavaNodeFactory.declStmt(null, Arrays.asList(varDecl)));
        }

        Expr function = call.getCallee();

        List<Expr> args = paramNames.stream()
                .map(param -> ClavaNodeFactory.literalExpr(param, ClavaNodeFactory.nullType(null)))
                .collect(Collectors.toList());

        CallExpr callExpr = ClavaNodeFactory.callExpr(function, returnType, args);

        if (isVoid) {
            wrapperStmts.add(CxxWeaver.getFactory().exprStmt(callExpr));
        } else {
            DeclRefExpr varAssigned = ClavaNodeFactory.declRefExpr(varName, returnType);
            ExprStmt assignment = ClavaNodeFactory.exprStmtAssign(varAssigned, callExpr, returnType);
            wrapperStmts.add(assignment);
        }

        if (!isVoid) {
            wrapperStmts.add(factory.returnStmt(factory.literalExpr(varName, ftype.getReturnType())));
            // wrapperStmts.add(
            // ClavaNodeFactory.returnStmt(null, ClavaNodeFactory.literalExpr(varName, ftype.getReturnType())));
        }

        code.append(call.getCalleeName()).append("(");
        code.append(paramNames.stream().collect(Collectors.joining(", ")));
        code.append(");");

        return wrapperStmts;
    }

    private boolean isVoid(Type returnType) {
        if (!(returnType instanceof BuiltinType)) {
            return false;
        }

        // return ((BuiltinType) returnType).isVoid();
        return ((BuiltinType) returnType).get(BuiltinType.KIND) == BuiltinKind.Void;
    }

    private TranslationUnit getImplementationFile() {

        // Make sure Clava wrapper files exist
        initClavaWrappers();

        return app.getNode().getFile(getImplFilename()).orElseThrow(() -> new RuntimeException(
                "Implementation file not found, make sure init function was called"));
    }

    private TranslationUnit getHeaderFile() {

        // Make sure Clava wrapper files exist
        initClavaWrappers();

        return app.getNode().getFile(WRAPPER_H_FILENAME).orElseThrow(() -> new RuntimeException(
                "Header file not found, make sure init function was called"));
    }
}

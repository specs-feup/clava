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

import pt.up.fe.specs.clava.ClavaNode;
import pt.up.fe.specs.clava.ClavaOptions;
import pt.up.fe.specs.clava.ast.decl.FunctionDecl;
import pt.up.fe.specs.clava.ast.decl.IncludeDecl;
import pt.up.fe.specs.clava.ast.decl.ParmVarDecl;
import pt.up.fe.specs.clava.ast.decl.VarDecl;
import pt.up.fe.specs.clava.ast.expr.BinaryOperator;
import pt.up.fe.specs.clava.ast.expr.CallExpr;
import pt.up.fe.specs.clava.ast.expr.DeclRefExpr;
import pt.up.fe.specs.clava.ast.expr.Expr;
import pt.up.fe.specs.clava.ast.expr.enums.BinaryOperatorKind;
import pt.up.fe.specs.clava.ast.extra.TranslationUnit;
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
                addWrapperFunctionInPlace(name, false);
                cxxCall.setName(name); // need to call this here before returning
                return;
            case DECLARATION_IN_IMPLEMENTATION:

                addWrapperFunctionInPlace(name, true);
                cxxCall.setName(name);
                return;
            }

        }

        // Add include
        String includePath = getHeaderFile().getRelativeFilepath();

        cxxCall.getNode().getAncestor(TranslationUnit.class).addInclude(includePath, false);

        // Set call name
        cxxCall.setName(name);
    }

    /**
     * Creates a wrapper function based on the declaration of the function call.
     *
     * @param name
     * @param cxxCall
     */
    private void createUserIncludeWrapper(String name) {

        // Get declaration of function call
        FunctionDecl declaration = (FunctionDecl) cxxCall.getDeclarationImpl().getNode();

        // Get include file
        TranslationUnit includeFile = declaration.getAncestor(TranslationUnit.class);

        // Get wrapper implementation file
        TranslationUnit implTu = getImplementationFile();

        String includePath = includeFile.getRelativeFilepath();

        implTu.addInclude(includePath, false);

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

    /**
     * Creates a wrapper below the found implementation of the original function.
     *
     * @param name
     */
    private void addWrapperFunctionInPlace(String name, boolean hasDecl) {

        FunctionDecl originalDefinition = (FunctionDecl) cxxCall.getDefinitionImpl().getNode();

        FunctionDecl wrapperFunctionDeclImpl = (FunctionDecl) originalDefinition.copy();
        wrapperFunctionDeclImpl.setDeclName(name);

        // Create code that calls previous function
        List<String> paramNames = wrapperFunctionDeclImpl.getParameters().stream()
                .map(param -> param.getDeclName())
                .collect(Collectors.toList());

        List<Stmt> functionCallCode = createFunctionCallCode(paramNames);

        wrapperFunctionDeclImpl.setBody(factory.compoundStmt(functionCallCode));

        // add to original file
        TranslationUnit originalFile = originalDefinition.getAncestor(TranslationUnit.class);
        TranslationUnit updatedFile = cxxCall.getNode().getApp().getTranslationUnit(originalFile.getLocation());

        // adds the wrapper implementation after the implementation of the original
        int originalDefinitionIndex = getIndex(originalDefinition, updatedFile);
        updatedFile.addChild(originalDefinitionIndex + 1, wrapperFunctionDeclImpl);

        // adds a wrapper forward declaration...
        FunctionDecl forwardDecl = (FunctionDecl) wrapperFunctionDeclImpl.copy();
        forwardDecl.getBody().get().detach();
        if (hasDecl) {
            // ... after the declaration of the original
            FunctionDecl originalDeclaration = (FunctionDecl) cxxCall.getDeclarationImpl().getNode();
            int originalDeclarationIndex = getIndex(originalDeclaration, updatedFile);
            updatedFile.addChild(originalDeclarationIndex + 1, forwardDecl);
        } else {
            // ... before the definition of the original
            // this is needed when there is recursion and the original will call the wrapper
            updatedFile.addChild(originalDefinitionIndex, forwardDecl);
        }

        // Save function implementation declaration
        app.getAppData().get(WRAPPER_CALLS).add(name);
    }

    private int getIndex(FunctionDecl decl, TranslationUnit tu) {

        int originalDefinitionIndex = -1;
        for (int i = 0; i < tu.getNumChildren(); i++) {
            ClavaNode child = tu.getChild(i);

            if (child.getLocation().toString().equals(decl.getLocation().toString())) {
                originalDefinitionIndex = i;
                break;
            }
        }
        return originalDefinitionIndex;
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

        List<String> paramNames = IntStream.rangeClosed(1, numParameters)
                .mapToObj(SpecsStrings::toExcelColumn)
                .collect(Collectors.toList());

        List<ParmVarDecl> inputs = IntStream.range(0, numParameters)
                .mapToObj(i -> factory.parmVarDecl(paramNames.get(i), paramTypes.get(i)))
                .collect(Collectors.toList());

        FunctionDecl declaration = factory.functionDecl(name, functionType);
        declaration.setParameters(inputs);

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

            // If definition but no declaration, check if it is associated with a File. If not, consider it a system
            // header function
            if (functionDefJp.getNode().getAncestorTry(TranslationUnit.class).isEmpty()) {
                return CallWrapType.SYSTEM_INCLUDE;
            }

            // If no declaration but definition is present, this most likely indicates that the function is defined in
            // the
            // file of the function call
            FunctionDecl funcDef = (FunctionDecl) functionDefJp.getNode();
            SpecsLogs.msgLib("Could not find declaration of function '" + funcDef.getDeclName() + "' at "
                    + funcDef.getLocation());
            return CallWrapType.NO_INCLUDE;

        }

        FunctionDecl functionDecl = (FunctionDecl) functionDeclJp.getNode();

        // Get include file of declaration
        // FunctionDecl declaration = declarationTry.get();
        FunctionDecl declaration = (FunctionDecl) functionDeclJp.getNode();
        Optional<TranslationUnit> includeFileTry = declaration.getAncestorTry(TranslationUnit.class);

        // TODO: Confirm with Pedro what should be done here
        if (!includeFileTry.isPresent()) {
            SpecsLogs
                    .msgLib("Could not find translation unit of the declaration of function '"
                            + declaration.getDeclName() + "' at "
                            + declaration.getLocation());
            return CallWrapType.SYSTEM_INCLUDE;
        }

        TranslationUnit includeFile = includeFileTry.get();

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
            AFile implFile = AstFactory.file(implementationFilename, WRAPPERS_FOLDERNAME);
            AFile headerFile = AstFactory.file(WRAPPER_H_FILENAME, WRAPPERS_FOLDERNAME);

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
            VarDecl varDecl = factory.varDecl(varName, ftype.getReturnType());

            wrapperStmts.add(factory.declStmt(varDecl));
        }

        Expr function = call.getCallee();

        List<Expr> args = paramNames.stream()
                .map(param -> factory.literalExpr(param, factory.nullType()))
                .collect(Collectors.toList());

        CallExpr callExpr = CxxWeaver.getFactory().callExpr(function, returnType, args);

        if (isVoid) {
            wrapperStmts.add(CxxWeaver.getFactory().exprStmt(callExpr));
        } else {
            DeclRefExpr varAssigned = factory.declRefExpr(varName, returnType);
            BinaryOperator op = factory.binaryOperator(BinaryOperatorKind.Assign, returnType, varAssigned, callExpr);
            ExprStmt assignment = factory.exprStmt(op);
            wrapperStmts.add(assignment);
        }

        if (!isVoid) {
            wrapperStmts.add(factory.returnStmt(factory.literalExpr(varName, ftype.getReturnType())));
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
